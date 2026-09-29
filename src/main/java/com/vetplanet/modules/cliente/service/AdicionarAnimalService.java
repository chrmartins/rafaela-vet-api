package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.dto.CriarAnimalRequestDto;
import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.entity.TutorEntity;
import com.vetplanet.modules.cliente.exception.TutorNaoEncontradoException;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.service.HistoricoDoAnimal;
import com.vetplanet.modules.cliente.repository.TutorRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adiciona mais um animal a um tutor que já existe — o segundo gato. */
@Service
public class AdicionarAnimalService {

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public AdicionarAnimalService(
            TutorRepository tutorRepository, AnimalRepository animalRepository, HistoricoDoAnimal historicoDoAnimal) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    @Transactional
    public AnimalResponseDto adicionarAnimal(UUID idTutor, CriarAnimalRequestDto request) {
        TutorEntity tutor =
                tutorRepository
                        .findById(idTutor)
                        .orElseThrow(() -> new TutorNaoEncontradoException(idTutor));

        AnimalEntity animal =
                AnimalEntity.criar(
                        tutor,
                        request.nome(),
                        request.especie(),
                        request.raca(),
                        request.sexo(),
                        request.dataNascimento(),
                        request.castrado(),
                        request.observacoes());

        animalRepository.save(animal);
        return AnimalResponseDto.de(animal, !historicoDoAnimal.temHistorico(animal.getId()));
    }
}
