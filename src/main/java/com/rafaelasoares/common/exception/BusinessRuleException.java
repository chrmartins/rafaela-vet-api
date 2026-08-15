package com.rafaelasoares.common.exception;

/**
 * A requisição está bem formada, mas fere uma regra de negócio. Vira
 * <b>422 Unprocessable Entity</b>.
 *
 * <p>É o caso de coisas como "prontuário confirmado não pode ser editado, só
 * retificado" ou "não é possível agendar em bloco indisponível" — situações
 * que nenhuma validação de campo pegaria, porque dependem do estado do
 * domínio, não do formato do dado.
 *
 * <p>Se o problema for o <i>formato</i> do dado, isso é 400 e quem resolve é
 * o Bean Validation nos DTOs, não esta exceção.
 */
public abstract class BusinessRuleException extends DomainException {

    protected BusinessRuleException(String mensagem) {
        super(mensagem);
    }
}
