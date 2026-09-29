package com.vetplanet.modules.cliente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Cadastro de tutor — <b>com os animais dele, na mesma requisição</b>.
 *
 * <p>Não é conveniência: é o formato em que a informação chega. Quando alguém
 * liga pela primeira vez, ela recebe tudo de uma vez ("aqui é a Ana, moro na
 * Tijuca, é pro meu gato Mel"). Separar em duas chamadas inventaria um passo
 * que a realidade não tem, e deixaria a porta aberta para tutor órfão quando
 * a segunda chamada falhasse.
 *
 * <p>Sem CPF: faturamento é pós-MVP e documento sem finalidade é coleta
 * excessiva sob a LGPD. Entra quando houver cobrança.
 */
public record CriarTutorRequestDto(
        @NotBlank(message = "Informe o nome do tutor.")
                @Size(max = 120, message = "Nome muito longo.")
                String nomeCompleto,
        @NotBlank(message = "Informe o telefone (WhatsApp).")
                @Size(max = 20, message = "Telefone muito longo.")
                String telefoneWhatsapp,
        @Email(message = "E-mail inválido.") @Size(max = 180) String email,
        @Valid EnderecoDto endereco,
        @Size(max = 2000) String observacoes,
        @NotEmpty(message = "Informe ao menos um animal.")
                @Valid
                List<CriarAnimalRequestDto> animais) {}
