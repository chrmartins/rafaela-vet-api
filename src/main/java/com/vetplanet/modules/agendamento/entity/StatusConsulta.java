package com.vetplanet.modules.agendamento.entity;

/**
 * Ciclo de vida de uma consulta.
 *
 * <p>Consulta <b>nunca é excluída</b> — muda de status. Isso não é detalhe de
 * implementação: é o que mantém o histórico clínico íntegro, e é o que faz o
 * banco recusar a exclusão de um animal que já foi atendido.
 *
 * <p>Urgência <b>não está aqui</b>, e é de propósito: uma consulta urgente
 * também está solicitada ou confirmada. São dois eixos, e misturá-los
 * obrigaria a escolher entre dizer que é urgente e dizer se está confirmada.
 */
public enum StatusConsulta {
    /** Combinada por cima, ainda sem horário fechado com o tutor. */
    SOLICITADA,
    CONFIRMADA,
    CANCELADA,
    CONCLUIDA
}
