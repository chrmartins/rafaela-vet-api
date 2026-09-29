package com.vetplanet.modules.acesso.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciais de entrada no painel. */
public record CriarSessaoRequestDto(
        @NotBlank(message = "Informe o e-mail.") String email,
        @NotBlank(message = "Informe a senha.") String senha) {

    /** Sem a senha no toString — este objeto pode acabar em log de erro. */
    @Override
    public String toString() {
        return "CriarSessaoRequestDto{email=%s}".formatted(email);
    }
}
