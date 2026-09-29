package com.vetplanet.modules.prontuario.exception;

import com.vetplanet.common.exception.BusinessRuleException;

/** Tentou concluir sem o mínimo que a Resolução CFMV nº 1.321/2020 exige. */
public class AtendimentoIncompletoException extends BusinessRuleException {

    public AtendimentoIncompletoException() {
        super(
                "Preencha o exame e o diagnóstico presuntivo antes de concluir:"
                        + " são exigidos no prontuário.");
    }
}
