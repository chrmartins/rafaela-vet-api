package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.CriarSessaoRequest;
import com.rafaelasoares.acesso.dto.SessaoResponse;
import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.entity.TokenAutenticacao;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.exception.CredenciaisInvalidasException;
import com.rafaelasoares.acesso.repository.TokenAutenticacaoRepository;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Autentica e abre uma sessão — é o "entrar" do painel. */
@Service
public class CriarSessaoService {

    private static final Logger log = LoggerFactory.getLogger(CriarSessaoService.class);

    private final UsuarioRepository usuarioRepository;
    private final TokenAutenticacaoRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final Duration validadeDaSessao;

    public CriarSessaoService(
            UsuarioRepository usuarioRepository,
            TokenAutenticacaoRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            TokenGenerator tokenGenerator,
            @Value("${app.sessao.validade:PT12H}") Duration validadeDaSessao) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.validadeDaSessao = validadeDaSessao;
    }

    @Transactional
    public SessaoResponse criarSessao(CriarSessaoRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmailIgnoreCase(request.email());

        // Compara a senha mesmo quando o e-mail não existe. Sem isso, a
        // resposta volta mais rápido para e-mail inexistente do que para senha
        // errada, e essa diferença de tempo revela quem tem conta.
        boolean senhaConfere =
                encontrado
                        .map(usuario -> passwordEncoder.matches(request.senha(), usuario.getSenhaHash()))
                        .orElseGet(
                                () -> {
                                    passwordEncoder.matches(request.senha(), HASH_DESCARTAVEL);
                                    return false;
                                });

        Usuario usuario = encontrado.orElse(null);
        if (!senhaConfere || usuario == null || !usuario.isAtivo()) {
            // Log com o e-mail tentado ajuda a investigar; a resposta ao
            // cliente continua genérica.
            log.info("Tentativa de login rejeitada para {}", request.email());
            throw new CredenciaisInvalidasException();
        }

        String token = tokenGenerator.gerar();
        TokenAutenticacao sessao =
                TokenAutenticacao.criar(usuario, tokenGenerator.hash(token), validadeDaSessao);
        tokenRepository.save(sessao);

        log.info("Sessão aberta para {}", usuario.getEmail());
        // Única vez em que o token existe fora do cliente.
        return new SessaoResponse(token, sessao.getExpiraEm(), UsuarioResponse.de(usuario));
    }

    /**
     * Hash real de uma senha qualquer, só para gastar o mesmo tempo de CPU
     * quando o e-mail não existe (ver comentário acima).
     */
    private static final String HASH_DESCARTAVEL =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
}
