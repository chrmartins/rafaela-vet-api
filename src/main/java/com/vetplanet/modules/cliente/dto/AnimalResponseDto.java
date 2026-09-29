package com.vetplanet.modules.cliente.dto;

import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.entity.EspecieAnimal;
import com.vetplanet.modules.cliente.entity.SexoAnimal;
import com.vetplanet.modules.cliente.entity.SituacaoAnimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Animal como sai da API. */
public record AnimalResponseDto(
        UUID idAnimal,
        UUID idTutor,
        String nome,
        EspecieAnimal especie,
        String raca,
        SexoAnimal sexo,
        LocalDate dataNascimento,
        Boolean castrado,
        String observacoes,
        SituacaoAnimal situacao,
        /**
         * O animal ainda pode ser excluído de verdade?
         *
         * <p>Serve para a tela **não oferecer** a lixeira quando o delete
         * fosse falhar. Quem de fato impede é a chave estrangeira no banco
         * (ver {@code ExcluirAnimalService}); isto aqui é só a aparência.
         *
         * <p>Quem monta o DTO precisa informar — não há valor padrão, de
         * propósito: um default silencioso voltaria a mentir quando
         * `prontuario` existir e ninguém lembrar de perguntar a ele.
         */
        boolean podeExcluir,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    /**
     * Só use com o tutor carregado (ver {@code AnimalRepository#buscarComTutor})
     * ou dentro da transação — {@code getTutor().getId()} toca a relação LAZY.
     */
    public static AnimalResponseDto de(AnimalEntity animal, boolean podeExcluir) {
        return new AnimalResponseDto(
                animal.getId(),
                animal.getTutor().getId(),
                animal.getNome(),
                animal.getEspecie(),
                animal.getRaca(),
                animal.getSexo(),
                animal.getDataNascimento(),
                animal.getCastrado(),
                animal.getObservacoes(),
                animal.getSituacao(),
                podeExcluir,
                animal.getCriadoEm(),
                animal.getAtualizadoEm());
    }
}
