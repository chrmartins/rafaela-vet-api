package com.vetplanet.modules.cliente.dto;

import com.vetplanet.modules.cliente.entity.EspecieAnimal;
import com.vetplanet.modules.cliente.entity.SexoAnimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Edição dos dados de um animal.
 *
 * <p>Sem {@code situacao}: mudar de ativo para óbito não é "editar um campo",
 * é um fato registrado — tem endpoint próprio, para não acontecer por
 * descuido num formulário de dados cadastrais.
 *
 * <p>Sem {@code idTutor}: transferir animal de tutor é outra operação, com
 * consequências no histórico. Ainda não exposta.
 */
public record AtualizarAnimalRequestDto(
        @NotBlank(message = "Informe o nome do animal.")
                @Size(max = 80, message = "Nome do animal muito longo.")
                String nome,
        @NotNull(message = "Informe a espécie.") EspecieAnimal especie,
        @Size(max = 80) String raca,
        @NotNull(message = "Informe o sexo.") SexoAnimal sexo,
        @PastOrPresent(message = "Data de nascimento não pode ser no futuro.")
                LocalDate dataNascimento,
        Boolean castrado,
        @Size(max = 2000) String observacoes) {}
