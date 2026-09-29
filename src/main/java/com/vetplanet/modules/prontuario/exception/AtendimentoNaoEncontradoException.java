package com.vetplanet.modules.prontuario.exception;

import com.vetplanet.common.exception.NotFoundException;
import java.util.UUID;

public class AtendimentoNaoEncontradoException extends NotFoundException {

    public AtendimentoNaoEncontradoException(UUID idAtendimento) {
        super("Atendimento " + idAtendimento + " não encontrado.");
    }
}
