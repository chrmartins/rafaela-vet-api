package com.vetplanet.config;

import com.vetplanet.modules.acesso.config.DevSeedConfig;
import com.vetplanet.modules.acesso.filter.TokenAutenticacaoFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Segurança da API.
 *
 * <p><b>Só o login é público.</b> Todo o resto exige token de sessão válido, e
 * a gestão de usuários exige ainda o perfil {@code ADMINISTRADOR} — declarado
 * com {@code @PreAuthorize} no próprio controller, onde fica visível.
 *
 * <p>Não há auto-cadastro: quem cria usuário é administrador. Em
 * desenvolvimento o primeiro vem do {@link DevSeedConfig}; em produção, de
 * procedimento operacional.
 */
@Configuration
@EnableWebSecurity
// Habilita @PreAuthorize nos controllers.
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Rotas que o springdoc serve. Só são liberadas quando a documentação está
     * ligada — ver {@link #documentacaoHabilitada}.
     */
    private static final String[] CAMINHOS_DA_DOCUMENTACAO = {
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    private final TokenAutenticacaoFilter tokenAutenticacaoFilter;

    /**
     * Espelha {@code springdoc.api-docs.enabled}, que é {@code false} por
     * padrão e só vira {@code true} no perfil {@code dev}.
     *
     * <p>O default aqui também é {@code false}: se a propriedade sumir do
     * yml, a documentação continua fechada. Falhar fechado é o que se quer
     * numa API que trata CPF e prontuário.
     */
    private final boolean documentacaoHabilitada;

    public SecurityConfig(
            TokenAutenticacaoFilter tokenAutenticacaoFilter,
            @Value("${springdoc.api-docs.enabled:false}") boolean documentacaoHabilitada) {
        this.tokenAutenticacaoFilter = tokenAutenticacaoFilter;
        this.documentacaoHabilitada = documentacaoHabilitada;
    }

    /**
     * BCrypt para hash de senha. Nunca guardar senha em texto puro.
     *
     * <p>Custo padrão (10). Aumentar exige rehash das senhas existentes, então
     * é decisão consciente, não ajuste solto.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF desligado porque esta API não é consumida pelo
                // navegador diretamente: quem chama é o servidor do Next
                // (padrão BFF), mandando o token no header Authorization. CSRF
                // protege contra credencial que o browser anexa sozinho — não
                // é o caso. Se o browser passar a chamar a API direto com
                // cookie, isto tem que voltar.
                .csrf(csrf -> csrf.disable())
                // Sem sessão de servidor: cada request se autentica pelo token.
                .sessionManagement(
                        sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        rotas -> {
                            // Entrar precisa ser público — é o único jeito de
                            // obter um token.
                            rotas.requestMatchers(HttpMethod.POST, "/api/sessoes").permitAll();
                            // Health check do orquestrador.
                            rotas.requestMatchers("/actuator/health").permitAll();
                            // A documentação entra no filter chain apenas
                            // quando ligada. Em produção estas rotas não são
                            // liberadas aqui — e o springdoc nem sobe —, então
                            // são duas travas independentes, não uma.
                            if (documentacaoHabilitada) {
                                rotas.requestMatchers(CAMINHOS_DA_DOCUMENTACAO).permitAll();
                            }
                            rotas.anyRequest().authenticated();
                        })
                // Sem autenticação → 401 com corpo vazio, em vez do 403 que o
                // Spring devolveria por padrão. 401 é o correto: falta
                // credencial, não é permissão negada.
                .exceptionHandling(
                        erros ->
                                erros.authenticationEntryPoint(
                                        (req, resp, ex) ->
                                                resp.sendError(
                                                        HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(
                        tokenAutenticacaoFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
