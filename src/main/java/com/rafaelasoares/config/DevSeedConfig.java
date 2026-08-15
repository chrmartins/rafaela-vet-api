package com.rafaelasoares.config;

import com.rafaelasoares.acesso.entity.PerfilAcesso;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria o primeiro administrador em ambiente de desenvolvimento.
 *
 * <p>Existe por causa do ovo-e-galinha: todo endpoint exige autenticação,
 * inclusive o que cria usuário, então sem um usuário inicial não há como
 * entrar.
 *
 * <p><b>Só roda no perfil {@code dev}</b>, que o {@code bootRun} ativa (ver
 * build.gradle). Um {@code java -jar} sem perfil — que é como sobe em
 * produção — não executa isto, então não há risco de credencial padrão em
 * ambiente real. Lá o primeiro administrador é criado por procedimento
 * operacional.
 */
@Configuration
@Profile("dev")
public class DevSeedConfig {

    private static final Logger log = LoggerFactory.getLogger(DevSeedConfig.class);

    @Bean
    public CommandLineRunner criarAdministradorDeDesenvolvimento(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.dev.admin-email:admin@rafaelasoares.vet}") String email,
            @Value("${app.dev.admin-senha:desenvolvimento}") String senha) {

        return argumentos -> {
            if (usuarioRepository.count() > 0) {
                return;
            }

            usuarioRepository.save(
                    Usuario.criar(
                            "Administrador de Desenvolvimento",
                            email,
                            passwordEncoder.encode(senha),
                            PerfilAcesso.ADMINISTRADOR));

            log.warn(
                    """

                    ┌───────────────────────────────────────────────────────────
                    │ PERFIL DEV: administrador inicial criado
                    │   e-mail: {}
                    │   senha:  {}
                    │ Credencial de desenvolvimento — não existe em produção.
                    └───────────────────────────────────────────────────────────""",
                    email,
                    senha);
        };
    }
}
