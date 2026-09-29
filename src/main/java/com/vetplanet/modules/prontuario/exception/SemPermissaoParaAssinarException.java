package com.vetplanet.modules.prontuario.exception;

import com.vetplanet.common.exception.BusinessRuleException;

/**
 * Quem tentou registrar não é veterinário com CRMV.
 *
 * <p>A Resolução CFMV nº 1.321/2020, Art. 9º, II e VIII, exige que o prontuário
 * identifique o profissional por nome completo e número de CRMV. Uma atendente
 * pode marcar consulta e escrever "portão dos fundos"; não pode assinar
 * registro clínico.
 *
 * <p><b>Perfil não é profissão:</b> o administrador do sistema também não
 * assina, a menos que seja veterinário com CRMV cadastrado.
 */
public class SemPermissaoParaAssinarException extends BusinessRuleException {

    public SemPermissaoParaAssinarException() {
        super(
                "Só um veterinário com CRMV cadastrado pode registrar atendimento."
                        + " Confira o CRMV no seu cadastro de usuário.");
    }
}
