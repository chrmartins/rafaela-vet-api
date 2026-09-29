package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.exception.AnimalNaoEncontradoException;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.service.HistoricoDoAnimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra o óbito de um animal.
 *
 * <p>Caso de uso próprio, e não um campo de status a ser editado: é um fato
 * clínico, não um ajuste cadastral. Como campo de formulário, aconteceria por
 * descuido num envio distraído.
 *
 * <p>O registro <b>não apaga nada</b> — consultas e prontuários continuam
 * inteiros, que é o que a veterinária vai querer consultar depois. É essa a
 * diferença para {@link ExcluirAnimalService}, e é por isso que a mensagem de
 * exclusão barrada aponta para cá.
 */
@Service
public class RegistrarObitoService {

    private static final Logger log = LoggerFactory.getLogger(RegistrarObitoService.class);

    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public RegistrarObitoService(AnimalRepository animalRepository, HistoricoDoAnimal historicoDoAnimal) {
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    @Transactional
    public AnimalResponseDto registrarObito(UUID idAnimal) {
        AnimalEntity animal =
                animalRepository
                        .findById(idAnimal)
                        .orElseThrow(() -> new AnimalNaoEncontradoException(idAnimal));

        animal.registrarObito();
        log.info("Óbito registrado para o animal {}", idAnimal);
        return AnimalResponseDto.de(animal, !historicoDoAnimal.temHistorico(animal.getId()));
    }
}
