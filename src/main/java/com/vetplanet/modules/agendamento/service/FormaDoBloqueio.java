package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.dto.CriarBloqueioRequestDto;
import com.vetplanet.modules.agendamento.entity.BloqueioEntity;
import com.vetplanet.modules.agendamento.exception.BloqueioInvalidoException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * O pedido de bloqueio já decidido e conferido: semanal ou período.
 *
 * <p>Existe porque criar e atualizar aplicam <b>exatamente</b> as mesmas
 * regras — atualizar substitui o conteúdo inteiro, inclusive a forma. Deixar
 * a validação em cada service faria as duas listas divergirem no primeiro
 * ajuste, e é o tipo de divergência que ninguém percebe até um caminho aceitar
 * o que o outro recusa.
 *
 * <p>Package-private de propósito: é peça interna do domínio, não contrato.
 *
 * <p>O banco tem os mesmos checks ({@code V005}, {@code V006}). Não é
 * duplicação inútil — a checagem daqui devolve uma mensagem que a pessoa
 * entende, e a do banco garante que nenhuma linha torta entre por outro
 * caminho.
 */
record FormaDoBloqueio(
        String motivo,
        Short[] diasDaSemana,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim) {

    static FormaDoBloqueio de(CriarBloqueioRequestDto request) {
        List<Short> dias = request.diasDaSemana();
        boolean ehSemanal = dias != null && !dias.isEmpty();
        boolean temPeriodo = request.dataInicio() != null || request.dataFim() != null;

        if (ehSemanal && temPeriodo) {
            throw new BloqueioInvalidoException(
                    "Um bloqueio ou repete toda semana, ou vale para um período — não os dois.");
        }
        if (!ehSemanal && !temPeriodo) {
            throw new BloqueioInvalidoException(
                    "Escolha ao menos um dia da semana, para repetir, ou informe as datas de"
                            + " início e fim.");
        }
        if (temPeriodo && (request.dataInicio() == null || request.dataFim() == null)) {
            throw new BloqueioInvalidoException("Informe as datas de início e de fim.");
        }
        if (temPeriodo && request.dataFim().isBefore(request.dataInicio())) {
            throw new BloqueioInvalidoException("A data de fim não pode ser antes da de início.");
        }
        if ((request.horaInicio() == null) != (request.horaFim() == null)) {
            throw new BloqueioInvalidoException(
                    "Informe as duas horas, ou nenhuma para bloquear o dia inteiro.");
        }
        if (request.horaInicio() != null && !request.horaFim().isAfter(request.horaInicio())) {
            throw new BloqueioInvalidoException("A hora de fim precisa ser depois da de início.");
        }
        // Bloqueio semanal de dia inteiro é legítimo: é o "não atendo domingo".

        return new FormaDoBloqueio(
                request.motivo(),
                ehSemanal ? normalizarDias(dias) : null,
                request.dataInicio(),
                request.dataFim(),
                request.horaInicio(),
                request.horaFim());
    }

    /**
     * Ordena e tira repetidos.
     *
     * <p>{@code [5, 1, 1]} vira {@code [1, 5]}. Sem isso, marcar segunda duas
     * vezes na tela gravaria o dia duplicado, e a lista sairia fora de ordem —
     * "sexta, segunda" em vez de "segunda, sexta".
     */
    private static Short[] normalizarDias(List<Short> dias) {
        for (Short dia : dias) {
            if (dia == null || dia < 0 || dia > 6) {
                throw new BloqueioInvalidoException("Dia da semana inválido: " + dia + ".");
            }
        }
        SortedSet<Short> ordenados = new TreeSet<>(dias);
        return ordenados.toArray(new Short[0]);
    }

    boolean ehSemanal() {
        return diasDaSemana != null;
    }

    BloqueioEntity novaEntidade() {
        return ehSemanal()
                ? BloqueioEntity.semanal(motivo, diasDaSemana, horaInicio, horaFim)
                : BloqueioEntity.periodo(motivo, dataInicio, dataFim, horaInicio, horaFim);
    }

    void aplicarEm(BloqueioEntity bloqueio) {
        bloqueio.atualizar(motivo, diasDaSemana, dataInicio, dataFim, horaInicio, horaFim);
    }
}
