package com.vetplanet.modules.acesso.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regras que a entidade garante sozinha, sem banco nem Spring.
 *
 * <p>São as invariantes que, se quebrarem, quebram silenciosamente: e-mail
 * fora do padrão fura o índice único, e hash em log vaza credencial.
 */
class UsuarioEntityTest {

    private static final String HASH_FALSO = "$2a$10$hashDeTeste";

    @Test
    @DisplayName("normaliza e-mail para minúsculas — o índice único é sobre lower(email)")
    void normalizaEmail() {
        UsuarioEntity usuario =
                UsuarioEntity.criar(
                        "Rafaela Soares",
                        "  Rafaela@RafaelaSoares.VET  ",
                        HASH_FALSO,
                        PerfilAcesso.VETERINARIO, "CRMV-RJ 12345");

        assertThat(usuario.getEmail()).isEqualTo("rafaela@rafaelasoares.vet");
    }

    @Test
    @DisplayName("remove espaços sobrando do nome")
    void removeEspacosDoNome() {
        UsuarioEntity usuario =
                UsuarioEntity.criar(
                        "  Rafaela Soares  ", "r@r.vet", HASH_FALSO, PerfilAcesso.VETERINARIO, "CRMV-RJ 12345");

        assertThat(usuario.getNomeCompleto()).isEqualTo("Rafaela Soares");
    }

    @Test
    @DisplayName("nasce ativo e com os timestamps preenchidos")
    void nasceAtivoComTimestamps() {
        UsuarioEntity usuario =
                UsuarioEntity.criar("Rafaela", "r@r.vet", HASH_FALSO, PerfilAcesso.ADMINISTRADOR, null);

        assertThat(usuario.isAtivo()).isTrue();
        // Preenchidos já no construtor, e não só no flush — foi justamente o
        // que quebrou quando dependíamos de @CreationTimestamp.
        assertThat(usuario.getCriadoEm()).isNotNull();
        assertThat(usuario.getAtualizadoEm()).isNotNull();
    }

    @Test
    @DisplayName("inativar não apaga: só desliga e marca a atualização")
    void inativaSemApagar() {
        UsuarioEntity usuario =
                UsuarioEntity.criar("Rafaela", "r@r.vet", HASH_FALSO, PerfilAcesso.ATENDENTE, null);
        var atualizadoAntes = usuario.getAtualizadoEm();

        usuario.inativar();

        assertThat(usuario.isAtivo()).isFalse();
        assertThat(usuario.getEmail()).isEqualTo("r@r.vet");
        assertThat(usuario.getAtualizadoEm()).isAfterOrEqualTo(atualizadoAntes);
    }

    @Test
    @DisplayName("toString não expõe o hash da senha — evita vazamento em log")
    void toStringNaoVazaHash() {
        UsuarioEntity usuario =
                UsuarioEntity.criar("Rafaela", "r@r.vet", HASH_FALSO, PerfilAcesso.ADMINISTRADOR, null);

        assertThat(usuario.toString()).doesNotContain(HASH_FALSO).doesNotContain("senha");
    }

    @Test
    @DisplayName("guarda o CRMV do veterinário, aparado")
    void guardaCrmvDoVeterinario() {
        UsuarioEntity vet =
                UsuarioEntity.criar(
                        "Rafaela Soares",
                        "r@r.vet",
                        HASH_FALSO,
                        PerfilAcesso.VETERINARIO,
                        "  CRMV-RJ 12345  ");

        assertThat(vet.getCrmv()).isEqualTo("CRMV-RJ 12345");
        assertThat(vet.podeAssinarProntuario()).isTrue();
    }

    @Test
    @DisplayName("descarta CRMV de quem não é veterinário — perfil não é profissão")
    void descartaCrmvDeQuemNaoAssina() {
        // Administrador com CRMV preenchido diria que ele assina prontuário, e
        // não assina. O banco recusaria a linha; a entidade nem deixa chegar lá.
        for (PerfilAcesso perfil :
                new PerfilAcesso[] {PerfilAcesso.ADMINISTRADOR, PerfilAcesso.ATENDENTE}) {
            UsuarioEntity usuario =
                    UsuarioEntity.criar("Fulano", "f@r.vet", HASH_FALSO, perfil, "CRMV-RJ 99999");

            assertThat(usuario.getCrmv()).isNull();
            assertThat(usuario.podeAssinarProntuario()).isFalse();
        }
    }

    @Test
    @DisplayName("rebaixar veterinário limpa o CRMV")
    void rebaixarLimpaOCrmv() {
        // Sem isto o número ficaria para trás e a linha violaria o check do
        // banco — o erro apareceria como 500 no meio de uma edição comum.
        UsuarioEntity usuario =
                UsuarioEntity.criar(
                        "Bruno", "b@r.vet", HASH_FALSO, PerfilAcesso.VETERINARIO, "CRMV-RJ 54321");

        usuario.atualizarDados("Bruno", "b@r.vet", PerfilAcesso.ATENDENTE, "CRMV-RJ 54321");

        assertThat(usuario.getCrmv()).isNull();
        assertThat(usuario.podeAssinarProntuario()).isFalse();
    }

    @Test
    @DisplayName("promover a veterinário passa a aceitar o CRMV")
    void promoverAceitaOCrmv() {
        UsuarioEntity usuario =
                UsuarioEntity.criar("Ana", "a@r.vet", HASH_FALSO, PerfilAcesso.ATENDENTE, null);

        usuario.atualizarDados("Ana", "a@r.vet", PerfilAcesso.VETERINARIO, "CRMV-RJ 77777");

        assertThat(usuario.getCrmv()).isEqualTo("CRMV-RJ 77777");
        assertThat(usuario.podeAssinarProntuario()).isTrue();
    }

    @Test
    @DisplayName("veterinário sem CRMV não assina — a entidade não inventa número")
    void veterinarioSemCrmvNaoAssina() {
        // A entidade aceita construir: quem exige o número é o service, para a
        // mensagem chegar ao lado do campo. O que ela garante é não mentir
        // sobre quem pode assinar.
        UsuarioEntity vet =
                UsuarioEntity.criar("Sem CRMV", "s@r.vet", HASH_FALSO, PerfilAcesso.VETERINARIO, "  ");

        assertThat(vet.getCrmv()).isNull();
        assertThat(vet.podeAssinarProntuario()).isFalse();
    }
}
