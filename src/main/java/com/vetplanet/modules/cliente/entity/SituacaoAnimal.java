package com.vetplanet.modules.cliente.entity;

/**
 * Situação do animal no cadastro.
 *
 * <p><b>{@code INATIVO} não quer dizer "parou de aparecer".</b> Um animal pode
 * passar cinco anos sem consulta e voltar; basta continuar no cadastro.
 * Ausência é fato que se observa — a data da última consulta já diz — e não
 * estado que alguém precisa declarar. Um campo assim nunca seria marcado, e
 * campo que nunca é marcado faz {@code ATIVO} não significar nada.
 *
 * <p>O que {@code INATIVO} faz é <b>sumir da lista</b>: é o substituto da
 * exclusão para o animal que já tem histórico e por isso não pode ser apagado.
 * Mesmo papel que {@code ativo} cumpre em tutor e usuário.
 *
 * <p>Cadastro criado por engano, ainda sem histórico nenhum, não vira status —
 * é excluído de verdade. Ver {@code ExcluirAnimalService}.
 */
public enum SituacaoAnimal {
    ATIVO,
    /** Sumiu da lista. Tem histórico, então não pôde ser excluído. */
    INATIVO,
    OBITO
}
