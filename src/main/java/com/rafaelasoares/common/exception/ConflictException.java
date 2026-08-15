package com.rafaelasoares.common.exception;

/**
 * A operação conflita com dado que já existe. Vira <b>409</b>.
 *
 * <p>Tipicamente violação de unicidade: e-mail já cadastrado, horário já
 * ocupado. Diferente de {@link BusinessRuleException}, aqui o pedido em si é
 * válido — só esbarra no estado atual do sistema.
 */
public abstract class ConflictException extends DomainException {

    protected ConflictException(String mensagem) {
        super(mensagem);
    }
}
