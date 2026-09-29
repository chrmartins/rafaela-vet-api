package com.vetplanet.modules.cliente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edição dos dados do tutor.
 *
 * <p>Sem a lista de animais: mexer nos animais tem caminho próprio
 * (adicionar, editar, inativar cada um). Aceitar a lista inteira aqui faria
 * um PUT descuidado apagar animais por omissão.
 */
public record AtualizarTutorRequestDto(
        @NotBlank(message = "Informe o nome do tutor.")
                @Size(max = 120, message = "Nome muito longo.")
                String nomeCompleto,
        @NotBlank(message = "Informe o telefone (WhatsApp).")
                @Size(max = 20, message = "Telefone muito longo.")
                String telefoneWhatsapp,
        @Email(message = "E-mail inválido.") @Size(max = 180) String email,
        @Valid EnderecoDto endereco,
        @Size(max = 2000) String observacoes) {}
