package com.vetplanet.modules.agendamento.service;

import com.vetplanet.modules.agendamento.exception.BloqueioNaoEncontradoException;
import com.vetplanet.modules.agendamento.repository.BloqueioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apaga um bloqueio — de verdade, não por status.
 *
 * <p><b>É a segunda exceção à regra de "nenhuma exclusão física"</b>, e pelo
 * mesmo motivo da primeira (animal sem histórico): aquela regra protege
 * <i>histórico clínico</i>, e um bloqueio não tem nenhum. Ele é uma regra de
 * agenda, não um fato ocorrido — o almoço que ela deixou de fazer às terças
 * não é informação que alguém vá querer consultar em 2030.
 *
 * <p>Um status "inativo" aqui só encheria a tela de bloqueios revogados que
 * ela teria de aprender a ignorar.
 */
@Service
public class ExcluirBloqueioService {

    private final BloqueioRepository bloqueioRepository;

    public ExcluirBloqueioService(BloqueioRepository bloqueioRepository) {
        this.bloqueioRepository = bloqueioRepository;
    }

    @Transactional
    public void excluirBloqueio(UUID idBloqueio) {
        if (!bloqueioRepository.existsById(idBloqueio)) {
            throw new BloqueioNaoEncontradoException(idBloqueio);
        }
        bloqueioRepository.deleteById(idBloqueio);
    }
}
