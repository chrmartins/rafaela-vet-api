package com.vetplanet.modules.acesso.service;

import com.vetplanet.modules.acesso.dto.AtualizarUsuarioRequestDto;
import com.vetplanet.modules.acesso.dto.UsuarioResponseDto;
import com.vetplanet.modules.acesso.entity.PerfilAcesso;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.exception.EmailJaCadastradoException;
import com.vetplanet.modules.acesso.exception.UltimoAdministradorException;
import com.vetplanet.modules.acesso.exception.UsuarioNaoEncontradoException;
import com.vetplanet.modules.acesso.repository.UsuarioRepository;
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
    public UsuarioResponseDto atualizarUsuario(UUID idUsuario, AtualizarUsuarioRequestDto request) {
        UsuarioEntity usuario =
                usuarioRepository
                        .findById(idUsuario)
                        .orElseThrow(() -> new UsuarioNaoEncontradoException(idUsuario));

        // Ignora o próprio usuário: manter o mesmo e-mail não é conflito.
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), idUsuario)) {
            throw new EmailJaCadastradoException(request.email());
        }

        garantirQueSobraAdministrador(usuario, request.perfilAcesso());

        usuario.atualizarDados(
                request.nomeCompleto(),
                request.email(),
                request.perfilAcesso(),
                CriarUsuarioService.exigirCrmvDeVeterinario(
                        request.perfilAcesso(), request.crmv()));

        // Sem save() explícito: a entidade está gerenciada dentro da transação,
        // então o Hibernate persiste a mudança no commit.
        return UsuarioResponseDto.de(usuario);
    }

    /**
     * Rebaixar o último administrador tranca todo mundo do lado de fora tanto
     * quanto inativá-lo, então a trava vale aqui também.
     */
    private void garantirQueSobraAdministrador(UsuarioEntity usuario, PerfilAcesso novoPerfil) {
        boolean deixaDeSerAdministrador =
                usuario.isAtivo()
                        && usuario.getPerfilAcesso() == PerfilAcesso.ADMINISTRADOR
                        && novoPerfil != PerfilAcesso.ADMINISTRADOR;

        if (deixaDeSerAdministrador
                && usuarioRepository.countByPerfilAcessoAndAtivoTrue(PerfilAcesso.ADMINISTRADOR)
                        <= 1) {
            throw new UltimoAdministradorException();
        }
    }
}
