package com.rafaelasoares.acesso.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rafaelasoares.TestcontainersConfiguration;
import com.rafaelasoares.acesso.entity.PerfilAcesso;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.repository.TokenAutenticacaoRepository;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Ciclo de sessão: entrar, usar o token, sair. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SessaoControllerTest {

    private static final String SENHA = "senhaSegura123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TokenAutenticacaoRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararBase() {
        tokenRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.save(
                Usuario.criar(
                        "Rafaela Soares",
                        "rafaela@rafaelasoares.vet",
                        passwordEncoder.encode(SENHA),
                        PerfilAcesso.ADMINISTRADOR));
    }

    private String entrarEObterToken() throws Exception {
        String corpo =
                mockMvc.perform(
                                post("/api/sessoes")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"rafaela@rafaelasoares.vet",
                                                 "senha":"%s"}"""
                                                        .formatted(SENHA)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        // Extrai o token sem depender de parser JSON.
        return corpo.split("\"token\":\"")[1].split("\"")[0];
    }

    @Test
    @DisplayName("login devolve token e dados do usuário, sem hash de senha")
    void loginDevolveToken() throws Exception {
        String corpo =
                mockMvc.perform(
                                post("/api/sessoes")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"rafaela@rafaelasoares.vet",
                                                 "senha":"%s"}"""
                                                        .formatted(SENHA)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.token").isNotEmpty())
                        .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                        .andExpect(jsonPath("$.usuario.email").value("rafaela@rafaelasoares.vet"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(corpo).doesNotContain("senhaHash").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("senha errada e e-mail inexistente dão a MESMA resposta 401")
    void naoRevelaQuemTemConta() throws Exception {
        // Distinguir os dois casos permitiria descobrir quem tem conta.
        String comSenhaErrada =
                mockMvc.perform(
                                post("/api/sessoes")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"rafaela@rafaelasoares.vet",
                                                 "senha":"senhaErrada123"}"""))
                        .andExpect(status().isUnauthorized())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String comEmailInexistente =
                mockMvc.perform(
                                post("/api/sessoes")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"ninguem@lugar.nenhum",
                                                 "senha":"senhaErrada123"}"""))
                        .andExpect(status().isUnauthorized())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(extrairMensagem(comSenhaErrada))
                .isEqualTo(extrairMensagem(comEmailInexistente));
    }

    @Test
    @DisplayName("token autentica; depois do logout o mesmo token é recusado")
    void logoutInvalidaDeVerdade() throws Exception {
        String token = entrarEObterToken();

        mockMvc.perform(get("/api/sessoes/atual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rafaela@rafaelasoares.vet"));

        mockMvc.perform(
                        delete("/api/sessoes/atual")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // É isto que o JWT puro não daria: o token para de valer na hora.
        mockMvc.perform(get("/api/sessoes/atual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token inventado não autentica")
    void tokenInvalidoNaoAutentica() throws Exception {
        mockMvc.perform(
                        get("/api/sessoes/atual")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token-inventado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("usuário inativado perde a sessão imediatamente")
    void inativarDerrubaSessao() throws Exception {
        String token = entrarEObterToken();

        Usuario usuario =
                usuarioRepository.findByEmailIgnoreCase("rafaela@rafaelasoares.vet").orElseThrow();
        // Precisa haver outro administrador, senão a regra do último admin barra.
        usuarioRepository.save(
                Usuario.criar(
                        "Outro Admin",
                        "outro@rafaelasoares.vet",
                        passwordEncoder.encode(SENHA),
                        PerfilAcesso.ADMINISTRADOR));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                        "/api/usuarios/" + usuario.getId() + "/inativar")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sessoes/atual").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String extrairMensagem(String corpoJson) {
        return corpoJson.split("\"mensagem\":\"")[1].split("\"")[0];
    }
}
