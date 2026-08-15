package com.rafaelasoares.acesso.controller;

import com.rafaelasoares.TestcontainersConfiguration;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Fluxo HTTP do cadastro de usuário, contra Postgres real (Testcontainers).
 *
 * <p>O JSON vai escrito à mão de propósito: é exatamente o que trafega na
 * requisição, então o teste falha se o contrato mudar sem querer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UsuarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limparBase() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("sem autenticação, o cadastro de usuário responde 401")
    void exigeAutenticacao() throws Exception {
        // Não existe auto-cadastro: criar usuário é ato de administrador.
        mockMvc.perform(
                        post("/api/usuarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "nomeCompleto": "Invasor",
                                          "email": "invasor@teste.vet",
                                          "senha": "senhaQualquer1",
                                          "perfilAcesso": "ADMINISTRADOR"
                                        }"""))
                .andExpect(status().isUnauthorized());

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    @DisplayName("cria usuário e devolve 201, sem jamais expor o hash da senha")
    void criaSemExporHash() throws Exception {
        String corpo =
                mockMvc.perform(
                                post("/api/usuarios")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "nomeCompleto": "Rafaela Soares",
                                                  "email": "Rafaela@RafaelaSoares.vet",
                                                  "senha": "senhaSegura123",
                                                  "perfilAcesso": "VETERINARIO"
                                                }"""))
                        .andExpect(status().isCreated())
                        // E-mail normalizado para minúsculas na entrada.
                        .andExpect(jsonPath("$.email").value("rafaela@rafaelasoares.vet"))
                        .andExpect(jsonPath("$.ativo").value(true))
                        .andExpect(jsonPath("$.criadoEm").isNotEmpty())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        // A verificação que mais importa: nem o nome do campo, nem o hash.
        assertThat(corpo).doesNotContain("senhaHash").doesNotContain("$2a$");

        // E a senha foi guardada como hash, não em texto puro.
        Usuario salvo =
                usuarioRepository.findByEmailIgnoreCase("rafaela@rafaelasoares.vet").orElseThrow();
        assertThat(salvo.getSenhaHash()).isNotEqualTo("senhaSegura123").startsWith("$2");
    }

    @Test
    @WithMockUser
    @DisplayName("e-mail repetido em outra caixa é conflito (409)")
    void rejeitaEmailDuplicadoIgnorandoCaixa() throws Exception {
        mockMvc.perform(
                        post("/api/usuarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "nomeCompleto": "Primeira",
                                          "email": "rafaela@rafaelasoares.vet",
                                          "senha": "senhaSegura123",
                                          "perfilAcesso": "VETERINARIO"
                                        }"""))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/usuarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "nomeCompleto": "Segunda",
                                          "email": "RAFAELA@RafaelaSoares.VET",
                                          "senha": "outraSenha123",
                                          "perfilAcesso": "ATENDENTE"
                                        }"""))
                .andExpect(status().isConflict());

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("campos inválidos viram 400 detalhando cada campo")
    void validaCampos() throws Exception {
        mockMvc.perform(
                        post("/api/usuarios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "nomeCompleto": "",
                                          "email": "nao-e-email",
                                          "senha": "curta",
                                          "perfilAcesso": null
                                        }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("id inexistente responde 404")
    void buscaInexistente() throws Exception {
        mockMvc.perform(get("/api/usuarios/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
