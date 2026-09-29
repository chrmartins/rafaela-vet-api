package com.vetplanet.modules.cliente.dto;

import com.vetplanet.modules.cliente.entity.TutorEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tutor como sai da API.
 *
 * <p>{@code animais} vem preenchido na busca por id e nulo na listagem: a
 * lista de tutores não deve disparar uma consulta de animais por linha. Quem
 * precisa dos bichos abre a ficha.
 */
public record TutorResponseDto(
        UUID idTutor,
        String nomeCompleto,
        String telefoneWhatsapp,
        String email,
        EnderecoDto endereco,
        String observacoes,
        boolean ativo,
        /** Quantos animais o tutor tem — barato o bastante para a listagem. */
        long quantidadeDeAnimais,
        List<AnimalResponseDto> animais,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    /** Para a listagem: sem os animais, só a contagem. */
    public static TutorResponseDto resumo(TutorEntity tutor, long quantidadeDeAnimais) {
        return montar(tutor, quantidadeDeAnimais, null);
    }

    /**
     * Para a ficha: com os animais já montados.
     *
     * <p>Recebe os DTOs prontos, e não as entidades, porque cada animal
     * carrega {@code podeExcluir} — que depende de perguntar a outro domínio
     * se há histórico. Um record estático não tem como fazer essa pergunta;
     * quem tem é o service.
     */
    public static TutorResponseDto completo(TutorEntity tutor, List<AnimalResponseDto> animais) {
        return montar(tutor, animais.size(), animais);
    }

    private static TutorResponseDto montar(
            TutorEntity tutor, long quantidade, List<AnimalResponseDto> animais) {
        return new TutorResponseDto(
                tutor.getId(),
                tutor.getNomeCompleto(),
                tutor.getTelefoneWhatsapp(),
                tutor.getEmail(),
                EnderecoDto.de(tutor.getEndereco()),
                tutor.getObservacoes(),
                tutor.isAtivo(),
                quantidade,
                animais,
                tutor.getCriadoEm(),
                tutor.getAtualizadoEm());
    }
}
