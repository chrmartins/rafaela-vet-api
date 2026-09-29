package com.vetplanet.modules.agendamento.dto;

import com.vetplanet.modules.agendamento.entity.StatusConsulta;
import jakarta.validation.constraints.NotNull;

/**
 * Novo status da consulta.
 *
 * <p>Um endpoint com o status no corpo, e não quatro endpoints com verbo na
 * URL como em {@code /usuarios/{id}/inativar}: aqui os quatro estados são o
 * mesmo eixo, e a tela troca entre eles o tempo todo. Quatro rotas para uma
 * mudança de valor seria cerimônia sem ganho.
 */
public record AlterarStatusRequestDto(
        @NotNull(message = "Informe o novo status.") StatusConsulta status) {}
