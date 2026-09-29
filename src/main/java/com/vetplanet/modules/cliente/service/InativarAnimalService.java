package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.exception.AnimalNaoEncontradoException;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.service.HistoricoDoAnimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tira o animal da lista sem apagar nada.
 *
 * <p>É o substituto da exclusão para quem <b>já tem histórico</b>: consulta ou
 * prontuário travam o delete, e sem esta saída o cadastro errado ficaria
 * visível para sempre.
 *
 * <p>Não significa "parou de aparecer" — ver {@code SituacaoAnimal}.
 */
@Service
public class InativarAnimalService {

    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public InativarAnimalService(AnimalRepository animalRepository, HistoricoDoAnimal historicoDoAnimal) {
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    @Transactional
    public AnimalResponseDto inativarAnimal(UUID idAnimal) {
        AnimalEntity animal =
                animalRepository
                        .findById(idAnimal)
                        .orElseThrow(() -> new AnimalNaoEncontradoException(idAnimal));
        animal.inativar();
        return AnimalResponseDto.de(animal, !historicoDoAnimal.temHistorico(animal.getId()));
    }
}
