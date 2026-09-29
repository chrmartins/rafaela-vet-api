package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.dto.ConsultaResponseDto;
import com.vetplanet.modules.agendamento.entity.ConsultaEntity;
import com.vetplanet.modules.agendamento.exception.ConsultaNaoEncontradaException;
import com.vetplanet.modules.agendamento.repository.ConsultaRepository;
import com.vetplanet.modules.cliente.service.ResumirAnimaisService;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uma consulta pelo id — é o que a tela de edição carrega ao abrir.
 *
 * <p>Classe própria, e não mais um método em {@code ListarConsultasService}:
 * buscar uma é outro caso de uso, e o nome daquela classe já mentiria.
 */
@Service
public class BuscarConsultaService {

    private final ConsultaRepository consultaRepository;
    private final ResumirAnimaisService resumirAnimaisService;
    private final RegistroClinicoDaConsulta registroClinico;

    public BuscarConsultaService(
            ConsultaRepository consultaRepository,
            ResumirAnimaisService resumirAnimaisService,
            RegistroClinicoDaConsulta registroClinico) {
        this.consultaRepository = consultaRepository;
        this.resumirAnimaisService = resumirAnimaisService;
        this.registroClinico = registroClinico;
    }

    @Transactional(readOnly = true)
    public ConsultaResponseDto buscarConsulta(UUID idConsulta) {
        ConsultaEntity consulta =
                consultaRepository
                        .findById(idConsulta)
                        .orElseThrow(() -> new ConsultaNaoEncontradaException(idConsulta));

        return ConsultaResponseDto.de(
                consulta,
                resumirAnimaisService
                        .resumirAnimais(Set.of(consulta.getIdAnimal()))
                        .get(consulta.getIdAnimal()),
                !registroClinico.consultasComAtendimento(Set.of(idConsulta)).isEmpty());
    }
}
