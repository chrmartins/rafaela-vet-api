package com.vetplanet.modules.cliente.dto;

import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.entity.EspecieAnimal;
import com.vetplanet.modules.cliente.entity.SexoAnimal;
import com.vetplanet.modules.cliente.entity.SituacaoAnimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um animal na lista de busca.
 *
 * <p>Fica entre os dois DTOs que já existiam, e por isso é um terceiro:
 *
 * <ul>
 *   <li>{@code ResumoAnimalDto} é o mínimo que atravessa a fronteira para
 *       `agendamento` — mantê-lo magro é a intenção, não um descuido.
 *   <li>{@code AnimalResponseDto} traz observações e {@code podeExcluir}, que
 *       custa uma consulta ao histórico por linha. Numa lista de busca isso
 *       seria N consultas para exibir uma lixeira que a lista nem mostra.
 * </ul>
 *
 * <p>Espécie, raça e situação entram porque a lista precisa desempatar: duas
 * cadelas chamadas Mel de tutoras diferentes, e a que faleceu não pode parecer
 * a que está em acompanhamento.
 *
 * <p>Os nomes dos campos são {@code nomeAnimal} e {@code nomeTutor}, e não
 * {@code nome}, para o seletor da Agenda continuar lendo esta lista sem
 * mudança — é a mesma rota.
 */
public record AnimalNaListaDto(
        UUID idAnimal,
        String nomeAnimal,
        EspecieAnimal especie,
        String raca,
        SexoAnimal sexo,
        LocalDate dataNascimento,
        SituacaoAnimal situacao,
        UUID idTutor,
        String nomeTutor,
        boolean tutorAtivo) {

    /** Exige o tutor já carregado — ver {@code AnimalRepository}. */
    public static AnimalNaListaDto de(AnimalEntity animal) {
        return new AnimalNaListaDto(
                animal.getId(),
                animal.getNome(),
                animal.getEspecie(),
                animal.getRaca(),
                animal.getSexo(),
                animal.getDataNascimento(),
                animal.getSituacao(),
                animal.getTutor().getId(),
                animal.getTutor().getNomeCompleto(),
                animal.getTutor().isAtivo());
    }
}
