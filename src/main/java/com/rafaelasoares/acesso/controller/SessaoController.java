package com.rafaelasoares.acesso.controller;

import com.rafaelasoares.acesso.dto.CriarSessaoRequest;
import com.rafaelasoares.acesso.dto.SessaoResponse;
import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.service.BuscarUsuarioAtualService;
import com.rafaelasoares.acesso.service.CriarSessaoService;
import com.rafaelasoares.acesso.service.EncerrarSessaoService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sessão do painel: entrar, saber quem está logado, sair.
 *
 * <p>"Entrar" é criar uma sessão e "sair" é apagá-la, então o verbo continua
 * sendo o método HTTP — nada de {@code /login} ou {@code /logout} na URL.
 */
@RestController
@RequestMapping("/api/sessoes")
public class SessaoController {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final CriarSessaoService criarSessaoService;
    private final EncerrarSessaoService encerrarSessaoService;
    private final BuscarUsuarioAtualService buscarUsuarioAtualService;

    public SessaoController(
            CriarSessaoService criarSessaoService,
            EncerrarSessaoService encerrarSessaoService,
            BuscarUsuarioAtualService buscarUsuarioAtualService) {
        this.criarSessaoService = criarSessaoService;
        this.encerrarSessaoService = encerrarSessaoService;
        this.buscarUsuarioAtualService = buscarUsuarioAtualService;
    }

    /** Entrar. Único endpoint público da API. */
    @PostMapping
    public SessaoResponse entrar(@Valid @RequestBody CriarSessaoRequest request) {
        return criarSessaoService.criarSessao(request);
    }

    /** Quem está logado — o painel usa para montar o cabeçalho e o menu. */
    @GetMapping("/atual")
    public UsuarioResponse sessaoAtual(Principal principal) {
        return buscarUsuarioAtualService.buscarUsuarioAtual(principal.getName());
    }

    /** Sair. Revoga o token; o mesmo valor não autentica mais. */
    @DeleteMapping("/atual")
    public ResponseEntity<Void> sair(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String cabecalhoAutorizacao) {
        if (cabecalhoAutorizacao.startsWith(PREFIXO_BEARER)) {
            encerrarSessaoService.encerrarSessao(
                    cabecalhoAutorizacao.substring(PREFIXO_BEARER.length()).trim());
        }
        return ResponseEntity.noContent().build();
    }
}
