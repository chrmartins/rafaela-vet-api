package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.CriarUsuarioRequest;
import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.exception.EmailJaCadastradoException;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastra um usuário do painel. */
@Service
public class CriarUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CriarUsuarioService(
            UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse criarUsuario(CriarUsuarioRequest request) {
        // Checagem antecipada para devolver 409 com mensagem clara. O índice
        // único do banco continua sendo a garantia real contra corrida entre
        // dois cadastros simultâneos.
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailJaCadastradoException(request.email());
        }

        Usuario usuario =
                Usuario.criar(
                        request.nomeCompleto(),
                        request.email(),
                        passwordEncoder.encode(request.senha()),
                        request.perfilAcesso());

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }
}
