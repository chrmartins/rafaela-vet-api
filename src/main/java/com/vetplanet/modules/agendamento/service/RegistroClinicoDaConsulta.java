package com.vetplanet.modules.agendamento.service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * A consulta já virou registro clínico?
 *
 * <p><b>A interface é declarada aqui, em `agendamento`, e implementada em
 * `prontuario`.</b> Inversão de dependência pelo mesmo motivo de
 * {@code HistoricoDoAnimal}: `prontuario` já depende de `agendamento` — precisa
 * da consulta para abrir o atendimento —, e se `agendamento` importasse
 * `prontuario` de volta teríamos um ciclo entre módulos.
 *
 * <p>Serve a duas coisas, e a diferença entre elas importa:
 *
 * <ul>
 *   <li>{@link #temAtendimentoConcluido} <b>trava</b> o remarcar. Consulta que
 *       já foi registrada aconteceu; mudar a hora dela seria reescrever um
 *       fato. É a trava que o CLAUDE.md previu quando dizia que ela viria "do
 *       que está preso à consulta, não da passagem do tempo".
 *   <li>{@link #consultasComAtendimento} é só <b>aparência</b>: a Agenda usa
 *       para o cartão dizer "Iniciar" ou "Ver". Se falhasse, ninguém perderia
 *       dado — só o rótulo ficaria errado.
 * </ul>
 */
public interface RegistroClinicoDaConsulta {

    boolean temAtendimentoConcluido(UUID idConsulta);

    /** Quais destas consultas já têm atendimento, concluído ou rascunho. */
    Set<UUID> consultasComAtendimento(Collection<UUID> idsConsulta);
}
