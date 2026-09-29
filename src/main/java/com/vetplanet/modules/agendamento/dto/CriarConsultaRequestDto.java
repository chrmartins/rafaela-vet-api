package com.vetplanet.modules.agendamento.dto;

import com.vetplanet.modules.agendamento.entity.StatusConsulta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Dados para marcar uma consulta.
 *
 * <p>Só o animal é informado — o tutor é resolvido a partir dele e gravado
 * junto, para o histórico não mudar de dono se o animal for transferido.
 *
 * <p>{@code motivo} é texto livre por enquanto: com catálogo de serviços, a
 * veterinária precisaria cadastrar serviços antes de marcar a primeira
 * consulta. Vira referência quando a repetição incomodar.
 *
 * <p>{@code status} é opcional e cai em {@code CONFIRMADA}: na maior parte
 * das vezes ela já acertou o horário no WhatsApp antes de lançar aqui.
 * Quando ainda vai confirmar, manda {@code SOLICITADA}.
 */
public record CriarConsultaRequestDto(
        @NotNull(message = "Informe o animal.") UUID idAnimal,
        @NotNull(message = "Informe a data e a hora.") OffsetDateTime dataHora,
        @NotNull(message = "Informe a duração.")
                @Min(value = 5, message = "Duração mínima de 5 minutos.")
                @Max(value = 600, message = "Duração máxima de 10 horas.")
                Integer duracaoMinutos,
        @NotBlank(message = "Informe o motivo do atendimento.")
                @Size(max = 200, message = "Motivo muito longo.")
                String motivo,
        StatusConsulta status,
        /**
         * {@code Boolean}, e não {@code boolean}: com o primitivo, omitir o
         * campo faz o Jackson recusar o corpo inteiro — o cliente recebia
         * "corpo inválido ou malformado" por não ter mandado um campo
         * opcional. Ausente vira {@code false} no service.
         */
        Boolean urgente,
        @Size(max = 400) String enderecoAtendimento,
        @Size(max = 2000) String observacoes) {}
