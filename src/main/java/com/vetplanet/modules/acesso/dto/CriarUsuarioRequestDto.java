package com.vetplanet.modules.acesso.dto;

import com.vetplanet.modules.acesso.entity.PerfilAcesso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para cadastrar um usuário do painel.
 *
 * <p>Não há auto-cadastro: quem cria usuário é o administrador.
 */
public record CriarUsuarioRequestDto(
        @NotBlank(message = "Informe o nome completo.")
                @Size(max = 120, message = "Nome completo muito longo.")
                String nomeCompleto,
        @NotBlank(message = "Informe o e-mail.")
                @Email(message = "E-mail inválido.")
                @Size(max = 180, message = "E-mail muito longo.")
                String email,
        // Mínimo de 8 conforme recomendação do NIST; o limite superior existe
        // porque o BCrypt ignora o que passa de 72 bytes — sem ele, senhas
        // longas dariam falsa sensação de segurança.
        @NotBlank(message = "Informe a senha.")
                @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
                String senha,
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
