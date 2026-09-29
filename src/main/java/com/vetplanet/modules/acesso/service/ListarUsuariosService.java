package com.vetplanet.modules.acesso.service;

import com.vetplanet.modules.acesso.dto.UsuarioResponseDto;
import com.vetplanet.modules.acesso.repository.UsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lista os usuários do painel, em ordem alfabética. */
@Service
public class ListarUsuariosService {

    private final UsuarioRepository usuarioRepository;

    public ListarUsuariosService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Devolve ativos e inativos — quem administra precisa enxergar quem foi
     * desativado para poder reativar. Filtrar fica a cargo da tela.
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarUsuarios() {
        return usuarioRepository.findAllByOrderByNomeCompletoAsc().stream()
                .map(UsuarioResponseDto::de)
                .toList();
    }
}
