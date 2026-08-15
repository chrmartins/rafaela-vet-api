package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.exception.UsuarioNaoEncontradoException;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inativa um usuário — o mais próximo de "excluir" que o sistema tem.
 *
 * <p>Não há exclusão física: o histórico precisa continuar apontando para
 * quem registrou cada consulta e cada prontuário.
 */
@Service
public class InativarUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public InativarUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public UsuarioResponse inativarUsuario(UUID idUsuario) {
        Usuario usuario =
                usuarioRepository
                        .findById(idUsuario)
                        .orElseThrow(() -> new UsuarioNaoEncontradoException(idUsuario));

        usuario.inativar();
        return UsuarioResponse.de(usuario);
    }
}
