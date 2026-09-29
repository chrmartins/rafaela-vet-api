package com.vetplanet.modules.prontuario.service;

import com.vetplanet.modules.agendamento.entity.StatusConsulta;
import com.vetplanet.modules.agendamento.service.AlterarStatusConsultaService;
import com.vetplanet.modules.prontuario.dto.AtendimentoResponseDto;
import com.vetplanet.modules.prontuario.entity.AtendimentoEntity;
import com.vetplanet.modules.prontuario.exception.AtendimentoIncompletoException;
import com.vetplanet.modules.prontuario.exception.AtendimentoNaoEncontradoException;
import com.vetplanet.modules.prontuario.repository.AtendimentoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fecha o atendimento — e, com ele, a consulta.
 *
 * <p><b>Concluir o prontuário é o que conclui a consulta.</b> Não existe botão
 * "Concluir" separado na Agenda, e é decisão de produto: na vida real você não
 * termina o atendimento e depois escreve — escrever é terminar. Dois botões
 * dizendo "acabei" permitiriam marcar uma consulta como realizada sem o
 * documento que a lei exige que exista.
 *
 * <p><b>Não é mais ponto sem volta.</b> A versão anterior travava o registro
 * ao concluir; a regra caiu porque impedia acrescentar o que ela lembrasse
 * depois — e prontuário que não cresce é prontuário pior. Concluir virou
 * <b>marco</b>: diz quando ela deu o atendimento por terminado e encerra a
 * consulta. O conteúdo segue editável, e cada mudança vai para o rastro
 * ({@code RegistrarAtendimentoService}).
 */
@Service
public class ConcluirAtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final AlterarStatusConsultaService alterarStatusConsultaService;

    public ConcluirAtendimentoService(
            AtendimentoRepository atendimentoRepository,
            AlterarStatusConsultaService alterarStatusConsultaService) {
        this.atendimentoRepository = atendimentoRepository;
        this.alterarStatusConsultaService = alterarStatusConsultaService;
    }

    @Transactional
    public AtendimentoResponseDto concluir(UUID idAtendimento) {
        AtendimentoEntity atendimento =
                atendimentoRepository
                        .findById(idAtendimento)
                        .orElseThrow(() -> new AtendimentoNaoEncontradoException(idAtendimento));

        // Concluir de novo não faz nada: já está concluído, e a consulta já
        // foi encerrada. Devolver o estado atual é mais gentil que um erro —
        // é o mesmo raciocínio que torna a abertura idempotente.
        if (atendimento.estaConcluido()) return AtendimentoResponseDto.de(atendimento);

        // O banco tem o mesmo check (V010). A checagem aqui existe para a
        // mensagem dizer o que falta, em vez de estourar violação de constraint.
        if (!atendimento.podeSerConcluido()) throw new AtendimentoIncompletoException();

        atendimento.concluir();

        // A consulta vira CONCLUIDA por consequência, não por escolha. É a
        // única porta: o `PATCH /status` do controller recusa CONCLUIDA
        // justamente para não existir caminho que conclua sem prontuário.
        alterarStatusConsultaService.alterarStatus(
                atendimento.getIdConsulta(), StatusConsulta.CONCLUIDA);

        return AtendimentoResponseDto.de(atendimento);
    }
}
