package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.repository.ConsultaRepository;
import com.vetplanet.modules.cliente.service.HistoricoDoAnimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responde a `cliente` se um animal já tem consulta.
 *
 * <p>É a implementação da porta {@code HistoricoDoAnimal}, declarada em
 * `cliente`. A dependência aponta para lá, nunca o contrário — sem isso os
 * dois módulos se importariam mutuamente.
 */
@Service
public class HistoricoDeConsultas implements HistoricoDoAnimal {

    private final ConsultaRepository consultaRepository;

    public HistoricoDeConsultas(ConsultaRepository consultaRepository) {
        this.consultaRepository = consultaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean temHistorico(UUID idAnimal) {
        return consultaRepository.existsByIdAnimal(idAnimal);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Instant> ultimoAtendimentoPorAnimal(Collection<UUID> idsDeAnimais) {
        // `in ()` vazio não é SQL válido em todo banco; a guarda evita depender
        // de o Hibernate reescrever isso por nós.
        if (idsDeAnimais == null || idsDeAnimais.isEmpty()) return Map.of();

        List<Object[]> linhas = consultaRepository.ultimoAtendimentoPorAnimal(idsDeAnimais);

        Map<UUID, Instant> porAnimal = new HashMap<>(linhas.size());
        for (Object[] linha : linhas) {
            porAnimal.put((UUID) linha[0], (Instant) linha[1]);
        }
        return porAnimal;
    }
}