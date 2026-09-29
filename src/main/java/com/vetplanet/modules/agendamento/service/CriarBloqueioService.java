package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.dto.BloqueioResponseDto;
import com.vetplanet.modules.agendamento.dto.CriarBloqueioRequestDto;
import com.vetplanet.modules.agendamento.repository.BloqueioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cria um bloqueio. As regras da forma ficam em {@link FormaDoBloqueio}. */
@Service
public class CriarBloqueioService {

    private final BloqueioRepository bloqueioRepository;

    public CriarBloqueioService(BloqueioRepository bloqueioRepository) {
        this.bloqueioRepository = bloqueioRepository;
    }

    @Transactional
    public BloqueioResponseDto criarBloqueio(CriarBloqueioRequestDto request) {
        return BloqueioResponseDto.de(
                bloqueioRepository.save(FormaDoBloqueio.de(request).novaEntidade()));
    }
}
