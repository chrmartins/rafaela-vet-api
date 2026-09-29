package com.vetplanet.modules.cliente.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * O animal tem algo preso a ele?
 *
 * <p><b>A interface é declarada aqui, em `cliente`, e implementada lá fora.</b>
 * É inversão de dependência de propósito: `agendamento` já depende de
 * `cliente` (precisa do animal para marcar consulta), e se `cliente`
 * importasse `agendamento` de volta teríamos um ciclo entre módulos — o
 * primeiro sinal de que a fronteira está furada.
 *
 * <p>Com a interface deste lado, `cliente` pergunta sem conhecer quem
 * responde. `prontuario` entra como segunda implementação quando existir, sem
 * tocar em nada aqui.
 *
 * <p>Serve só para a **aparência** da tela — esconder a lixeira quando o
 * delete fosse falhar. Quem de fato impede é a chave estrangeira no banco.
 */
public interface HistoricoDoAnimal {

    boolean temHistorico(UUID idAnimal);

    /**
     * Quando cada um destes animais foi atendido pela última vez.
     *
     * <p>Serve para ordenar a lista de animais por quem foi visto mais
     * recentemente, que é a ordem em que a veterinária pensa na clientela.
     *
     * <p><b>Em lote, e não um por animal.</b> Uma consulta por linha
     * transformaria a lista em N+1 — e a lista existe justamente para varrer
     * muitos de uma vez.
     *
     * <p>Animal que nunca foi atendido simplesmente <b>não aparece no mapa</b>,
     * em vez de aparecer com nulo. Quem chama decide o que fazer com a ausência
     * sem precisar distinguir "não veio" de "veio vazio".
     */
    Map<UUID, Instant> ultimoAtendimentoPorAnimal(Collection<UUID> idsDeAnimais);
}
