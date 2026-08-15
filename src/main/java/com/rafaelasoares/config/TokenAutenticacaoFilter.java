package com.rafaelasoares.config;

import com.rafaelasoares.acesso.entity.TokenAutenticacao;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.repository.TokenAutenticacaoRepository;
import com.rafaelasoares.acesso.service.TokenGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica a requisição a partir do token de sessão.
 *
 * <p>Espera {@code Authorization: Bearer <token>}. Busca a sessão pelo hash e
 * só autentica se ela estiver válida — não expirada, não revogada e com o
 * usuário ainda ativo.
 *
 * <p><b>Sem {@code @Transactional} aqui, deliberadamente.</b> Anotar um
 * filtro como transacional faz o Spring proxiá-lo com CGLIB, e o proxy é
 * criado sem chamar o construtor — o {@code logger} herdado de
 * {@code GenericFilterBean} fica nulo e a aplicação quebra no boot. Por isso
 * a consulta traz o usuário com {@code join fetch}, dispensando transação.
 *
 * <p>Não rejeita nada por conta própria: quando o token falta ou não presta,
 * apenas segue sem autenticar, e quem decide se a rota exigia autenticação é
 * a configuração do Spring Security.
 */
@Component
public class TokenAutenticacaoFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIXO = "Bearer ";

    private final TokenAutenticacaoRepository tokenRepository;
    private final TokenGenerator tokenGenerator;

    public TokenAutenticacaoFilter(
            TokenAutenticacaoRepository tokenRepository, TokenGenerator tokenGenerator) {
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {

        String token = extrairToken(requisicao);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenRepository
                    .buscarPorTokenHashComUsuario(tokenGenerator.hash(token))
                    .filter(TokenAutenticacao::estaValida)
                    .ifPresent(sessao -> autenticar(sessao.getUsuario(), requisicao));
        }

        cadeia.doFilter(requisicao, resposta);
    }

    private void autenticar(Usuario usuario, HttpServletRequest requisicao) {
        // Prefixo ROLE_ é o que hasRole('ADMINISTRADOR') espera encontrar.
        var permissoes =
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfilAcesso().name()));

        var autenticacao =
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, permissoes);
        autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));

        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }

    private String extrairToken(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader(HEADER);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return null;
        }
        String token = cabecalho.substring(PREFIXO.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
