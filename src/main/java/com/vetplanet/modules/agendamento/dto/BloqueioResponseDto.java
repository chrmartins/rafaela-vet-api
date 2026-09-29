package com.vetplanet.modules.agendamento.dto;

import com.vetplanet.modules.agendamento.entity.BloqueioEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Bloqueio como sai da API.
 *
 * <p>Sai <b>como foi guardado</b>: a regra, não as ocorrências dela. Expandir
 * um almoço semanal nos 30 dias do mês exibido seria trabalho do backend
 * chutando qual período interessa ao cliente — e a tela, que já sabe que dia
 * está desenhando, resolve com uma comparação de dia da semana.
 */
public record BloqueioResponseDto(
        UUID idBloqueio,
        String motivo,
        List<Short> diasDaSemana,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    public static BloqueioResponseDto de(BloqueioEntity bloqueio) {
        return new BloqueioResponseDto(
                bloqueio.getId(),
                bloqueio.getMotivo(),
                bloqueio.getDiasDaSemana() == null
                        ? null
                        : Arrays.asList(bloqueio.getDiasDaSemana()),
                bloqueio.getDataInicio(),
                bloqueio.getDataFim(),
                bloqueio.getHoraInicio(),
                bloqueio.getHoraFim(),
                bloqueio.getCriadoEm(),
                bloqueio.getAtualizadoEm());
    }
}
