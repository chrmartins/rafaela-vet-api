package com.vetplanet.modules.agendamento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetplanet.modules.agendamento.dto.CriarBloqueioRequestDto;
import com.vetplanet.modules.agendamento.exception.BloqueioInvalidoException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * As regras cruzadas do bloqueio, sem banco nem Spring.
 *
 * <p>São regras que nenhuma anotação de campo pega — dependem da combinação —
 * e que criar e atualizar compartilham. Testá-las aqui é o que impede as duas
 * rotas de divergirem: se alguém afrouxar uma regra, os dois caminhos quebram
 * junto, que é o comportamento desejado.
 *
 * <p>O banco tem os mesmos checks ({@code V005}, {@code V006}). Não é
 * duplicação inútil: aqui garante a mensagem legível, lá garante que nenhuma
 * linha torta entre por outro caminho.
 */
class FormaDoBloqueioTest {

    private static CriarBloqueioRequestDto semanal(List<Short> dias, String de, String ate) {
        return new CriarBloqueioRequestDto(
                "Almoço",
                dias,
                null,
                null,
                de == null ? null : LocalTime.parse(de),
                ate == null ? null : LocalTime.parse(ate));
    }

    private static CriarBloqueioRequestDto periodo(String de, String ate) {
        return new CriarBloqueioRequestDto(
                "Congresso", null, LocalDate.parse(de), LocalDate.parse(ate), null, null);
    }

    @Nested
    @DisplayName("forma")
    class Forma {

        @Test
        @DisplayName("semanal com dias e horas é aceito")
        void semanalValido() {
            FormaDoBloqueio forma =
                    FormaDoBloqueio.de(semanal(List.of((short) 1, (short) 2), "12:00", "13:00"));

            assertThat(forma.ehSemanal()).isTrue();
            assertThat(forma.diasDaSemana()).containsExactly((short) 1, (short) 2);
            assertThat(forma.dataInicio()).isNull();
        }

        @Test
        @DisplayName("período com as duas datas é aceito")
        void periodoValido() {
            FormaDoBloqueio forma = FormaDoBloqueio.de(periodo("2026-09-20", "2026-09-22"));

            assertThat(forma.ehSemanal()).isFalse();
            assertThat(forma.diasDaSemana()).isNull();
            assertThat(forma.dataFim()).isEqualTo(LocalDate.parse("2026-09-22"));
        }

        @Test
        @DisplayName("dias da semana E período juntos é recusado — seria a linha híbrida")
        void hibridoRecusado() {
            CriarBloqueioRequestDto hibrido =
                    new CriarBloqueioRequestDto(
                            "Confuso",
                            List.of((short) 1),
                            LocalDate.parse("2026-09-20"),
                            LocalDate.parse("2026-09-22"),
                            null,
                            null);

            assertThatThrownBy(() -> FormaDoBloqueio.de(hibrido))
                    .isInstanceOf(BloqueioInvalidoException.class)
                    .hasMessageContaining("não os dois");
        }

        @Test
        @DisplayName("nenhuma das duas formas é recusado")
        void semFormaRecusado() {
            assertThatThrownBy(() -> FormaDoBloqueio.de(semanal(null, null, null)))
                    .isInstanceOf(BloqueioInvalidoException.class);
        }

        @Test
        @DisplayName("lista de dias vazia conta como nenhuma forma")
        void listaVaziaRecusada() {
            // Sem isto, a lista vazia passaria pelo check de forma e criaria um
            // bloqueio semanal que não cai em dia nenhum.
            assertThatThrownBy(() -> FormaDoBloqueio.de(semanal(List.of(), null, null)))
                    .isInstanceOf(BloqueioInvalidoException.class);
        }

        @Test
        @DisplayName("período com só uma das datas é recusado")
        void periodoIncompletoRecusado() {
            CriarBloqueioRequestDto soInicio =
                    new CriarBloqueioRequestDto(
                            "Viagem", null, LocalDate.parse("2026-09-20"), null, null, null);

            assertThatThrownBy(() -> FormaDoBloqueio.de(soInicio))
                    .isInstanceOf(BloqueioInvalidoException.class)
                    .hasMessageContaining("início e de fim");
        }

        @Test
        @DisplayName("fim antes do início é recusado")
        void periodoInvertidoRecusado() {
            assertThatThrownBy(() -> FormaDoBloqueio.de(periodo("2026-09-22", "2026-09-20")))
                    .isInstanceOf(BloqueioInvalidoException.class)
                    .hasMessageContaining("antes da de início");
        }

        @Test
        @DisplayName("período de um dia só é aceito")
        void periodoDeUmDia() {
            assertThat(FormaDoBloqueio.de(periodo("2026-09-20", "2026-09-20")).dataInicio())
                    .isEqualTo(LocalDate.parse("2026-09-20"));
        }
    }

    @Nested
    @DisplayName("dias da semana")
    class DiasDaSemana {

        @Test
        @DisplayName("ordena e tira repetidos")
        void normaliza() {
            // Marcar segunda duas vezes na tela gravaria o dia duplicado, e a
            // lista sairia "sexta, segunda" em vez de "segunda a sexta".
            FormaDoBloqueio forma =
                    FormaDoBloqueio.de(
                            semanal(
                                    List.of(
                                            (short) 5,
                                            (short) 1,
                                            (short) 3,
                                            (short) 2,
                                            (short) 4,
                                            (short) 1),
                                    "12:00",
                                    "13:00"));

            assertThat(forma.diasDaSemana())
                    .containsExactly((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        }

        @Test
        @DisplayName("aceita domingo (0) e sábado (6) — as pontas")
        void aceitaAsPontas() {
            assertThat(FormaDoBloqueio.de(semanal(List.of((short) 0, (short) 6), null, null))
                            .diasDaSemana())
                    .containsExactly((short) 0, (short) 6);
        }

        @Test
        @DisplayName("recusa dia fora de 0..6")
        void recusaDiaInvalido() {
            for (short dia : new short[] {-1, 7, 9}) {
                assertThatThrownBy(() -> FormaDoBloqueio.de(semanal(List.of(dia), null, null)))
                        .isInstanceOf(BloqueioInvalidoException.class)
                        .hasMessageContaining("Dia da semana inválido");
            }
        }
    }

    @Nested
    @DisplayName("horário")
    class Horario {

        @Test
        @DisplayName("as duas horas vazias é o dia inteiro, e é legítimo")
        void diaInteiro() {
            // É o "não atendo domingo" e o congresso.
            FormaDoBloqueio forma = FormaDoBloqueio.de(semanal(List.of((short) 0), null, null));

            assertThat(forma.horaInicio()).isNull();
            assertThat(forma.horaFim()).isNull();
        }

        @Test
        @DisplayName("uma hora só é recusado — bloqueio sem começo ou sem fim")
        void horaSolitariaRecusada() {
            assertThatThrownBy(
                            () -> FormaDoBloqueio.de(semanal(List.of((short) 1), "12:00", null)))
                    .isInstanceOf(BloqueioInvalidoException.class)
                    .hasMessageContaining("as duas horas");

            assertThatThrownBy(
                            () -> FormaDoBloqueio.de(semanal(List.of((short) 1), null, "13:00")))
                    .isInstanceOf(BloqueioInvalidoException.class);
        }

        @Test
        @DisplayName("fim antes ou igual ao início é recusado")
        void horarioInvertidoRecusado() {
            for (String fim : new String[] {"11:00", "12:00"}) {
                assertThatThrownBy(
                                () ->
                                        FormaDoBloqueio.de(
                                                semanal(List.of((short) 1), "12:00", fim)))
                        .isInstanceOf(BloqueioInvalidoException.class)
                        .hasMessageContaining("depois da de início");
            }
        }
    }
}
