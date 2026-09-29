package com.vetplanet.modules.cliente.controller;

import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.dto.AtualizarTutorRequestDto;
import com.vetplanet.modules.cliente.dto.CriarAnimalRequestDto;
import com.vetplanet.modules.cliente.dto.CriarTutorRequestDto;
import com.vetplanet.modules.cliente.dto.TutorResponseDto;
import com.vetplanet.common.dto.PaginaDto;
import com.vetplanet.modules.cliente.service.AdicionarAnimalService;
import com.vetplanet.modules.cliente.service.AtualizarTutorService;
import com.vetplanet.modules.cliente.service.BuscarTutorService;
import com.vetplanet.modules.cliente.service.CriarTutorService;
import com.vetplanet.modules.cliente.service.InativarTutorService;
import com.vetplanet.modules.cliente.service.ListarTutoresService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tutores e os animais deles.
 *
 * <p><b>Sem {@code @PreAuthorize} de classe, ao contrário de
 * {@code UsuarioController}.</b> Gerenciar usuários é administração do
 * sistema; cadastrar tutor e animal é o trabalho clínico do dia — veterinária
 * e atendente precisam fazer isso. Qualquer sessão válida basta, o que já é
 * garantido pelo {@code SecurityConfig} (só o login e o health são públicos).
 *
 * <p>Os animais aparecem como sub-recurso ({@code /tutores/{id}/animais}) e
 * não como recurso de topo: animal não existe sem tutor, e a URL diz isso.
 */
@RestController
@RequestMapping("/api/tutores")
public class TutorController {

    private final CriarTutorService criarTutorService;
    private final ListarTutoresService listarTutoresService;
    private final BuscarTutorService buscarTutorService;
    private final AtualizarTutorService atualizarTutorService;
    private final InativarTutorService inativarTutorService;
    private final AdicionarAnimalService adicionarAnimalService;

    public TutorController(
            CriarTutorService criarTutorService,
            ListarTutoresService listarTutoresService,
            BuscarTutorService buscarTutorService,
            AtualizarTutorService atualizarTutorService,
            InativarTutorService inativarTutorService,
            AdicionarAnimalService adicionarAnimalService) {
        this.criarTutorService = criarTutorService;
        this.listarTutoresService = listarTutoresService;
        this.buscarTutorService = buscarTutorService;
        this.atualizarTutorService = atualizarTutorService;
        this.inativarTutorService = inativarTutorService;
        this.adicionarAnimalService = adicionarAnimalService;
    }

    /** Cadastra o tutor <b>e os animais dele</b> numa requisição só. */
    @PostMapping
    public ResponseEntity<TutorResponseDto> criar(
            @Valid @RequestBody CriarTutorRequestDto request) {
        TutorResponseDto tutor = criarTutorService.criarTutor(request);
        return ResponseEntity.created(URI.create("/api/tutores/" + tutor.idTutor())).body(tutor);
    }

    /**
     * @param busca trecho do nome do tutor; ausente lista todos
     * @param incluirInativos por padrão a lista mostra só quem está ativo
     */
    @GetMapping
    public PaginaDto<TutorResponseDto> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "false") boolean incluirInativos,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(required = false) Integer tamanho) {
        return listarTutoresService.listarTutores(busca, incluirInativos, pagina, tamanho);
    }

    @GetMapping("/{idTutor}")
    public TutorResponseDto buscar(@PathVariable UUID idTutor) {
        return buscarTutorService.buscarTutor(idTutor);
    }

    @PutMapping("/{idTutor}")
    public TutorResponseDto atualizar(
            @PathVariable UUID idTutor, @Valid @RequestBody AtualizarTutorRequestDto request) {
        return atualizarTutorService.atualizarTutor(idTutor, request);
    }

    /** Inativação lógica — não há exclusão física de tutor. */
    @PatchMapping("/{idTutor}/inativar")
    public TutorResponseDto inativar(@PathVariable UUID idTutor) {
        return inativarTutorService.inativarTutor(idTutor);
    }

    @PostMapping("/{idTutor}/animais")
    public ResponseEntity<AnimalResponseDto> adicionarAnimal(
            @PathVariable UUID idTutor, @Valid @RequestBody CriarAnimalRequestDto request) {
        AnimalResponseDto animal = adicionarAnimalService.adicionarAnimal(idTutor, request);
        return ResponseEntity.created(URI.create("/api/animais/" + animal.idAnimal())).body(animal);
    }
}
