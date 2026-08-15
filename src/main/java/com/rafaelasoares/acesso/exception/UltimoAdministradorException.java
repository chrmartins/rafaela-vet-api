package com.rafaelasoares.acesso.exception;

import com.rafaelasoares.common.exception.BusinessRuleException;

/**
 * Impede deixar o sistema sem nenhum administrador ativo.
 *
 * <p>Sem esta trava, inativar o último administrador trancaria todo mundo do
 * lado de fora de forma irreversível — não sobraria ninguém com permissão
 * para reativar alguém, e a saída seria mexer no banco à mão.
 */
public class UltimoAdministradorException extends BusinessRuleException {

    public UltimoAdministradorException() {
        super(
                "Este é o último administrador ativo. Promova outro usuário a "
                        + "administrador antes de inativá-lo.");
    }
}
