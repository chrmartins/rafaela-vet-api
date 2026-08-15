package com.rafaelasoares.acesso.exception;

import com.rafaelasoares.common.exception.DomainException;

/**
 * E-mail não existe, senha não confere, ou usuário inativo.
 *
 * <p>É <b>uma exceção só para os três casos</b>, e a mensagem é sempre a
 * mesma, de propósito: distinguir "e-mail não cadastrado" de "senha errada"
 * permitiria descobrir quem tem conta no sistema (enumeração de usuários).
 *
 * <p>Não estende as categorias de {@code common.exception} porque vira
 * <b>401</b>, que é falha de autenticação e não de regra de negócio — o
 * {@code ApiExceptionHandler} trata este caso à parte.
 */
public class CredenciaisInvalidasException extends DomainException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos.");
    }
}
