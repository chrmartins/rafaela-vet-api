package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.TutorResponseDto;
import com.vetplanet.modules.cliente.entity.TutorEntity;
import com.vetplanet.modules.cliente.exception.TutorNaoEncontradoException;
import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.repository.TutorRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A ficha do tutor, com os animais dele. */
@Service
public class BuscarTutorService {

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public BuscarTutorService(TutorRepository tutorRepository, AnimalRepository animalRepository, HistoricoDoAnimal historicoDoAnimal) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    @Transactional(readOnly = true)
    public TutorResponseDto buscarTutor(UUID idTutor) {
        TutorEntity tutor =
                tutorRepository
                        .findById(idTutor)
                        .orElseThrow(() -> new TutorNaoEncontradoException(idTutor));

        return TutorResponseDto.completo(tutor, montarAnimais(idTutor));
    }

    /** Cada animal precisa saber se ainda pode ser excluído. */
    private java.util.List<AnimalResponseDto> montarAnimais(java.util.UUID idTutor) {
        return animalRepository.findByTutorIdOrderByNomeAsc(idTutor).stream()
                .map(animal -> AnimalResponseDto.de(animal, !historicoDoAnimal.temHistorico(animal.getId())))
                .toList();
    }
}
