package com.vetplanet.modules.prontuario.service;

import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.service.BuscarUsuarioAtualService;
import com.vetplanet.modules.agendamento.dto.ConsultaResponseDto;
import com.vetplanet.modules.agendamento.service.BuscarConsultaService;
import com.vetplanet.modules.prontuario.dto.AtendimentoResponseDto;
import com.vetplanet.modules.prontuario.entity.AtendimentoEntity;
import com.vetplanet.modules.prontuario.exception.SemPermissaoParaAssinarException;
import com.vetplanet.modules.prontuario.repository.AtendimentoRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre o rascunho do atendimento de uma consulta.
 *
 * <p>É o que o botão "Iniciar atendimento" faz. <b>Só isso</b>: cria o registro
 * vazio para ela começar a escrever. Não marca hora de chegada nem põe a
 * consulta "em atendimento" — aqui não há recepção nem check-in, porque o
 * tutor não se desloca.
 *
 * <p><b>Idempotente.</b> Chamar duas vezes devolve o mesmo rascunho, em vez de
 * estourar com violação de unicidade: apertar o botão de novo é engano comum, e
 * o certo é abrir o que já existe.
 *
 * <p>Depende de `agendamento` chamando o application service dele, que é a
 * forma sancionada de atravessar módulos. A volta — `agendamento` perguntando
 * a `prontuario` — passa pela porta {@code RegistroClinicoDaConsulta}, para
 * não fechar um ciclo entre os dois.
 */
@Service
public class AbrirAtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final BuscarConsultaService buscarConsultaService;
    private final BuscarUsuarioAtualService buscarUsuarioAtualService;

    public AbrirAtendimentoService(
            AtendimentoRepository atendimentoRepository,
            BuscarConsultaService buscarConsultaService,
            BuscarUsuarioAtualService buscarUsuarioAtualService) {
        this.atendimentoRepository = atendimentoRepository;
        this.buscarConsultaService = buscarConsultaService;
        this.buscarUsuarioAtualService = buscarUsuarioAtualService;
    }

    @Transactional
    public AtendimentoResponseDto abrirAtendimento(UUID idConsulta, String emailDoUsuario) {
        return atendimentoRepository
                .findByIdConsulta(idConsulta)
                .map(AtendimentoResponseDto::de)
                .orElseGet(() -> criar(idConsulta, emailDoUsuario));
    }

    private AtendimentoResponseDto criar(UUID idConsulta, String emailDoUsuario) {
        UsuarioEntity veterinario = buscarUsuarioAtualService.buscarEntidade(emailDoUsuario);

        // Res. CFMV 1.321/2020, Art. 9º, II e VIII. Checado aqui, na abertura,
        // e não só no fechamento: descobrir que não pode assinar depois de
        // escrever a anamnese inteira seria cruel.
        if (!veterinario.podeAssinarProntuario()) {
            throw new SemPermissaoParaAssinarException();
        }

        ConsultaResponseDto consulta = buscarConsultaService.buscarConsulta(idConsulta);

        // O local é obrigatório no prontuário e opcional na consulta: marcar sem
        // saber o endereço é normal, atender sem saber onde não é. Quando a
        // consulta não trouxer, ela preenche na tela — o vazio aqui é só o
        // ponto de partida.
        String local =
                consulta.enderecoAtendimento() == null || consulta.enderecoAtendimento().isBlank()
                        ? "A informar"
                        : consulta.enderecoAtendimento();

        AtendimentoEntity atendimento =
                AtendimentoEntity.abrir(
                        idConsulta,
                        consulta.idAnimal(),
                        veterinario.getId(),
                        veterinario.getNomeCompleto(),
                        veterinario.getCrmv(),
                        local);

        return AtendimentoResponseDto.de(atendimentoRepository.save(atendimento));
    }
}
