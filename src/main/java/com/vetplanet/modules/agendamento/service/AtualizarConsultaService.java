package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.dto.AtualizarConsultaRequestDto;
import com.vetplanet.modules.agendamento.dto.ConsultaResponseDto;
import com.vetplanet.modules.agendamento.entity.ConsultaEntity;
import com.vetplanet.modules.agendamento.exception.ConsultaComRegistroClinicoException;
import com.vetplanet.modules.agendamento.exception.ConsultaNaoEncontradaException;
import com.vetplanet.modules.agendamento.repository.ConsultaRepository;
import com.vetplanet.modules.cliente.service.ResumirAnimaisService;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrige ou remarca uma consulta.
 *
 * <p><b>Existe para que remarcar não seja cancelar e criar de novo.</b> Aquele
 * caminho escreve um fato falso no histórico: {@code CANCELADA} quer dizer que
 * o atendimento não aconteceu, e um atendimento remarcado aconteceu — só que
 * mais tarde. De quebra, perdia as observações já digitadas.
 *
 * <p><b>Não trava por status — trava pelo prontuário.</b> Consulta concluída
 * sem registro clínico continua editável; consulta com atendimento concluído,
 * não. A trava vem do que está preso à consulta, não da passagem do tempo, que
 * é a mesma lógica de excluir animal: quem recusa é o histórico, não uma
 * checagem de estado.
 *
 * <p>Rascunho não trava nada: ela abriu a tela e desistiu, e isso não é fato
 * registrado.
 *
 * <p>Chama os dois métodos da entidade porque são coisas diferentes:
 * {@code reagendar} move o atendimento no tempo, {@code atualizarDados} conserta
 * o que está escrito sobre ele.
 */
@Service
public class AtualizarConsultaService {

    private final ConsultaRepository consultaRepository;
    private final ResumirAnimaisService resumirAnimaisService;
    private final RegistroClinicoDaConsulta registroClinico;

    public AtualizarConsultaService(
            ConsultaRepository consultaRepository,
            ResumirAnimaisService resumirAnimaisService,
            RegistroClinicoDaConsulta registroClinico) {
        this.consultaRepository = consultaRepository;
        this.resumirAnimaisService = resumirAnimaisService;
        this.registroClinico = registroClinico;
    }

    @Transactional
    public ConsultaResponseDto atualizarConsulta(
            UUID idConsulta, AtualizarConsultaRequestDto request) {
        ConsultaEntity consulta =
                consultaRepository
                        .findById(idConsulta)
                        .orElseThrow(() -> new ConsultaNaoEncontradaException(idConsulta));

        // A trava chegou com o prontuário, exatamente como estava previsto:
        // consulta registrada aconteceu, e mudar a hora dela seria reescrever
        // um fato. A correção passa a ser retificação do atendimento.
        if (registroClinico.temAtendimentoConcluido(idConsulta)) {
            throw new ConsultaComRegistroClinicoException();
        }

        consulta.reagendar(request.dataHora(), request.duracaoMinutos());
        consulta.atualizarDados(
                request.motivo(),
                Boolean.TRUE.equals(request.urgente()),
                request.enderecoAtendimento(),
                request.observacoes());

        // Chegou até aqui, então não havia atendimento concluído. Pode haver
        // rascunho — que não trava a edição, mas o cartão precisa saber.
        return ConsultaResponseDto.de(
                consulta,
                resumirAnimaisService
                        .resumirAnimais(Set.of(consulta.getIdAnimal()))
                        .get(consulta.getIdAnimal()),
                !registroClinico.consultasComAtendimento(Set.of(idConsulta)).isEmpty());
    }
}
