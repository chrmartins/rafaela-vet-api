package com.vetplanet.modules.prontuario.controller;

import com.vetplanet.modules.prontuario.dto.AtendimentoResponseDto;
import com.vetplanet.modules.prontuario.dto.RegistrarAtendimentoRequestDto;
import com.vetplanet.modules.prontuario.service.AbrirAtendimentoService;
import com.vetplanet.modules.prontuario.service.BuscarAtendimentoService;
import com.vetplanet.modules.prontuario.service.ConcluirAtendimentoService;
import com.vetplanet.modules.prontuario.service.RegistrarAtendimentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
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
 * Atendimentos — o registro clínico.
 *
 * <p>Sem {@code @PreAuthorize} de classe: quem pode assinar é decidido pelo
 * CRMV do usuário, não pelo perfil de acesso do sistema, e essa checagem vive
 * em {@code AbrirAtendimentoService}. Perfil não é profissão — um
 * administrador que não seja veterinário não assina prontuário.
 */
@RestController
@RequestMapping("/api/atendimentos")
@Tag(name = "Prontuário — atendimentos", description = "Registro clínico de cada consulta")
public class AtendimentoController {

    private final AbrirAtendimentoService abrirAtendimentoService;
    private final RegistrarAtendimentoService registrarAtendimentoService;
    private final ConcluirAtendimentoService concluirAtendimentoService;
    private final BuscarAtendimentoService buscarAtendimentoService;

    public AtendimentoController(
            AbrirAtendimentoService abrirAtendimentoService,
            RegistrarAtendimentoService registrarAtendimentoService,
            ConcluirAtendimentoService concluirAtendimentoService,
            BuscarAtendimentoService buscarAtendimentoService) {
        this.abrirAtendimentoService = abrirAtendimentoService;
        this.registrarAtendimentoService = registrarAtendimentoService;
        this.concluirAtendimentoService = concluirAtendimentoService;
        this.buscarAtendimentoService = buscarAtendimentoService;
    }

    @PostMapping("/consulta/{idConsulta}")
    @Operation(
            summary = "Abre o rascunho do atendimento de uma consulta",
            description =
                    "É o botão 'Iniciar atendimento'. Idempotente: chamar de novo devolve o "
                            + "rascunho que já existe, porque apertar duas vezes é engano comum. "
                            + "Só veterinário com CRMV cadastrado pode — Res. CFMV 1.321/2020, "
                            + "Art. 9º, II e VIII.")
    public AtendimentoResponseDto abrir(@PathVariable UUID idConsulta, Principal principal) {
        return abrirAtendimentoService.abrirAtendimento(idConsulta, principal.getName());
    }

    @GetMapping("/{idAtendimento}")
    @Operation(summary = "Um atendimento pelo id")
    public AtendimentoResponseDto buscar(@PathVariable UUID idAtendimento) {
        return buscarAtendimentoService.buscarAtendimento(idAtendimento);
    }

    @GetMapping("/consulta/{idConsulta}")
    @Operation(
            summary = "O atendimento de uma consulta, se já existir",
            description =
                    "204 quando a consulta ainda não tem registro — é resposta legítima, não "
                            + "erro: é o que a Agenda pergunta para o cartão dizer 'Iniciar' ou "
                            + "'Ver'.")
    public ResponseEntity<AtendimentoResponseDto> buscarPorConsulta(@PathVariable UUID idConsulta) {
        return buscarAtendimentoService
                .buscarPorConsulta(idConsulta)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/animal/{idAnimal}")
    @Operation(summary = "O histórico clínico do animal, do mais recente para o mais antigo")
    public List<AtendimentoResponseDto> listarDoAnimal(@PathVariable UUID idAnimal) {
        return buscarAtendimentoService.listarDoAnimal(idAnimal);
    }

    @PutMapping("/{idAtendimento}")
    @Operation(
            summary = "Salva o conteúdo clínico do rascunho",
            description =
                    "Aceita o registro pela metade: nota clínica é escrita em pedaços, durante a "
                            + "visita, e a tela salva sozinha enquanto ela digita. Aceita também "
                            + "depois de concluído — o prontuário cresce, e impedir isso "
                            + "empobreceria o documento. Toda alteração posterior à conclusão "
                            + "vira linha no rastro, com autor, campo, valor antigo e novo.")
    public AtendimentoResponseDto registrar(
            @PathVariable UUID idAtendimento,
            @Valid @RequestBody RegistrarAtendimentoRequestDto request,
            Principal principal) {
        return registrarAtendimentoService.registrar(idAtendimento, request, principal.getName());
    }

    @PatchMapping("/{idAtendimento}/conclusao")
    @Operation(
            summary = "Conclui o atendimento — e, com ele, a consulta",
            description =
                    "Marco, não cadeado: diz quando o atendimento foi dado por terminado e "
                            + "conclui a consulta — não existe outra forma de uma consulta virar "
                            + "CONCLUIDA. O conteúdo segue editável depois, com rastro.")
    public AtendimentoResponseDto concluir(@PathVariable UUID idAtendimento) {
        return concluirAtendimentoService.concluir(idAtendimento);
    }
}
