package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.exception.CredenciaisInvalidasException;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
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
    public Usuario buscarEntidade(String email) {
        return usuarioRepository
                .findByEmailIgnoreCase(email)
                // Sessão válida cujo usuário sumiu: trata como não autenticado.
                .orElseThrow(CredenciaisInvalidasException::new);
    }

    /** DTO — para devolver ao cliente. */
    @Transactional(readOnly = true)
    public UsuarioResponse buscarUsuarioAtual(String email) {
        return UsuarioResponse.de(buscarEntidade(email));
    }
}
