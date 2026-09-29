package com.vetplanet.modules.acesso.controller;

import com.vetplanet.modules.acesso.dto.AtualizarDadosDoReceituarioRequestDto;
import com.vetplanet.modules.acesso.dto.AtualizarUsuarioRequestDto;
import com.vetplanet.modules.acesso.dto.CriarUsuarioRequestDto;
import com.vetplanet.modules.acesso.dto.TrocarSenhaRequestDto;
import com.vetplanet.modules.acesso.dto.UsuarioResponseDto;
import com.vetplanet.modules.acesso.service.AtualizarDadosDoReceituarioService;
import com.vetplanet.modules.acesso.service.AtualizarUsuarioService;
import com.vetplanet.modules.acesso.service.BuscarUsuarioService;
import com.vetplanet.modules.acesso.service.CriarUsuarioService;
import com.vetplanet.modules.acesso.service.InativarUsuarioService;
import com.vetplanet.modules.acesso.service.ListarUsuariosService;
import com.vetplanet.modules.acesso.service.TrocarSenhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Usuários do painel administrativo.
 *
 * <p>Recurso no plural e sem verbo na URL — o verbo é o método HTTP. Não
 * existe endpoint de auto-cadastro: quem cria usuário é o administrador.
 *
 * <p><b>Gerenciar usuário é exclusivo de {@code ADMINISTRADOR}</b> — a
 * anotação vale para toda a classe, então um endpoint novo já nasce
 * protegido. A exceção é {@code /atual/senha}, que cada um usa na própria
 * conta.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(
        name = "Acesso — usuários",
        description =
                "Gestão dos usuários do painel. Exige perfil ADMINISTRADOR, exceto a troca da"
                        + " própria senha. Não existe auto-cadastro.")
public class UsuarioController {

    private final CriarUsuarioService criarUsuarioService;
    private final ListarUsuariosService listarUsuariosService;
    private final BuscarUsuarioService buscarUsuarioService;
    private final AtualizarUsuarioService atualizarUsuarioService;
    private final InativarUsuarioService inativarUsuarioService;
    private final TrocarSenhaService trocarSenhaService;
    private final AtualizarDadosDoReceituarioService atualizarDadosDoReceituarioService;

    public UsuarioController(
            CriarUsuarioService criarUsuarioService,
            ListarUsuariosService listarUsuariosService,
            BuscarUsuarioService buscarUsuarioService,
            AtualizarUsuarioService atualizarUsuarioService,
            InativarUsuarioService inativarUsuarioService,
            TrocarSenhaService trocarSenhaService,
            AtualizarDadosDoReceituarioService atualizarDadosDoReceituarioService) {
        this.criarUsuarioService = criarUsuarioService;
        this.listarUsuariosService = listarUsuariosService;
        this.buscarUsuarioService = buscarUsuarioService;
        this.atualizarUsuarioService = atualizarUsuarioService;
        this.inativarUsuarioService = inativarUsuarioService;
        this.trocarSenhaService = trocarSenhaService;
        this.atualizarDadosDoReceituarioService = atualizarDadosDoReceituarioService;
    }

    @Operation(
            summary = "Criar usuário",
            description = "A senha entra com hash BCrypt e nunca volta em resposta de API.")
    @PostMapping
    public ResponseEntity<UsuarioResponseDto> criar(@Valid @RequestBody CriarUsuarioRequestDto request) {
        UsuarioResponseDto usuario = criarUsuarioService.criarUsuario(request);
        URI localizacao = URI.create("/api/usuarios/" + usuario.idUsuario());
        return ResponseEntity.created(localizacao).body(usuario);
    }

    @Operation(summary = "Listar usuários", description = "Ordenados por nome completo.")
    @GetMapping
    public List<UsuarioResponseDto> listar() {
        return listarUsuariosService.listarUsuarios();
    }

    @Operation(summary = "Buscar usuário por id")
    @GetMapping("/{idUsuario}")
    public UsuarioResponseDto buscar(@PathVariable UUID idUsuario) {
        return buscarUsuarioService.buscarUsuario(idUsuario);
    }

    @Operation(
            summary = "Atualizar dados cadastrais",
            description =
                    "Senha não entra aqui — trocar senha é outra operação, com outras regras.")
    @PutMapping("/{idUsuario}")
    public UsuarioResponseDto atualizar(
            @PathVariable UUID idUsuario, @Valid @RequestBody AtualizarUsuarioRequestDto request) {
        return atualizarUsuarioService.atualizarUsuario(idUsuario, request);
    }

    @Operation(
            summary = "Inativar usuário",
            description =
                    "Não há exclusão física (LGPD). Inativar derruba as sessões abertas. O"
                            + " último administrador ativo não pode ser inativado.")
    @PatchMapping("/{idUsuario}/inativar")
    public UsuarioResponseDto inativar(@PathVariable UUID idUsuario) {
        return inativarUsuarioService.inativarUsuario(idUsuario);
    }

    /**
     * Os próprios dados de contato — qualquer perfil, sobre a própria conta.
     *
     * <p>Como a troca de senha, sobrepõe o {@code @PreAuthorize} da classe: não
     * faz sentido exigir administrador para alguém preencher o rodapé do
     * próprio receituário. O alvo vem da sessão, nunca da URL.
     */
    @Operation(
            summary = "Atualizar os próprios dados do receituário",
            description =
                    "Telefone, Instagram, site e cidade — o rodapé do papel timbrado. Nome,"
                            + " e-mail e perfil não entram: mudar o próprio perfil seria escalar"
                            + " privilégio.")
    @PutMapping("/atual/receituario")
    @PreAuthorize("isAuthenticated()")
    public UsuarioResponseDto atualizarDadosDoReceituario(
            Principal principal,
            @Valid @RequestBody AtualizarDadosDoReceituarioRequestDto request) {
        return atualizarDadosDoReceituarioService.atualizar(principal.getName(), request);
    }

    /**
     * Trocar a própria senha — qualquer perfil, sobre a própria conta.
     *
     * <p>Sobrepõe o {@code @PreAuthorize} da classe: não faz sentido exigir
     * perfil de administrador para alguém trocar a senha dela mesma. O alvo
     * vem da sessão, nunca da URL, então não há como mexer na conta de outro.
     */
    @Operation(
            summary = "Trocar a própria senha",
            description =
                    "Qualquer perfil, sobre a própria conta — o alvo vem da sessão, nunca da"
                            + " URL. Exige a senha atual e derruba todas as sessões.")
    @PatchMapping("/atual/senha")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(
            Principal principal, @Valid @RequestBody TrocarSenhaRequestDto request) {
        trocarSenhaService.trocarSenha(principal.getName(), request);
    }
}
