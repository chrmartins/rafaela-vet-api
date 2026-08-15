package com.rafaelasoares.acesso.controller;

import com.rafaelasoares.acesso.dto.AtualizarUsuarioRequest;
import com.rafaelasoares.acesso.dto.CriarUsuarioRequest;
import com.rafaelasoares.acesso.dto.UsuarioResponse;
import com.rafaelasoares.acesso.service.AtualizarUsuarioService;
import com.rafaelasoares.acesso.service.BuscarUsuarioService;
import com.rafaelasoares.acesso.service.CriarUsuarioService;
import com.rafaelasoares.acesso.service.InativarUsuarioService;
import com.rafaelasoares.acesso.service.ListarUsuariosService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Usuários do painel administrativo.
 *
 * <p>Recurso no plural e sem verbo na URL — o verbo é o método HTTP. Não
 * existe endpoint de auto-cadastro: quem cria usuário é o administrador.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final CriarUsuarioService criarUsuarioService;
    private final ListarUsuariosService listarUsuariosService;
    private final BuscarUsuarioService buscarUsuarioService;
    private final AtualizarUsuarioService atualizarUsuarioService;
    private final InativarUsuarioService inativarUsuarioService;

    public UsuarioController(
            CriarUsuarioService criarUsuarioService,
            ListarUsuariosService listarUsuariosService,
            BuscarUsuarioService buscarUsuarioService,
            AtualizarUsuarioService atualizarUsuarioService,
            InativarUsuarioService inativarUsuarioService) {
        this.criarUsuarioService = criarUsuarioService;
        this.listarUsuariosService = listarUsuariosService;
        this.buscarUsuarioService = buscarUsuarioService;
        this.atualizarUsuarioService = atualizarUsuarioService;
        this.inativarUsuarioService = inativarUsuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        UsuarioResponse usuario = criarUsuarioService.criarUsuario(request);
        URI localizacao = URI.create("/api/usuarios/" + usuario.idUsuario());
        return ResponseEntity.created(localizacao).body(usuario);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return listarUsuariosService.listarUsuarios();
    }

    @GetMapping("/{idUsuario}")
    public UsuarioResponse buscar(@PathVariable UUID idUsuario) {
        return buscarUsuarioService.buscarUsuario(idUsuario);
    }

    @PutMapping("/{idUsuario}")
    public UsuarioResponse atualizar(
            @PathVariable UUID idUsuario, @Valid @RequestBody AtualizarUsuarioRequest request) {
        return atualizarUsuarioService.atualizarUsuario(idUsuario, request);
    }

    @PatchMapping("/{idUsuario}/inativar")
    public UsuarioResponse inativar(@PathVariable UUID idUsuario) {
        return inativarUsuarioService.inativarUsuario(idUsuario);
    }
}
