package com.vetplanet.modules.cliente.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Invariantes do tutor, sem banco nem Spring. */
class TutorEntityTest {

    private static TutorEntity umTutor() {
        return TutorEntity.criar(
                "  Ana Prado  ", "21999990000", "  Ana@Exemplo.COM  ", null, "   ");
    }

    @Test
    @DisplayName("nasce ativo")
    void nasceAtivo() {
        assertThat(umTutor().isAtivo()).isTrue();
    }

    @Test
    @DisplayName("apara o nome e transforma texto em branco em nulo")
    void normalizaTextos() {
        TutorEntity tutor = umTutor();

        assertThat(tutor.getNomeCompleto()).isEqualTo("Ana Prado");
        assertThat(tutor.getObservacoes()).isNull();
    }

    @Test
    @DisplayName("inativar e reativar — não existe exclusão de tutor")
    void inativarEReativar() {
        // O histórico de consultas precisa continuar apontando para quem era o
        // responsável, então o mais próximo de "excluir" é inativar.
        TutorEntity tutor = umTutor();

        tutor.inativar();
        assertThat(tutor.isAtivo()).isFalse();

        tutor.reativar();
        assertThat(tutor.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("atualizarDados não mexe em ativo")
    void atualizarNaoMexeEmAtivo() {
        TutorEntity tutor = umTutor();
        tutor.inativar();

        tutor.atualizarDados("Ana Prado Silva", "21999990000", null, null, null);

        assertThat(tutor.isAtivo()).isFalse();
        assertThat(tutor.getNomeCompleto()).isEqualTo("Ana Prado Silva");
    }
}
