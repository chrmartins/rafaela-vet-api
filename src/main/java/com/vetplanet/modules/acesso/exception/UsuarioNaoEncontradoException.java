package com.vetplanet.modules.acesso.exception;

import com.vetplanet.common.exception.NotFoundException;
import java.util.UUID;

/** Não existe usuário com o identificador informado. */
public class UsuarioNaoEncontradoException extends NotFoundException {

    public UsuarioNaoEncontradoException(UUID idUsuario) {
        super("Usuário %s não encontrado.".formatted(idUsuario));
    }
}
