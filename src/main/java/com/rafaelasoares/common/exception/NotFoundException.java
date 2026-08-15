package com.rafaelasoares.common.exception;

/**
 * O recurso pedido não existe. Vira <b>404</b>.
 *
 * <p>Exemplos de subclasse: {@code UsuarioNaoEncontradoException},
 * {@code AnimalNaoEncontradoException}.
 */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String mensagem) {
        super(mensagem);
    }
}
