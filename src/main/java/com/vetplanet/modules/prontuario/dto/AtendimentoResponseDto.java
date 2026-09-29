package com.vetplanet.modules.prontuario.dto;

import com.vetplanet.modules.prontuario.entity.AtendimentoEntity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * O atendimento como sai da API.
 *
 * <p>{@code concluidoEm} nulo é rascunho — a tela usa para decidir entre
 * "Continuar atendimento" e "Ver atendimento", e para saber se ainda pode
 * editar.
 */
public record AtendimentoResponseDto(
        UUID idAtendimento,
        UUID idConsulta,
        UUID idAnimal,
        UUID idVeterinario,
        String nomeVeterinario,
        String crmvVeterinario,
        String localAtendimento,
        String anamnese,
        String exameClinico,
        String diagnosticoPresuntivo,
        String diagnosticoConclusivo,
        String recomendacoes,
        String prescricao,
        BigDecimal pesoKg,
        BigDecimal temperaturaC,
        Integer frequenciaCardiaca,
        Integer frequenciaRespiratoria,
        OffsetDateTime concluidoEm,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    public static AtendimentoResponseDto de(AtendimentoEntity atendimento) {
        return new AtendimentoResponseDto(
                atendimento.getId(),
                atendimento.getIdConsulta(),
                atendimento.getIdAnimal(),
                atendimento.getIdVeterinario(),
                atendimento.getNomeVeterinario(),
                atendimento.getCrmvVeterinario(),
                atendimento.getLocalAtendimento(),
                atendimento.getAnamnese(),
                atendimento.getExameClinico(),
                atendimento.getDiagnosticoPresuntivo(),
                atendimento.getDiagnosticoConclusivo(),
                atendimento.getRecomendacoes(),
                atendimento.getPrescricao(),
                atendimento.getPesoKg(),
                atendimento.getTemperaturaC(),
                atendimento.getFrequenciaCardiaca(),
                atendimento.getFrequenciaRespiratoria(),
                atendimento.getConcluidoEm(),
                atendimento.getCriadoEm(),
                atendimento.getAtualizadoEm());
    }
}
