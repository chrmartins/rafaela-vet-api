package com.vetplanet.modules.acesso.controller;

import com.vetplanet.modules.acesso.dto.CriarSessaoRequestDto;
import com.vetplanet.modules.acesso.dto.SessaoResponseDto;
import com.vetplanet.modules.acesso.dto.UsuarioResponseDto;
import com.vetplanet.modules.acesso.service.BuscarUsuarioAtualService;
import com.vetplanet.modules.acesso.service.CriarSessaoService;
import com.vetplanet.modules.acesso.service.EncerrarSessaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Acesso — sessões", description = "Entrar no painel, saber quem está logado, sair.")
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
    @Operation(
            summary = "Autenticar e abrir sessão",
            description =
                    "Devolve um token opaco válido por 12h. O token aparece uma única vez, "
                            + "aqui — o banco guarda só o hash. Mensagem de erro é idêntica "
                            + "para e-mail inexistente, senha errada e usuário inativo, para "
                            + "não permitir descobrir quem tem conta.")
    // Anula a exigência global de token: é justamente o endpoint que fornece um.
    @SecurityRequirements
    @PostMapping
    public SessaoResponseDto entrar(@Valid @RequestBody CriarSessaoRequestDto request) {
        return criarSessaoService.criarSessao(request);
    }

    /** Quem está logado — o painel usa para montar o cabeçalho e o menu. */
    @Operation(
            summary = "Usuário da sessão atual",
            description = "O painel usa para montar o cabeçalho e decidir o que exibir no menu.")
    @GetMapping("/atual")
    public UsuarioResponseDto sessaoAtual(Principal principal) {
        return buscarUsuarioAtualService.buscarUsuarioAtual(principal.getName());
    }

    /** Sair. Revoga o token; o mesmo valor não autentica mais. */
    @Operation(
            summary = "Encerrar a sessão",
            description = "Revoga o token no banco — o mesmo valor deixa de autenticar na hora.")
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
