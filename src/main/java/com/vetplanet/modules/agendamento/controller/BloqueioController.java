package com.vetplanet.modules.agendamento.controller;

import com.vetplanet.modules.agendamento.dto.BloqueioResponseDto;
import com.vetplanet.modules.agendamento.dto.CriarBloqueioRequestDto;
import com.vetplanet.modules.agendamento.service.AtualizarBloqueioService;
import com.vetplanet.modules.agendamento.service.CriarBloqueioService;
import com.vetplanet.modules.agendamento.service.ExcluirBloqueioService;
import com.vetplanet.modules.agendamento.service.ListarBloqueiosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bloqueios — o tempo que não é para atender.
 *
 * <p>Recurso próprio, e não {@code /api/agendas/disponibilidade} como o
 * {@code padrao-nomenclatura.md} previa. <b>Desvio deliberado</b>: o que está
 * implementado é o negativo (o que <i>não</i> dá), não o positivo (a janela em
 * que ela atende). Chamar um bloqueio de "disponibilidade" inverteria o
 * sentido de quem lesse o endpoint.
 *
 * <p>Sem {@code @PreAuthorize} de classe, como em {@code ConsultaController}:
 * marcar o próprio almoço é trabalho do dia, não administração do sistema.
 */
@RestController
@RequestMapping("/api/bloqueios")
@Tag(name = "Agendamento — bloqueios", description = "Tempo indisponível: almoço, congresso, feriado")
public class BloqueioController {

    private final CriarBloqueioService criarBloqueioService;
    private final ListarBloqueiosService listarBloqueiosService;
    private final AtualizarBloqueioService atualizarBloqueioService;
    private final ExcluirBloqueioService excluirBloqueioService;

    public BloqueioController(
            CriarBloqueioService criarBloqueioService,
            ListarBloqueiosService listarBloqueiosService,
            AtualizarBloqueioService atualizarBloqueioService,
            ExcluirBloqueioService excluirBloqueioService) {
        this.criarBloqueioService = criarBloqueioService;
        this.listarBloqueiosService = listarBloqueiosService;
        this.atualizarBloqueioService = atualizarBloqueioService;
        this.excluirBloqueioService = excluirBloqueioService;
    }

    @PostMapping
    @Operation(
            summary = "Bloqueia um tempo na agenda",
            description =
                    "Duas formas: semanal (diasDaSemana, 0=domingo..6=sábado, vários por "
                            + "bloqueio) ou período (dataInicio e dataFim). Nunca as duas. Sem "
                            + "horaInicio/horaFim, bloqueia o dia inteiro. As horas são civis do "
                            + "fuso da clínica, não instantes UTC — bloqueio semanal não é um "
                            + "ponto no tempo.")
    public ResponseEntity<BloqueioResponseDto> criar(
            @Valid @RequestBody CriarBloqueioRequestDto request) {
        BloqueioResponseDto bloqueio = criarBloqueioService.criarBloqueio(request);
        return ResponseEntity.created(URI.create("/api/bloqueios/" + bloqueio.idBloqueio()))
                .body(bloqueio);
    }

    @GetMapping
    @Operation(
            summary = "Todos os bloqueios cadastrados",
            description =
                    "Devolve a regra, não as ocorrências: um almoço semanal é uma linha, não "
                            + "trinta. Quem desenha o dia é que cruza com o dia da semana.")
    public List<BloqueioResponseDto> listar() {
        return listarBloqueiosService.listarBloqueios();
    }

    @PutMapping("/{idBloqueio}")
    @Operation(
            summary = "Substitui um bloqueio",
            description =
                    "PUT e não PATCH: substitui o conteúdo inteiro, inclusive a forma — um "
                            + "bloqueio semanal pode virar período. Campos soltos deixariam "
                            + "conviver dia da semana e datas, que é a linha que o banco recusa. "
                            + "Existe para mudar o almoço das 12h para as 13h sem apagar e "
                            + "recriar; bloqueio não tem histórico preso, então a edição é direta.")
    public BloqueioResponseDto atualizar(
            @PathVariable UUID idBloqueio, @Valid @RequestBody CriarBloqueioRequestDto request) {
        return atualizarBloqueioService.atualizarBloqueio(idBloqueio, request);
    }

    @DeleteMapping("/{idBloqueio}")
    @Operation(
            summary = "Apaga um bloqueio",
            description =
                    "Exclusão física, ao contrário de consulta e prontuário: bloqueio é regra de "
                            + "agenda, não histórico clínico — não há o que preservar.")
    public ResponseEntity<Void> excluir(@PathVariable UUID idBloqueio) {
        excluirBloqueioService.excluirBloqueio(idBloqueio);
        return ResponseEntity.noContent().build();
    }
}
