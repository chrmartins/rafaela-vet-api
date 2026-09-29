package com.vetplanet.modules.acesso.config;

import com.vetplanet.modules.acesso.entity.PerfilAcesso;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria o primeiro administrador fora do perfil {@code dev}.
 *
 * <p>Resolve o mesmo ovo-e-galinha do {@link DevSeedConfig}, mas para
 * ambiente real: todo endpoint exige autenticação, inclusive o que cria
 * usuário, e <b>não há cadastro público</b>. Sem isto, uma instância recém
 * subida tem a tabela {@code usuario} vazia e ninguém consegue entrar — nem o
 * dono do sistema.
 *
 * <p>Difere do {@code DevSeedConfig} em três pontos, todos deliberados:
 *
 * <ol>
 *   <li><b>Não tem valor padrão.</b> Sem {@code ADMIN_INICIAL_EMAIL} e
 *       {@code ADMIN_INICIAL_SENHA} definidos, não faz nada. Credencial
 *       padrão em ambiente real é como sistema é invadido no primeiro dia.
 *   <li><b>Não registra a senha no log.</b> O {@code DevSeedConfig} imprime a
 *       dele porque é conhecida e descartável; esta é real.
 *   <li><b>Exige senha de pelo menos 12 caracteres.</b> A conta criada aqui é
 *       ADMINISTRADOR e nasce exposta na internet.
 * </ol>
 *
 * <p><b>É idempotente:</b> se já existe qualquer usuário, não faz nada. Isso
 * o torna seguro em todo restart e em plataforma que reinicia contêiner
 * sozinha — as variáveis podem ficar configuradas sem risco de sobrescrever
 * ninguém.
 *
 * <p>Depois do primeiro acesso, o certo é <b>trocar a senha pelo painel e
 * remover as duas variáveis</b> do ambiente: elas ficam visíveis no painel de
 * qualquer plataforma de deploy, e o valor delas deixa de ser necessário
 * assim que a conta existe.
 *
 * <p>Quando o VetPlanet tiver mais de um assinante, este é o mecanismo que
 * precisa virar parte do provisionamento de cada novo inquilino — não uma
 * variável de ambiente global.
 */
@Configuration
@Profile("!dev")
public class AdministradorInicialConfig {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicialConfig.class);

    /** Mínimo do NIST SP 800-63B para senha escolhida por pessoa, sem MFA. */
    private static final int TAMANHO_MINIMO_SENHA = 12;

    @Bean
    public CommandLineRunner criarAdministradorInicial(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin-inicial.nome:Administrador}") String nome,
            @Value("${app.admin-inicial.email:}") String email,
            @Value("${app.admin-inicial.senha:}") String senha) {

        return argumentos -> {
            if (usuarioRepository.count() > 0) {
                return;
            }

            if (email.isBlank() || senha.isBlank()) {
                log.warn(
                        "Nenhum usuário cadastrado e ADMIN_INICIAL_EMAIL/ADMIN_INICIAL_SENHA não"
                            + " definidas: ninguém consegue entrar. Defina as duas variáveis e"
                            + " reinicie para criar o administrador inicial.");
                return;
            }

            if (senha.length() < TAMANHO_MINIMO_SENHA) {
                // Falha o start de propósito: seguir de pé com um administrador
                // de senha fraca é pior do que não subir.
                throw new IllegalStateException(
                        "ADMIN_INICIAL_SENHA precisa ter ao menos "
                                + TAMANHO_MINIMO_SENHA
                                + " caracteres.");
            }

            usuarioRepository.save(
                    UsuarioEntity.criar(
                            nome,
                            email,
                            passwordEncoder.encode(senha),
                            PerfilAcesso.ADMINISTRADOR,
                            null));

            log.info(
                    "Administrador inicial criado para {}. Troque a senha no primeiro acesso e"
                        + " remova ADMIN_INICIAL_EMAIL e ADMIN_INICIAL_SENHA do ambiente.",
                    email);
        };
    }
}
