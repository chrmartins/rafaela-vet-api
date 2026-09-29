package com.vetplanet.modules.acesso.dto;

import com.vetplanet.modules.acesso.entity.PerfilAcesso;
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
public record AtualizarUsuarioRequestDto(
        @NotBlank(message = "Informe o nome completo.")
                @Size(max = 120, message = "Nome completo muito longo.")
                String nomeCompleto,
        @NotBlank(message = "Informe o e-mail.")
                @Email(message = "E-mail inválido.")
                @Size(max = 180, message = "E-mail muito longo.")
                String email,
        @NotNull(message = "Informe o perfil de acesso.") PerfilAcesso perfilAcesso,
        /**
         * Número de inscrição no CRMV.
         *
         * <p>Obrigatório quando o perfil é {@code VETERINARIO} e ignorado nos
         * demais — a checagem cruzada fica no service, porque depende da
         * combinação dos dois campos e nenhuma anotação de campo a pega.
         *
         * <p>Exigência da Resolução CFMV nº 1.321/2020, Art. 9º, II e VIII:
         * o prontuário identifica o profissional por nome e número de CRMV.
         */
        @Size(max = 30, message = "CRMV muito longo.") String crmv) {}
