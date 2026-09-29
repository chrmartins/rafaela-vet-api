package com.vetplanet.modules.agendamento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Dados para bloquear um tempo na agenda. Serve para criar e para atualizar —
 * atualizar substitui o conteúdo inteiro, então o corpo é o mesmo.
 *
 * <p><b>Um DTO para as duas formas</b>, e a validação cruzada fica no service
 * ({@code FormaDoBloqueio}). Dois DTOs obrigariam a tela a escolher a rota
 * antes de a pessoa terminar de preencher, e o que ela preenche é um
 * formulário só com uma pergunta no meio: "repete toda semana?".
 *
 * <p>Preencha {@code diasDaSemana} <b>ou</b> o par {@code dataInicio}/{@code
 * dataFim} — nunca os dois. {@code horaInicio} e {@code horaFim} vazios
 * bloqueiam o dia inteiro.
 *
 * <p>{@code diasDaSemana} é lista porque um almoço vale de segunda a sexta: é
 * uma decisão só de quem cadastra, e virar cinco bloqueios faria apagar cinco
 * para desfazer.
 *
 * <p>As horas são <b>civis</b>, do fuso da clínica, e não instantes UTC. A
 * razão está na migração {@code V005}: bloqueio semanal não é um ponto do
 * tempo, é uma hora do relógio da parede.
 */
public record CriarBloqueioRequestDto(
        @NotBlank(message = "Informe o motivo do bloqueio.")
                @Size(max = 200, message = "Motivo muito longo.")
                String motivo,
        @Size(max = 7, message = "Dias da semana repetidos.") List<Short> diasDaSemana,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horaInicio,
        LocalTime horaFim) {}
