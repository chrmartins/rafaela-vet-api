package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.exception.UsuarioNaoEncontradoException;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Busca um usuário pelo identificador. */
@Service
public class BuscarUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public BuscarUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarUsuario(UUID idUsuario) {
        return usuarioRepository
                .findById(idUsuario)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(idUsuario));
    }
}
