package com.vetplanet.modules.acesso.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetplanet.modules.acesso.entity.PerfilAcesso;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Decisões que o bootstrap toma sozinho, sem banco nem Spring.
 *
 * <p>Vale cobrir porque é o mecanismo que decide <b>quem consegue entrar no
 * sistema</b>. Errar para o lado permissivo cria credencial padrão exposta na
 * internet; errar para o lado restritivo tranca o dono do lado de fora da
 * própria instância. As duas falhas são silenciosas em produção.
 */
@ExtendWith(MockitoExtension.class)
class AdministradorInicialConfigTest {

    private static final String SENHA_VALIDA = "senha-bem-longa-123";

    @Mock private UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final AdministradorInicialConfig config = new AdministradorInicialConfig();

    private void executar(String email, String senha) throws Exception {
        config.criarAdministradorInicial(
                        usuarioRepository, passwordEncoder, "Administrador", email, senha)
                .run();
    }

    @Test
    @DisplayName("cria o administrador quando o banco está vazio e as variáveis existem")
    void criaQuandoBancoVazio() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        executar("dono@vetplanet.vet", SENHA_VALIDA);

        ArgumentCaptor<UsuarioEntity> salvo = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(salvo.capture());

        UsuarioEntity criado = salvo.getValue();
        assertThat(criado.getEmail()).isEqualTo("dono@vetplanet.vet");
        assertThat(criado.getPerfilAcesso()).isEqualTo(PerfilAcesso.ADMINISTRADOR);
        // Senha em texto puro violaria a regra de LGPD do projeto e vazaria no
        // primeiro dump de banco.
        assertThat(criado.getSenhaHash()).isNotEqualTo(SENHA_VALIDA);
        assertThat(passwordEncoder.matches(SENHA_VALIDA, criado.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("não cria nada quando as variáveis não estão definidas")
    void naoCriaSemVariaveis() throws Exception {
        when(usuarioRepository.count()).thenReturn(0L);

        executar("", "");

        // Sem credencial padrão: é o que separa este bootstrap do DevSeedConfig.
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("não cria um segundo administrador quando já existe usuário")
    void naoCriaQuandoJaExisteUsuario() throws Exception {
        when(usuarioRepository.count()).thenReturn(1L);

        executar("dono@vetplanet.vet", SENHA_VALIDA);

        // É o que torna seguro deixar as variáveis configuradas: todo restart
        // passa por aqui, e nenhum deles cria um administrador a mais.
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("falha o start quando a senha é curta demais")
    void falhaComSenhaCurta() {
        when(usuarioRepository.count()).thenReturn(0L);

        // Subir de pé com administrador de senha fraca é pior do que não subir.
        assertThatThrownBy(() -> executar("dono@vetplanet.vet", "curta123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12 caracteres");

        verify(usuarioRepository, never()).save(any());
    }
}
