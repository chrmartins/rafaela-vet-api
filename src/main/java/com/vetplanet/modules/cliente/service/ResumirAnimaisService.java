package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.ResumoAnimalDto;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve nomes de animais e tutores em lote, para outro domínio.
 *
 * <p><b>É a porta de `cliente` para o resto do sistema.</b> `agendamento`
 * guarda só os ids e chama aqui para montar a Agenda — nunca faz join com
 * `cliente.animal`.
 *
 * <p>Em lote, e não um por vez, porque a Agenda de um mês pede dezenas de
 * consultas de uma vez: resolver individualmente seria N+1 na cara.
 */
@Service
public class ResumirAnimaisService {

    private final AnimalRepository animalRepository;

    public ResumirAnimaisService(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    /** Ids que não existirem simplesmente não aparecem no mapa. */
    @Transactional(readOnly = true)
    public Map<UUID, ResumoAnimalDto> resumirAnimais(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();

        return animalRepository.buscarComTutorPorIds(ids).stream()
                .map(
                        animal ->
                                new ResumoAnimalDto(
                                        animal.getId(),
                                        animal.getNome(),
                                        animal.getTutor().getId(),
                                        animal.getTutor().getNomeCompleto()))
                .collect(
                        java.util.stream.Collectors.toMap(
                                ResumoAnimalDto::idAnimal, Function.identity()));
    }
}
