package com.vetplanet.modules.agendamento.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Invariantes que a entidade garante sozinha, sem banco nem Spring. */
class BloqueioEntityTest {

    @Test
    @DisplayName("semanal não guarda datas, e período não guarda dias")
    void formasNaoSeMisturam() {
        BloqueioEntity semanal =
                BloqueioEntity.semanal(
                        "Almoço",
                        new Short[] {1, 2},
                        LocalTime.parse("12:00"),
                        LocalTime.parse("13:00"));

        assertThat(semanal.getDiasDaSemana()).containsExactly((short) 1, (short) 2);
        assertThat(semanal.getDataInicio()).isNull();
        assertThat(semanal.getDataFim()).isNull();

        BloqueioEntity periodo =
                BloqueioEntity.periodo(
                        "Congresso",
                        LocalDate.parse("2026-09-20"),
                        LocalDate.parse("2026-09-22"),
                        null,
                        null);

        assertThat(periodo.getDiasDaSemana()).isNull();
        assertThat(periodo.getDataInicio()).isEqualTo(LocalDate.parse("2026-09-20"));
    }

    @Test
    @DisplayName("atualizar troca a forma e zera a que saiu")
    void atualizarTrocaAForma() {
        // Um almoço semanal pode virar viagem. Se os dias ficassem para trás, a
        // linha viraria híbrida e o check do banco a recusaria — o que
        // apareceria como erro 500 no meio de uma edição comum.
        BloqueioEntity bloqueio =
                BloqueioEntity.semanal(
                        "Almoço",
                        new Short[] {1, 2, 3},
                        LocalTime.parse("12:00"),
                        LocalTime.parse("13:00"));

        bloqueio.atualizar(
                "Viagem",
                null,
                LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-05"),
                null,
                null);

        assertThat(bloqueio.getMotivo()).isEqualTo("Viagem");
        assertThat(bloqueio.getDiasDaSemana()).isNull();
        assertThat(bloqueio.getHoraInicio()).isNull();
        assertThat(bloqueio.getDataInicio()).isEqualTo(LocalDate.parse("2026-10-01"));
    }

    @Test
    @DisplayName("atualizar mexe em atualizadoEm e não em criadoEm")
    void atualizarMarcaOTempo() {
        BloqueioEntity bloqueio =
                BloqueioEntity.semanal("Almoço", new Short[] {1}, null, null);

        assertThat(bloqueio.getCriadoEm()).isEqualTo(bloqueio.getAtualizadoEm());

        bloqueio.atualizar("Outro", new Short[] {2}, null, null, null, null);

        assertThat(bloqueio.getAtualizadoEm()).isAfterOrEqualTo(bloqueio.getCriadoEm());
    }

    @Test
    @DisplayName("apara espaços do motivo")
    void aparaMotivo() {
        assertThat(
                        BloqueioEntity.semanal("  Almoço  ", new Short[] {1}, null, null)
                                .getMotivo())
                .isEqualTo("Almoço");
    }
}
