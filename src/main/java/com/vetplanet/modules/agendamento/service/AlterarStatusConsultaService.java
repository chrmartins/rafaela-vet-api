package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.dto.ConsultaResponseDto;
import com.vetplanet.modules.agendamento.entity.ConsultaEntity;
import com.vetplanet.modules.agendamento.entity.StatusConsulta;
import com.vetplanet.modules.agendamento.exception.ConsultaNaoEncontradaException;
import com.vetplanet.modules.agendamento.repository.ConsultaRepository;
import com.vetplanet.modules.cliente.service.ResumirAnimaisService;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma, cancela ou conclui uma consulta.
 *
 * <p><b>Cancelar é status, não exclusão.</b> A consulta cancelada continua no
 * histórico — é informação clínica e financeira, e some da agenda pela tela,
 * não do banco.
 */
@Service
public class AlterarStatusConsultaService {

    private final ConsultaRepository consultaRepository;
    private final ResumirAnimaisService resumirAnimaisService;
    private final RegistroClinicoDaConsulta registroClinico;

    public AlterarStatusConsultaService(
            ConsultaRepository consultaRepository,
            ResumirAnimaisService resumirAnimaisService,
            RegistroClinicoDaConsulta registroClinico) {
        this.consultaRepository = consultaRepository;
        this.resumirAnimaisService = resumirAnimaisService;
        this.registroClinico = registroClinico;
    }

    @Transactional
    public ConsultaResponseDto alterarStatus(UUID idConsulta, StatusConsulta novoStatus) {
        ConsultaEntity consulta =
                consultaRepository
                        .findById(idConsulta)
                        .orElseThrow(() -> new ConsultaNaoEncontradaException(idConsulta));

        consulta.alterarStatus(novoStatus);

        return ConsultaResponseDto.de(
                consulta,
                resumirAnimaisService.resumirAnimais(Set.of(consulta.getIdAnimal()))
                        .get(consulta.getIdAnimal()),
                !registroClinico.consultasComAtendimento(Set.of(idConsulta)).isEmpty());
    }
}
