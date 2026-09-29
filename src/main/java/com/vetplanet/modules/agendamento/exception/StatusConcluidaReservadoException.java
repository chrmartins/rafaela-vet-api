package com.vetplanet.modules.agendamento.exception;

import com.vetplanet.common.exception.BusinessRuleException;

/**
 * Tentou concluir a consulta pela porta lateral.
 *
 * <p>Uma consulta só vira {@code CONCLUIDA} quando o atendimento é concluído —
 * escrever é terminar. Se este endpoint aceitasse o status direto, existiria um
 * caminho para marcar a consulta como realizada sem o documento que a lei exige
 * que exista, e a regra viraria decoração.
 */
public class StatusConcluidaReservadoException extends BusinessRuleException {

    public StatusConcluidaReservadoException() {
        super(
                "Uma consulta é concluída ao concluir o atendimento dela."
                        + " Registre o atendimento para encerrá-la.");
    }
}
