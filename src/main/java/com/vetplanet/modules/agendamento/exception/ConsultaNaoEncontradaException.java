package com.vetplanet.modules.agendamento.exception;

import com.vetplanet.common.exception.NotFoundException;
import java.util.UUID;

/** Não existe consulta com o identificador informado. */
public class ConsultaNaoEncontradaException extends NotFoundException {

    public ConsultaNaoEncontradaException(UUID idConsulta) {
        super("Consulta %s não encontrada.".formatted(idConsulta));
    }
}
