package com.vetplanet.modules.acesso.exception;

import com.vetplanet.common.exception.ConflictException;

/** E-mail já pertence a outro usuário — o e-mail identifica o login. */
public class EmailJaCadastradoException extends ConflictException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário com o e-mail %s.".formatted(email));
    }
}
