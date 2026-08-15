package com.rafaelasoares.acesso.exception;

import com.rafaelasoares.common.exception.NotFoundException;
import java.util.UUID;

/** Não existe usuário com o identificador informado. */
public class UsuarioNaoEncontradoException extends NotFoundException {

    public UsuarioNaoEncontradoException(UUID idUsuario) {
        super("Usuário %s não encontrado.".formatted(idUsuario));
    }
}
