package com.rafaelasoares.acesso.exception;

import com.rafaelasoares.common.exception.ConflictException;

/** E-mail já pertence a outro usuário — o e-mail identifica o login. */
public class EmailJaCadastradoException extends ConflictException {

    public EmailJaCadastradoException(String email) {
        super("Já existe um usuário com o e-mail %s.".formatted(email));
    }
}
