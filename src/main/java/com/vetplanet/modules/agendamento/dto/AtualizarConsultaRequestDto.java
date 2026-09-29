package com.vetplanet.modules.agendamento.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Dados para corrigir ou remarcar uma consulta.
 *
 * <p><b>Não tem {@code idAnimal}, e isso é a regra, não um esquecimento.</b> A
 * consulta é a âncora do histórico clínico do animal — é ela que a chave
 * estrangeira do prontuário vai apontar, e é ela que impede apagar um bicho
 * que já foi atendido. Trocar o animal reescreveria dois históricos de uma vez:
 * tiraria um atendimento de quem o teve e daria a quem não teve.
 *
 * <p>Marcar no bicho errado acontece, e a saída certa é outra: <b>cancelar e
 * marcar de novo</b>. Ali o cancelamento é honesto — aquela consulta realmente
 * nunca deveria ter existido para aquele animal. É o oposto do remarcar, em
 * que o atendimento aconteceu, só que noutra hora, e por isso cancelar seria
 * escrever um fato falso.
 *
 * <p><b>Também não tem {@code status}</b>: confirmar, cancelar e concluir têm
 * endpoint próprio. São fatos que ocorrem, não campos de formulário.
 */
public record AtualizarConsultaRequestDto(
        @NotNull(message = "Informe a data e a hora.") OffsetDateTime dataHora,
        @NotNull(message = "Informe a duração.")
                @Min(value = 5, message = "Duração mínima de 5 minutos.")
                @Max(value = 600, message = "Duração máxima de 10 horas.")
                Integer duracaoMinutos,
        @NotBlank(message = "Informe o motivo do atendimento.")
                @Size(max = 200, message = "Motivo muito longo.")
                String motivo,
        /** {@code Boolean} e não {@code boolean}: omitir o campo com o primitivo
         * faz o Jackson recusar o corpo inteiro. Ausente vira {@code false}. */
        Boolean urgente,
        @Size(max = 400) String enderecoAtendimento,
        @Size(max = 2000) String observacoes) {}
