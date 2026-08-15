package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.AtualizarUsuarioRequest;
import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.exception.EmailJaCadastradoException;
import com.rafaelasoares.acesso.exception.UsuarioNaoEncontradoException;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atualiza os dados cadastrais de um usuário (não mexe em senha). */
@Service
public class AtualizarUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public AtualizarUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public UsuarioResponse atualizarUsuario(UUID idUsuario, AtualizarUsuarioRequest request) {
        Usuario usuario =
                usuarioRepository
                        .findById(idUsuario)
                        .orElseThrow(() -> new UsuarioNaoEncontradoException(idUsuario));

        // Ignora o próprio usuário: manter o mesmo e-mail não é conflito.
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), idUsuario)) {
            throw new EmailJaCadastradoException(request.email());
        }

        usuario.atualizarDados(
                request.nomeCompleto(), request.email(), request.perfilAcesso());

        // Sem save() explícito: a entidade está gerenciada dentro da transação,
        // então o Hibernate persiste a mudança no commit.
        return UsuarioResponse.de(usuario);
    }
}
