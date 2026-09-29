package com.vetplanet.modules.acesso.exception;

import com.vetplanet.common.exception.BusinessRuleException;

/**
 * Veterinário sem CRMV.
 *
 * <p>422 e não 400 porque nenhum campo isolado está errado: o problema é a
 * relação entre perfil e número. Bean Validation valida campo, não combinação.
 *
 * <p>A regra vem da Resolução CFMV nº 1.321/2020, Art. 9º, II e VIII — o
 * prontuário identifica o profissional por nome completo e número de CRMV.
 * Um veterinário sem CRMV cadastrado não consegue assinar atendimento, e
 * descobrir isso na hora de registrar seria tarde demais.
 */
public class CrmvObrigatorioException extends BusinessRuleException {

    public CrmvObrigatorioException() {
        super("Informe o CRMV: é ele que identifica o veterinário no prontuário.");
    }
}
