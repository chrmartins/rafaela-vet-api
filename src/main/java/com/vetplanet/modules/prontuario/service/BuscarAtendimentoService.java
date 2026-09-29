package com.vetplanet.modules.prontuario.service;

import com.vetplanet.modules.prontuario.dto.AtendimentoResponseDto;
import com.vetplanet.modules.prontuario.exception.AtendimentoNaoEncontradoException;
import com.vetplanet.modules.prontuario.repository.AtendimentoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura do prontuário: um atendimento, ou o histórico de um animal. */
@Service
public class BuscarAtendimentoService {

    private final AtendimentoRepository atendimentoRepository;

    public BuscarAtendimentoService(AtendimentoRepository atendimentoRepository) {
        this.atendimentoRepository = atendimentoRepository;
    }

    @Transactional(readOnly = true)
    public AtendimentoResponseDto buscarAtendimento(UUID idAtendimento) {
        return atendimentoRepository
                .findById(idAtendimento)
                .map(AtendimentoResponseDto::de)
                .orElseThrow(() -> new AtendimentoNaoEncontradoException(idAtendimento));
    }

    /**
     * O atendimento de uma consulta, se existir.
     *
     * <p>Devolve vazio em vez de 404 porque "esta consulta ainda não tem
     * registro" é resposta legítima, não erro — é o que a Agenda pergunta para
     * decidir entre "Iniciar" e "Ver".
     */
    @Transactional(readOnly = true)
    public Optional<AtendimentoResponseDto> buscarPorConsulta(UUID idConsulta) {
        return atendimentoRepository.findByIdConsulta(idConsulta).map(AtendimentoResponseDto::de);
    }

    /** O histórico clínico do animal, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<AtendimentoResponseDto> listarDoAnimal(UUID idAnimal) {
        return atendimentoRepository.findByIdAnimalOrderByCriadoEmDesc(idAnimal).stream()
                .map(AtendimentoResponseDto::de)
                .toList();
    }
}
