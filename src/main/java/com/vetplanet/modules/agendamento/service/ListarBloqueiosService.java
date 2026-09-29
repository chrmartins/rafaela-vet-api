package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.dto.BloqueioResponseDto;
import com.vetplanet.modules.agendamento.repository.BloqueioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Os bloqueios cadastrados — a Agenda cruza com o dia que está desenhando. */
@Service
public class ListarBloqueiosService {

    private final BloqueioRepository bloqueioRepository;

    public ListarBloqueiosService(BloqueioRepository bloqueioRepository) {
        this.bloqueioRepository = bloqueioRepository;
    }

    @Transactional(readOnly = true)
    public List<BloqueioResponseDto> listarBloqueios() {
        return bloqueioRepository.listarOrdenados().stream().map(BloqueioResponseDto::de).toList();
    }
}
