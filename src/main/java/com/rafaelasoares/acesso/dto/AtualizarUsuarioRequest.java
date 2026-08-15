package com.rafaelasoares.acesso.dto;

import com.rafaelasoares.acesso.PerfilAcesso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados cadastrais editáveis de um usuário.
 *
 * <p>Senha não entra aqui de propósito: trocar senha é outra operação, com
 * outras regras (confirmação, exigir a senha atual), e misturar as duas
 * facilitaria alterar credencial sem querer.
 */
public record AtualizarUsuarioRequest(
        @NotBlank(message = "Informe o nome completo.")
                @Size(max = 120, message = "Nome completo muito longo.")
                String nomeCompleto,
        @NotBlank(message = "Informe o e-mail.")
                @Email(message = "E-mail inválido.")
                @Size(max = 180, message = "E-mail muito longo.")
                String email,
        @NotNull(message = "Informe o perfil de acesso.") PerfilAcesso perfilAcesso) {}
