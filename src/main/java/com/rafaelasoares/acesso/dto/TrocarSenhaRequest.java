package com.rafaelasoares.acesso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca da própria senha.
 *
 * <p>Exige a senha atual de propósito: sem isso, quem pegasse uma sessão
 * aberta (máquina destravada) trocaria a senha e tomaria a conta.
 */
public record TrocarSenhaRequest(
        @NotBlank(message = "Informe a senha atual.") String senhaAtual,
        @NotBlank(message = "Informe a nova senha.")
                @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
                String novaSenha) {

    /** Sem senhas no toString — este objeto pode acabar em log de erro. */
    @Override
    public String toString() {
        return "TrocarSenhaRequest{}";
    }
}
