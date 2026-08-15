package com.rafaelasoares.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segurança da API.
 *
 * <p><b>Nada em {@code /api/**} é público.</b> Não existe auto-cadastro: até
 * o endpoint que cria usuário exige autenticação, porque criar usuário é ato
 * de administrador. Em desenvolvimento o primeiro administrador vem do
 * {@link DevSeedConfig}; em produção, de procedimento operacional — nunca de
 * endpoint aberto.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                // (padrão BFF), enviando credencial explícita. CSRF protege
                // contra credencial que o browser anexa sozinho — não é o
                // caso aqui. Se algum dia o browser passar a chamar a API
                // direto com cookie, isto tem que voltar.
                .csrf(csrf -> csrf.disable())
                // Sem sessão de servidor: cada request se autentica sozinho.
                .sessionManagement(
                        sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        rotas ->
                                rotas
                                        // Health check do container/orquestrador.
                                        .requestMatchers("/actuator/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                // HTTP Basic por enquanto. Será substituído por JWT quando
                // /api/sessoes entrar — o UsuarioDetailsService continua o
                // mesmo, só muda como o token chega.
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
