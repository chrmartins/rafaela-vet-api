package com.vetplanet.modules.cliente.dto;

import com.vetplanet.modules.cliente.entity.EspecieAnimal;
import com.vetplanet.modules.cliente.entity.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Dados de um animal — usado tanto dentro do cadastro do tutor quanto ao
 * adicionar mais um animal a um tutor existente.
 *
 * <p>Sem peso: é medição clínica e pertence ao prontuário.
 */
public record CriarAnimalRequestDto(
        @NotBlank(message = "Informe o nome do animal.")
                @Size(max = 80, message = "Nome do animal muito longo.")
                String nome,
        @NotNull(message = "Informe a espécie.") EspecieAnimal especie,
        @Size(max = 80) String raca,
        @NotNull(message = "Informe o sexo.") SexoAnimal sexo,
        @PastOrPresent(message = "Data de nascimento não pode ser no futuro.")
                LocalDate dataNascimento,
        /** {@code null} quando não se sabe — diferente de "não é castrado". */
        Boolean castrado,
        @Size(max = 2000) String observacoes) {}
