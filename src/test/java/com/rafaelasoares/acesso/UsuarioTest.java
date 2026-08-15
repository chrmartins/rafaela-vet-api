package com.rafaelasoares.acesso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regras que a entidade garante sozinha, sem banco nem Spring.
 *
 * <p>São as invariantes que, se quebrarem, quebram silenciosamente: e-mail
 * fora do padrão fura o índice único, e hash em log vaza credencial.
 */
class UsuarioTest {

    private static final String HASH_FALSO = "$2a$10$hashDeTeste";

    @Test
    @DisplayName("normaliza e-mail para minúsculas — o índice único é sobre lower(email)")
    void normalizaEmail() {
        Usuario usuario =
                Usuario.criar(
                        "Rafaela Soares",
                        "  Rafaela@RafaelaSoares.VET  ",
                        HASH_FALSO,
                        PerfilAcesso.VETERINARIO);

        assertThat(usuario.getEmail()).isEqualTo("rafaela@rafaelasoares.vet");
    }

    @Test
    @DisplayName("remove espaços sobrando do nome")
    void removeEspacosDoNome() {
        Usuario usuario =
                Usuario.criar(
                        "  Rafaela Soares  ", "r@r.vet", HASH_FALSO, PerfilAcesso.VETERINARIO);

        assertThat(usuario.getNomeCompleto()).isEqualTo("Rafaela Soares");
    }

    @Test
    @DisplayName("nasce ativo e com os timestamps preenchidos")
    void nasceAtivoComTimestamps() {
        Usuario usuario =
                Usuario.criar("Rafaela", "r@r.vet", HASH_FALSO, PerfilAcesso.ADMINISTRADOR);

        assertThat(usuario.isAtivo()).isTrue();
        // Preenchidos já no construtor, e não só no flush — foi justamente o
        // que quebrou quando dependíamos de @CreationTimestamp.
        assertThat(usuario.getCriadoEm()).isNotNull();
        assertThat(usuario.getAtualizadoEm()).isNotNull();
    }

    @Test
    @DisplayName("inativar não apaga: só desliga e marca a atualização")
    void inativaSemApagar() {
        Usuario usuario =
                Usuario.criar("Rafaela", "r@r.vet", HASH_FALSO, PerfilAcesso.ATENDENTE);
        var atualizadoAntes = usuario.getAtualizadoEm();

        usuario.inativar();

        assertThat(usuario.isAtivo()).isFalse();
        assertThat(usuario.getEmail()).isEqualTo("r@r.vet");
        assertThat(usuario.getAtualizadoEm()).isAfterOrEqualTo(atualizadoAntes);
    }

    @Test
    @DisplayName("toString não expõe o hash da senha — evita vazamento em log")
    void toStringNaoVazaHash() {
        Usuario usuario =
                Usuario.criar("Rafaela", "r@r.vet", HASH_FALSO, PerfilAcesso.ADMINISTRADOR);

        assertThat(usuario.toString()).doesNotContain(HASH_FALSO).doesNotContain("senha");
    }
}
