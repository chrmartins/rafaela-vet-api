package com.vetplanet.modules.cliente.exception;

import com.vetplanet.common.exception.ConflictException;

/**
 * Tentou excluir um animal que já tem histórico.
 *
 * <p>Excluir é permitido só enquanto não há nada a perder — cadastro criado
 * por engano, duplicata. A partir da primeira consulta, o cadastro vira a
 * âncora do histórico clínico, e apagá-lo apagaria prontuário junto. O
 * {@code CLAUDE.md} é explícito: não há exclusão física de consulta nem de
 * prontuário.
 *
 * <p>Se o animal faleceu, o caminho é registrar o óbito — que preserva tudo.
 */
public class AnimalComHistoricoException extends ConflictException {

    public AnimalComHistoricoException(String nome) {
        super(
                ("%s já tem histórico no sistema e não pode ser excluído. "
                                + "Se ele faleceu, registre o óbito — o histórico continua guardado.")
                        .formatted(nome));
    }
}
