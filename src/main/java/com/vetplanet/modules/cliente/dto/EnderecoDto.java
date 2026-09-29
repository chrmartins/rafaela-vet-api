package com.vetplanet.modules.cliente.dto;

import com.vetplanet.modules.cliente.entity.EnderecoTutor;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Endereço na borda HTTP — serve de entrada e de saída.
 *
 * <p>Todos os campos são opcionais. O cadastro costuma começar por telefone e
 * o endereço chega depois; exigi-lo aqui empurraria a pessoa a inventar dado
 * só para conseguir salvar.
 */
public record EnderecoDto(
        @Size(max = 9, message = "CEP inválido.") String cep,
        @Size(max = 180) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 120) String complemento,
        @Size(max = 120) String bairro,
        @Size(max = 120) String cidade,
        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "UF deve ter duas letras.") String uf) {

    public EnderecoTutor paraEntidade() {
        return new EnderecoTutor(cep, logradouro, numero, complemento, bairro, cidade, uf);
    }

    public static EnderecoDto de(EnderecoTutor endereco) {
        if (endereco == null) return null;
        return new EnderecoDto(
                endereco.cep(),
                endereco.logradouro(),
                endereco.numero(),
                endereco.complemento(),
                endereco.bairro(),
                endereco.cidade(),
                endereco.uf());
    }
}
