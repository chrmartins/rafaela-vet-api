package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.TutorResponseDto;
import com.vetplanet.modules.cliente.entity.TutorEntity;
import com.vetplanet.modules.cliente.exception.TutorNaoEncontradoException;
import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.repository.TutorRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inativa um tutor — o mais próximo de "excluir" que o sistema tem.
 *
 * <p>Não há exclusão física: o histórico de consultas e prontuários precisa
 * continuar apontando para quem era o responsável.
 *
 * <p>Os animais <b>não</b> são inativados junto, de propósito. Um tutor pode
 * sair da clientela enquanto o animal continua sendo atendido por outra
 * pessoa da família — inativar em cascata apagaria bichos vivos da lista.
 */
@Service
public class InativarTutorService {

    private static final Logger log = LoggerFactory.getLogger(InativarTutorService.class);

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public InativarTutorService(
            TutorRepository tutorRepository, AnimalRepository animalRepository, HistoricoDoAnimal historicoDoAnimal) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    @Transactional
    public TutorResponseDto inativarTutor(UUID idTutor) {
        TutorEntity tutor =
                tutorRepository
                        .findById(idTutor)
                        .orElseThrow(() -> new TutorNaoEncontradoException(idTutor));

        tutor.inativar();
        log.info("Tutor {} inativado", idTutor);

        return TutorResponseDto.completo(tutor, montarAnimais(idTutor));
    }

    /** Cada animal precisa saber se ainda pode ser excluído. */
    private java.util.List<AnimalResponseDto> montarAnimais(java.util.UUID idTutor) {
        return animalRepository.findByTutorIdOrderByNomeAsc(idTutor).stream()
                .map(animal -> AnimalResponseDto.de(animal, !historicoDoAnimal.temHistorico(animal.getId())))
                .toList();
    }
}
