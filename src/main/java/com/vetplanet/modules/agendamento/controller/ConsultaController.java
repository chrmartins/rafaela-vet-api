package com.vetplanet.modules.agendamento.controller;

import com.vetplanet.modules.agendamento.dto.AlterarStatusRequestDto;
import com.vetplanet.modules.agendamento.dto.AtualizarConsultaRequestDto;
import com.vetplanet.modules.agendamento.dto.ConsultaResponseDto;
import com.vetplanet.modules.agendamento.dto.CriarConsultaRequestDto;
import com.vetplanet.modules.agendamento.entity.StatusConsulta;
import com.vetplanet.modules.agendamento.exception.StatusConcluidaReservadoException;
import com.vetplanet.modules.agendamento.service.AlterarStatusConsultaService;
import com.vetplanet.modules.agendamento.service.AtualizarConsultaService;
import com.vetplanet.modules.agendamento.service.BuscarConsultaService;
import com.vetplanet.modules.agendamento.service.CriarConsultaService;
import com.vetplanet.modules.agendamento.service.ListarConsultasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas — o que alimenta a Agenda.
 *
 * <p>Sem {@code @PreAuthorize} de classe: marcar e confirmar atendimento é o
 * trabalho do dia, não administração do sistema. Qualquer sessão válida basta,
 * o que o {@code SecurityConfig} já garante.
 */
@RestController
@RequestMapping("/api/consultas")
@Tag(name = "Agendamento — consultas", description = "Atendimentos marcados; alimenta a Agenda")
public class ConsultaController {

    private final CriarConsultaService criarConsultaService;
    private final ListarConsultasService listarConsultasService;
    private final BuscarConsultaService buscarConsultaService;
    private final AtualizarConsultaService atualizarConsultaService;
    private final AlterarStatusConsultaService alterarStatusConsultaService;

    public ConsultaController(
            CriarConsultaService criarConsultaService,
            ListarConsultasService listarConsultasService,
            BuscarConsultaService buscarConsultaService,
            AtualizarConsultaService atualizarConsultaService,
            AlterarStatusConsultaService alterarStatusConsultaService) {
        this.criarConsultaService = criarConsultaService;
        this.listarConsultasService = listarConsultasService;
        this.buscarConsultaService = buscarConsultaService;
        this.atualizarConsultaService = atualizarConsultaService;
        this.alterarStatusConsultaService = alterarStatusConsultaService;
    }

    @PostMapping
    @Operation(
            summary = "Marca uma consulta",
            description =
                    "Só o animal é informado — o tutor é resolvido a partir dele e gravado junto, "
                            + "para o histórico continuar apontando o responsável da época se o "
                            + "animal mudar de dono. Sem status, entra como CONFIRMADA: na maior "
                            + "parte das vezes o horário já foi acertado antes de chegar aqui.")
    public ResponseEntity<ConsultaResponseDto> criar(
            @Valid @RequestBody CriarConsultaRequestDto request) {
        ConsultaResponseDto consulta = criarConsultaService.criarConsulta(request);
        return ResponseEntity.created(URI.create("/api/consultas/" + consulta.idConsulta()))
                .body(consulta);
    }

    @GetMapping
    @Operation(
            summary = "Consultas de um intervalo",
            description =
                    "É como a Agenda lê: o mês na grade, o dia no painel. Os nomes de animal e "
                            + "tutor vêm resolvidos numa consulta só, não uma por linha.")
    public List<ConsultaResponseDto> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime ate) {
        return listarConsultasService.listarConsultas(de, ate);
    }

    @GetMapping("/{idConsulta}")
    @Operation(summary = "Uma consulta pelo id — o que a tela de edição carrega")
    public ConsultaResponseDto buscar(@PathVariable UUID idConsulta) {
        return buscarConsultaService.buscarConsulta(idConsulta);
    }

    @GetMapping("/animal/{idAnimal}")
    @Operation(summary = "Histórico de um animal, do mais recente para o mais antigo")
    public List<ConsultaResponseDto> listarDoAnimal(@PathVariable UUID idAnimal) {
        return listarConsultasService.listarConsultasDoAnimal(idAnimal);
    }

    @PutMapping("/{idConsulta}")
    @Operation(
            summary = "Corrige ou remarca uma consulta",
            description =
                    "Existe para remarcar não ser cancelar e criar de novo — CANCELADA quer dizer "
                            + "que o atendimento não aconteceu, e um remarcado aconteceu, só que "
                            + "mais tarde. NÃO aceita idAnimal: a consulta é a âncora do histórico "
                            + "do animal, e trocá-lo reescreveria dois históricos de uma vez; "
                            + "marcar no bicho errado se resolve cancelando e marcando de novo, e "
                            + "ali o cancelamento é honesto. Status também não entra: tem "
                            + "endpoint próprio.")
    public ConsultaResponseDto atualizar(
            @PathVariable UUID idConsulta,
            @Valid @RequestBody AtualizarConsultaRequestDto request) {
        return atualizarConsultaService.atualizarConsulta(idConsulta, request);
    }

    @PatchMapping("/{idConsulta}/status")
    @Operation(
            summary = "Confirma, cancela ou conclui",
            description =
                    "Cancelar é status, não exclusão: a consulta continua no histórico. Um "
                            + "endpoint com o status no corpo, e não quatro rotas com verbo, "
                            + "porque os quatro estados são o mesmo eixo. NÃO aceita CONCLUIDA: "
                            + "uma consulta é concluída ao concluir o atendimento dela, e uma "
                            + "porta lateral aqui permitiria marcá-la como realizada sem o "
                            + "prontuário que a lei exige.")
    public ConsultaResponseDto alterarStatus(
            @PathVariable UUID idConsulta, @Valid @RequestBody AlterarStatusRequestDto request) {
        // A trava vive no controller, e não no service: quem conclui de
        // verdade é `ConcluirAtendimentoService`, que chama o mesmo service
        // internamente. O que não pode existir é a porta HTTP.
        if (request.status() == StatusConsulta.CONCLUIDA) {
            throw new StatusConcluidaReservadoException();
        }
        return alterarStatusConsultaService.alterarStatus(idConsulta, request.status());
    }
}
