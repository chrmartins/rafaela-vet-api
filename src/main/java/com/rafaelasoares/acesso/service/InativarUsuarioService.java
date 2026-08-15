package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.entity.PerfilAcesso;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.exception.UltimoAdministradorException;
import com.rafaelasoares.acesso.exception.UsuarioNaoEncontradoException;
import com.rafaelasoares.acesso.repository.UsuarioRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(InativarUsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final EncerrarSessaoService encerrarSessaoService;

    public InativarUsuarioService(
            UsuarioRepository usuarioRepository, EncerrarSessaoService encerrarSessaoService) {
        this.usuarioRepository = usuarioRepository;
        this.encerrarSessaoService = encerrarSessaoService;
    }

    @Transactional
    public UsuarioResponse inativarUsuario(UUID idUsuario) {
        Usuario usuario =
                usuarioRepository
                        .findById(idUsuario)
                        .orElseThrow(() -> new UsuarioNaoEncontradoException(idUsuario));

        garantirQueSobraAdministrador(usuario);

        usuario.inativar();

        // Inativar sem derrubar as sessões deixaria a pessoa trabalhando até o
        // token expirar — o acesso tem que cair na hora.
        encerrarSessaoService.encerrarSessoesDoUsuario(usuario);

        log.info("Usuário {} inativado; sessões encerradas", usuario.getEmail());
        return UsuarioResponse.de(usuario);
    }

    private void garantirQueSobraAdministrador(Usuario alvo) {
        boolean ehAdministradorAtivo =
                alvo.isAtivo() && alvo.getPerfilAcesso() == PerfilAcesso.ADMINISTRADOR;
        if (!ehAdministradorAtivo) {
            return;
        }
        if (usuarioRepository.countByPerfilAcessoAndAtivoTrue(PerfilAcesso.ADMINISTRADOR) <= 1) {
            throw new UltimoAdministradorException();
        }
    }
}
