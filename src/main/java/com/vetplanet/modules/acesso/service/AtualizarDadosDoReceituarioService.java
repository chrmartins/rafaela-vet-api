package com.vetplanet.modules.acesso.service;

import com.vetplanet.modules.acesso.dto.AtualizarDadosDoReceituarioRequestDto;
import com.vetplanet.modules.acesso.dto.UsuarioResponseDto;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import com.vetplanet.modules.acesso.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O profissional edita os próprios dados de contato.
 *
 * <p><b>O alvo vem da sessão, nunca de parâmetro.</b> Receber o id abriria uma
 * rota para editar o cadastro de outro assinante — e como não há checagem de
 * perfil aqui (qualquer um edita o que é seu), o id seria a única barreira.
 */
@Service
public class AtualizarDadosDoReceituarioService {

    private final UsuarioRepository usuarioRepository;
    private final BuscarUsuarioAtualService buscarUsuarioAtualService;

    public AtualizarDadosDoReceituarioService(
            UsuarioRepository usuarioRepository,
            BuscarUsuarioAtualService buscarUsuarioAtualService) {
        this.usuarioRepository = usuarioRepository;
        this.buscarUsuarioAtualService = buscarUsuarioAtualService;
    }

    @Transactional
    public UsuarioResponseDto atualizar(
            String email, AtualizarDadosDoReceituarioRequestDto request) {
        UsuarioEntity usuario = buscarUsuarioAtualService.buscarEntidade(email);

        usuario.atualizarDadosDoReceituario(
                request.telefoneContato(),
                request.instagram(),
                request.site(),
                request.cidadeAtuacao());

        return UsuarioResponseDto.de(usuarioRepository.save(usuario));
    }
}
