package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.dto.TrocarSenhaRequest;
import com.rafaelasoares.acesso.entity.Usuario;
import com.rafaelasoares.acesso.exception.CredenciaisInvalidasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Troca a senha do próprio usuário logado.
 *
 * <p>Ao trocar, <b>todas as outras sessões são derrubadas</b>. É o
 * comportamento esperado de quem troca a senha por desconfiar de acesso
 * indevido: de nada adiantaria a senha nova se a sessão do invasor
 * continuasse aberta.
 */
@Service
public class TrocarSenhaService {

    private static final Logger log = LoggerFactory.getLogger(TrocarSenhaService.class);

    private final BuscarUsuarioAtualService buscarUsuarioAtualService;
    private final EncerrarSessaoService encerrarSessaoService;
    private final PasswordEncoder passwordEncoder;

    public TrocarSenhaService(
            BuscarUsuarioAtualService buscarUsuarioAtualService,
            EncerrarSessaoService encerrarSessaoService,
            PasswordEncoder passwordEncoder) {
        this.buscarUsuarioAtualService = buscarUsuarioAtualService;
        this.encerrarSessaoService = encerrarSessaoService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void trocarSenha(String emailDoUsuarioLogado, TrocarSenhaRequest request) {
        Usuario usuario = buscarUsuarioAtualService.buscarEntidade(emailDoUsuarioLogado);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            log.info("Troca de senha rejeitada para {}: senha atual incorreta", usuario.getEmail());
            throw new CredenciaisInvalidasException();
        }

        usuario.trocarSenha(passwordEncoder.encode(request.novaSenha()));

        // Derruba as sessões abertas, inclusive a atual: quem trocou a senha
        // entra de novo com ela.
        encerrarSessaoService.encerrarSessoesDoUsuario(usuario);
        log.info("Senha trocada para {}; sessões encerradas", usuario.getEmail());
    }
}
