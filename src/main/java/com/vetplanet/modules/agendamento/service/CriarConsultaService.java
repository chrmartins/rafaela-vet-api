package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.dto.ConsultaResponseDto;
import com.vetplanet.modules.agendamento.dto.CriarConsultaRequestDto;
import com.vetplanet.modules.agendamento.entity.ConsultaEntity;
import com.vetplanet.modules.agendamento.entity.StatusConsulta;
import com.vetplanet.modules.agendamento.repository.ConsultaRepository;
import com.vetplanet.modules.cliente.dto.ResumoAnimalDto;
import com.vetplanet.modules.cliente.exception.AnimalNaoEncontradoException;
import com.vetplanet.modules.cliente.service.ResumirAnimaisService;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marca uma consulta.
 *
 * <p>O tutor não vem na requisição: é resolvido a partir do animal e gravado
 * junto. Assim o histórico continua dizendo quem era o responsável na época,
 * mesmo que o animal mude de dono depois.
 *
 * <p>Chama `cliente` por application service — nunca pelo repositório dele.
 */
@Service
public class CriarConsultaService {

    private static final Logger log = LoggerFactory.getLogger(CriarConsultaService.class);

    private final ConsultaRepository consultaRepository;
    private final ResumirAnimaisService resumirAnimaisService;

    public CriarConsultaService(
            ConsultaRepository consultaRepository, ResumirAnimaisService resumirAnimaisService) {
        this.consultaRepository = consultaRepository;
        this.resumirAnimaisService = resumirAnimaisService;
    }

    @Transactional
    public ConsultaResponseDto criarConsulta(CriarConsultaRequestDto request) {
        ResumoAnimalDto animal =
                resumirAnimaisService.resumirAnimais(Set.of(request.idAnimal())).values().stream()
                        .findFirst()
                        .orElseThrow(() -> new AnimalNaoEncontradoException(request.idAnimal()));

        ConsultaEntity consulta =
                ConsultaEntity.criar(
                        animal.idTutor(),
                        animal.idAnimal(),
                        request.dataHora(),
                        request.duracaoMinutos(),
                        request.motivo(),
                        // Padrão confirmada: na maior parte das vezes o horário
                        // já foi acertado no WhatsApp antes de chegar aqui.
                        request.status() == null ? StatusConsulta.CONFIRMADA : request.status(),
                        Boolean.TRUE.equals(request.urgente()),
                        request.enderecoAtendimento(),
                        request.observacoes());

        consultaRepository.save(consulta);
        log.info("Consulta {} marcada para {}", consulta.getId(), consulta.getDataHora());

        // Consulta recém-criada nunca tem registro: o atendimento nasce depois.
        return ConsultaResponseDto.de(consulta, animal, false);
    }
}
