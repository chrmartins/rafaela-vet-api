package com.vetplanet.modules.acesso.service;

import com.vetplanet.modules.acesso.dto.CriarUsuarioRequestDto;
import com.vetplanet.modules.acesso.dto.UsuarioResponseDto;
import com.vetplanet.modules.acesso.entity.PerfilAcesso;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.exception.CrmvObrigatorioException;
import com.vetplanet.modules.acesso.exception.EmailJaCadastradoException;
import com.vetplanet.modules.acesso.repository.UsuarioRepository;
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
    public UsuarioResponseDto criarUsuario(CriarUsuarioRequestDto request) {
        // Checagem antecipada para devolver 409 com mensagem clara. O índice
        // único do banco continua sendo a garantia real contra corrida entre
        // dois cadastros simultâneos.
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailJaCadastradoException(request.email());
        }

        UsuarioEntity usuario =
                UsuarioEntity.criar(
                        request.nomeCompleto(),
                        request.email(),
                        passwordEncoder.encode(request.senha()),
                        request.perfilAcesso(),
                        exigirCrmvDeVeterinario(request.perfilAcesso(), request.crmv()));

        return UsuarioResponseDto.de(usuarioRepository.save(usuario));
    }

    /**
     * Veterinário precisa de CRMV; os outros perfis não têm.
     *
     * <p>Vive aqui, e não numa anotação, porque depende da combinação de dois
     * campos. Vive num método com nome, e não solto no meio do fluxo, porque
     * {@code AtualizarUsuarioService} aplica exatamente a mesma regra — e duas
     * cópias divergiriam no primeiro ajuste.
     */
    static String exigirCrmvDeVeterinario(PerfilAcesso perfil, String crmv) {
        if (perfil != PerfilAcesso.VETERINARIO) return null;
        if (crmv == null || crmv.isBlank()) throw new CrmvObrigatorioException();
        return crmv.trim();
    }
}
