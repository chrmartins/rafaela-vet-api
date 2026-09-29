package com.vetplanet.modules.agendamento.exception;

import com.vetplanet.common.exception.BusinessRuleException;

/**
 * O corpo é bem formado, mas a combinação de campos não descreve um bloqueio.
 *
 * <p>422 e não 400 porque nenhum campo isolado está errado: o problema é a
 * relação entre eles — mandar dia da semana <i>e</i> período, ou hora de
 * início sem hora de fim. Bean Validation valida campo, não combinação.
 */
public class BloqueioInvalidoException extends BusinessRuleException {

    public BloqueioInvalidoException(String mensagem) {
        super(mensagem);
    }
}
