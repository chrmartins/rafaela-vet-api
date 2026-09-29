package com.vetplanet.modules.cliente.exception;

import com.vetplanet.common.exception.BusinessRuleException;

/**
 * Tutor precisa nascer com pelo menos um animal.
 *
 * <p>Não é burocracia: tutor existe no sistema porque tem um bicho a ser
 * atendido. Cadastro de pessoa sem animal nenhum é registro órfão que ninguém
 * volta para completar — e o cadastro sempre começa com o animal em mãos
 * ("aqui é a Ana, é pro meu gato Mel").
 *
 * <p>Isto vale só na criação. Um tutor pode ficar sem animais depois, se o
 * único que tinha for inativado ou tiver óbito registrado — e nesse caso o
 * cadastro dele continua, com o histórico.
 */
public class TutorSemAnimalException extends BusinessRuleException {

    public TutorSemAnimalException() {
        super("Informe ao menos um animal para cadastrar o tutor.");
    }
}
