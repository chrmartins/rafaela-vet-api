package com.vetplanet.modules.prontuario.service;

import com.vetplanet.modules.agendamento.service.RegistroClinicoDaConsulta;
import com.vetplanet.modules.prontuario.repository.AtendimentoRepository;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responde a `agendamento` o que ele precisa saber sobre o prontuário.
 *
 * <p>Implementação da porta {@code RegistroClinicoDaConsulta}, declarada lá. A
 * dependência aponta de `prontuario` para `agendamento`, nunca o contrário.
 */
@Service
public class AtendimentosDasConsultas implements RegistroClinicoDaConsulta {

    private final AtendimentoRepository atendimentoRepository;

    public AtendimentosDasConsultas(AtendimentoRepository atendimentoRepository) {
        this.atendimentoRepository = atendimentoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean temAtendimentoConcluido(UUID idConsulta) {
        return atendimentoRepository.existsByIdConsultaAndConcluidoEmNotNull(idConsulta);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> consultasComAtendimento(Collection<UUID> idsConsulta) {
        if (idsConsulta.isEmpty()) return Set.of();
        return Set.copyOf(atendimentoRepository.idsDeConsultaComAtendimento(idsConsulta));
    }
}
