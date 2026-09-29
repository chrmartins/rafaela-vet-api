package com.vetplanet.modules.cliente.exception;

import com.vetplanet.common.exception.NotFoundException;
import java.util.UUID;

/** Não existe animal com o identificador informado. */
public class AnimalNaoEncontradoException extends NotFoundException {

    public AnimalNaoEncontradoException(UUID idAnimal) {
        super("Animal %s não encontrado.".formatted(idAnimal));
    }
}
