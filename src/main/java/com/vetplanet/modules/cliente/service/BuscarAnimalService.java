package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.exception.AnimalNaoEncontradoException;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.service.HistoricoDoAnimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Um animal, com o tutor carregado junto. */
@Service
public class BuscarAnimalService {

    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public BuscarAnimalService(AnimalRepository animalRepository, HistoricoDoAnimal historicoDoAnimal) {
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    @Transactional(readOnly = true)
    public AnimalResponseDto buscarAnimal(UUID idAnimal) {
        var animal =
                animalRepository
                        .buscarComTutor(idAnimal)
                        .orElseThrow(() -> new AnimalNaoEncontradoException(idAnimal));
        return AnimalResponseDto.de(animal, !historicoDoAnimal.temHistorico(idAnimal));
    }
}
