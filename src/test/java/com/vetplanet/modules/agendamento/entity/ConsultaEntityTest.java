package com.vetplanet.modules.agendamento.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Invariantes da consulta, sem banco nem Spring. */
class ConsultaEntityTest {

    private static final UUID TUTOR = UUID.randomUUID();
    private static final UUID ANIMAL = UUID.randomUUID();
    private static final OffsetDateTime QUINTA_14H30 =
            OffsetDateTime.parse("2026-09-10T17:30:00Z");

    private static ConsultaEntity umaConsulta() {
        return ConsultaEntity.criar(
                TUTOR,
                ANIMAL,
                QUINTA_14H30,
                45,
                "  Retorno pós-cirúrgico  ",
                StatusConsulta.CONFIRMADA,
                false,
                "   ",
                null);
    }

    @Test
    @DisplayName("apara o motivo e transforma texto em branco em nulo")
    void normalizaTextos() {
        ConsultaEntity consulta = umaConsulta();

        assertThat(consulta.getMotivo()).isEqualTo("Retorno pós-cirúrgico");
        // Endereço só com espaços vira nulo: "" e "não informado" são coisas
        // diferentes, e a coluna guarda nulo quando não se sabe para onde ir.
        assertThat(consulta.getEnderecoAtendimento()).isNull();
        assertThat(consulta.getObservacoes()).isNull();
    }

    @Test
    @DisplayName("reagendar move no tempo e preserva o status")
    void reagendarPreservaStatus() {
        // Remarcar não é cancelar e criar de novo: a consulta continua a mesma,
        // com a mesma situação, só que noutra hora.
        ConsultaEntity consulta = umaConsulta();

        consulta.reagendar(OffsetDateTime.parse("2026-09-10T19:00:00Z"), 60);

        assertThat(consulta.getDataHora()).isEqualTo(OffsetDateTime.parse("2026-09-10T19:00:00Z"));
        assertThat(consulta.getDuracaoMinutos()).isEqualTo(60);
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.CONFIRMADA);
        assertThat(consulta.getIdAnimal()).isEqualTo(ANIMAL);
    }

    @Test
    @DisplayName("atualizarDados não mexe no horário nem no animal")
    void atualizarDadosNaoMoveAConsulta() {
        ConsultaEntity consulta = umaConsulta();

        consulta.atualizarDados("Vacinação", true, "Rua A, 10", "Levar caixa de transporte");

        assertThat(consulta.getMotivo()).isEqualTo("Vacinação");
        assertThat(consulta.isUrgente()).isTrue();
        assertThat(consulta.getEnderecoAtendimento()).isEqualTo("Rua A, 10");
        assertThat(consulta.getDataHora()).isEqualTo(QUINTA_14H30);
        assertThat(consulta.getIdAnimal()).isEqualTo(ANIMAL);
    }

    @Test
    @DisplayName("alterarStatus aceita qualquer transição — inclusive desfazer")
    void statusVaiEVolta() {
        // Sem máquina de estados de propósito: a veterinária é a única
        // operadora, e corrigir um clique errado precisa ser trivial.
        ConsultaEntity consulta = umaConsulta();

        consulta.alterarStatus(StatusConsulta.CONCLUIDA);
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.CONCLUIDA);

        consulta.alterarStatus(StatusConsulta.CONFIRMADA);
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.CONFIRMADA);

        consulta.alterarStatus(StatusConsulta.CANCELADA);
        consulta.alterarStatus(StatusConsulta.SOLICITADA);
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.SOLICITADA);
    }

    @Test
    @DisplayName("guarda o tutor da época, além do animal")
    void guardaOTutorDaEpoca() {
        // Tutor muda: adoção, venda, o filho que assume o cachorro dos pais.
        // Sem isso, o histórico de dois anos atrás exibiria o dono de hoje.
        assertThat(umaConsulta().getIdTutor()).isEqualTo(TUTOR);
    }

    @Test
    @DisplayName("urgência é atributo, não status — convivem")
    void urgenciaConviveComStatus() {
        ConsultaEntity urgente =
                ConsultaEntity.criar(
                        TUTOR,
                        ANIMAL,
                        QUINTA_14H30,
                        30,
                        "Emergência",
                        StatusConsulta.SOLICITADA,
                        true,
                        null,
                        null);

        assertThat(urgente.isUrgente()).isTrue();
        assertThat(urgente.getStatus()).isEqualTo(StatusConsulta.SOLICITADA);
    }
}
