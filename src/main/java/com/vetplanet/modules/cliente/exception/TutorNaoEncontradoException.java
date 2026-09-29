package com.vetplanet.modules.cliente.exception;

import com.vetplanet.common.exception.NotFoundException;
import java.util.UUID;

/** Não existe tutor com o identificador informado. */
public class TutorNaoEncontradoException extends NotFoundException {

    public TutorNaoEncontradoException(UUID idTutor) {
        super("Tutor %s não encontrado.".formatted(idTutor));
    }
}
