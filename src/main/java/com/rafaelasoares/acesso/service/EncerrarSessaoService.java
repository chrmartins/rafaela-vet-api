package com.rafaelasoares.acesso.service;

import com.rafaelasoares.acesso.entity.TokenAutenticacao;
import com.rafaelasoares.acesso.repository.TokenAutenticacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encerra a sessão — é o "sair" do painel.
 *
 * <p>Com token opaco, isto invalida de verdade: o próximo request com o mesmo
 * token é recusado. Era justamente o que JWT puro não daria.
 */
@Service
public class EncerrarSessaoService {

    private static final Logger log = LoggerFactory.getLogger(EncerrarSessaoService.class);

    private final TokenAutenticacaoRepository tokenRepository;
    private final TokenGenerator tokenGenerator;

    public EncerrarSessaoService(
            TokenAutenticacaoRepository tokenRepository, TokenGenerator tokenGenerator) {
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
    }

    /**
     * Silencioso quando o token não existe ou já foi revogado: sair duas vezes
     * não é erro, e responder diferente revelaria se um token é válido.
     */
    @Transactional
    public void encerrarSessao(String token) {
        tokenRepository
                .findByTokenHash(tokenGenerator.hash(token))
                .ifPresent(
                        sessao -> {
                            sessao.revogar();
                            log.info("Sessão encerrada para {}", sessao.getUsuario().getEmail());
                        });
    }

    /** Derruba todas as sessões abertas de um usuário. */
    @Transactional
    public void encerrarSessoesDoUsuario(com.rafaelasoares.acesso.entity.Usuario usuario) {
        for (TokenAutenticacao sessao :
                tokenRepository.findByUsuarioAndRevogadoEmIsNull(usuario)) {
            sessao.revogar();
        }
    }
}
