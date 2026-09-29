package com.vetplanet.modules.agendamento.exception;

import com.vetplanet.common.exception.NotFoundException;
import java.util.UUID;

public class BloqueioNaoEncontradoException extends NotFoundException {

    public BloqueioNaoEncontradoException(UUID idBloqueio) {
        super("Bloqueio " + idBloqueio + " não encontrado.");
    }
}
