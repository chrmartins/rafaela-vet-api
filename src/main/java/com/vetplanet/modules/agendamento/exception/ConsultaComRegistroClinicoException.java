package com.vetplanet.modules.agendamento.exception;

import com.vetplanet.common.exception.BusinessRuleException;

/**
 * Tentou mexer numa consulta que já virou prontuário.
 *
 * <p>Consulta com atendimento concluído <b>aconteceu</b>: mudar a hora dela
 * seria reescrever um fato registrado. A correção passa a ser retificação do
 * atendimento, não edição da consulta.
 *
 * <p>É exatamente a trava que o CLAUDE.md previu ao dizer que ela viria "do que
 * está preso à consulta, não da passagem do tempo". O prontuário chegou; a
 * trava chegou com ele.
 */
public class ConsultaComRegistroClinicoException extends BusinessRuleException {

    public ConsultaComRegistroClinicoException() {
        super(
                "Esta consulta já tem atendimento registrado e não pode ser alterada."
                        + " Para corrigir, registre uma retificação no atendimento.");
    }
}
