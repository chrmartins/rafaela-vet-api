package com.vetplanet.modules.acesso.service;

import com.vetplanet.modules.acesso.dto.UsuarioResponseDto;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.exception.CredenciaisInvalidasException;
import com.vetplanet.modules.acesso.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve o usuário da sessão em curso.
 *
 * <p>Serve tanto ao "quem sou eu" do painel quanto às operações que precisam
 * saber quem está agindo (trocar a própria senha, por exemplo).
 */
@Service
public class BuscarUsuarioAtualService {

    private final UsuarioRepository usuarioRepository;

    public BuscarUsuarioAtualService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Entidade — para quem precisa agir sobre o usuário. */
    @Transactional(readOnly = true)
    public UsuarioEntity buscarEntidade(String email) {
        return usuarioRepository
                .findByEmailIgnoreCase(email)
                // Sessão válida cujo usuário sumiu: trata como não autenticado.
                .orElseThrow(CredenciaisInvalidasException::new);
    }

    /** DTO — para devolver ao cliente. */
    @Transactional(readOnly = true)
    public UsuarioResponseDto buscarUsuarioAtual(String email) {
        return UsuarioResponseDto.de(buscarEntidade(email));
    }
}
