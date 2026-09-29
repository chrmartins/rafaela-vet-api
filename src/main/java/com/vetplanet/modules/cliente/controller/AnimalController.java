package com.vetplanet.modules.cliente.controller;

import com.vetplanet.modules.cliente.dto.AnimalResponseDto;
import com.vetplanet.modules.cliente.dto.AtualizarAnimalRequestDto;
import com.vetplanet.modules.cliente.dto.AnimalNaListaDto;
import com.vetplanet.common.dto.PaginaDto;
import com.vetplanet.modules.cliente.service.AtualizarAnimalService;
import com.vetplanet.modules.cliente.service.ListarAnimaisService;
import com.vetplanet.modules.cliente.service.BuscarAnimalService;
import com.vetplanet.modules.cliente.service.InativarAnimalService;
import com.vetplanet.modules.cliente.service.ReativarAnimalService;
import com.vetplanet.modules.cliente.service.ExcluirAnimalService;
import com.vetplanet.modules.cliente.service.RegistrarObitoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Um animal já existente.
 *
 * <p><b>Por que recurso de topo, se criar animal é sub-recurso do tutor?</b>
 * Porque as duas coisas são diferentes: um animal só nasce dentro de um tutor
 * (daí {@code POST /api/tutores/{id}/animais}), mas depois de existir ele tem
 * identidade própria e é alcançado direto — foi por isso que a criação já
 * devolvia {@code Location: /api/animais/{id}}. Exigir o id do tutor para
 * editar um animal seria pedir um dado que quem clicou no bicho não tem à mão.
 *
 * <p><b>Situação tem endpoint próprio, fora do PUT.</b> Registrar um óbito
 * não é editar um campo: é um fato clínico. Deixá-lo como campo do formulário
 * de dados cadastrais permitiria que acontecesse por descuido, num envio
 * distraído.
 *
 * <p>Sem {@code @PreAuthorize} de classe, como em {@code TutorController}:
 * é trabalho clínico do dia, não administração do sistema.
 */
@RestController
@RequestMapping("/api/animais")
public class AnimalController {

    private final ListarAnimaisService listarAnimaisService;
    private final BuscarAnimalService buscarAnimalService;
    private final AtualizarAnimalService atualizarAnimalService;
    private final RegistrarObitoService registrarObitoService;
    private final InativarAnimalService inativarAnimalService;
    private final ReativarAnimalService reativarAnimalService;
    private final ExcluirAnimalService excluirAnimalService;

    public AnimalController(
            ListarAnimaisService listarAnimaisService,
            BuscarAnimalService buscarAnimalService,
            AtualizarAnimalService atualizarAnimalService,
            RegistrarObitoService registrarObitoService,
            InativarAnimalService inativarAnimalService,
            ReativarAnimalService reativarAnimalService,
            ExcluirAnimalService excluirAnimalService) {
        this.listarAnimaisService = listarAnimaisService;
        this.buscarAnimalService = buscarAnimalService;
        this.atualizarAnimalService = atualizarAnimalService;
        this.registrarObitoService = registrarObitoService;
        this.inativarAnimalService = inativarAnimalService;
        this.reativarAnimalService = reativarAnimalService;
        this.excluirAnimalService = excluirAnimalService;
    }

    /**
     * Busca de animais, com o nome do tutor junto.
     *
     * <p>Uma lista só, e não "escolha o tutor, depois o bicho": ela pensa "a
     * Mel, da dona Ana", e às vezes lembra do bicho sem lembrar de quem é.
     *
     * <p>Sem parâmetro nenhum devolve quem está em acompanhamento, que é
     * exatamente o que o seletor da Agenda precisa — a mesma rota serve às
     * duas telas.
     *
     * @param busca trecho do nome do animal; ausente lista todos
     * @param incluirInativos por padrão a lista mostra só quem está em
     *     acompanhamento, com tutor ativo
     */
    @GetMapping
    public PaginaDto<AnimalNaListaDto> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "false") boolean incluirInativos,
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(required = false) Integer tamanho) {
        return listarAnimaisService.listarAnimais(busca, incluirInativos, pagina, tamanho);
    }

    @GetMapping("/{idAnimal}")
    public AnimalResponseDto buscar(@PathVariable UUID idAnimal) {
        return buscarAnimalService.buscarAnimal(idAnimal);
    }

    @PutMapping("/{idAnimal}")
    public AnimalResponseDto atualizar(
            @PathVariable UUID idAnimal, @Valid @RequestBody AtualizarAnimalRequestDto request) {
        return atualizarAnimalService.atualizarAnimal(idAnimal, request);
    }

    /**
     * Registra o óbito. Nada é apagado — o histórico continua inteiro.
     *
     * <p>O par {@code PATCH}/{@code DELETE} sobre o mesmo sub-recurso trata o
     * óbito como um fato que existe ou não, o que também dá o desfazer de graça.
     */
    @PatchMapping("/{idAnimal}/obito")
    public AnimalResponseDto registrarObito(@PathVariable UUID idAnimal) {
        return registrarObitoService.registrarObito(idAnimal);
    }

    /**
     * Some da lista sem apagar nada.
     *
     * <p>É o caminho para o animal que <b>já tem histórico</b> — o delete
     * falharia. Não quer dizer "parou de aparecer".
     */
    @PatchMapping("/{idAnimal}/inativar")
    public AnimalResponseDto inativar(@PathVariable UUID idAnimal) {
        return inativarAnimalService.inativarAnimal(idAnimal);
    }

    /** Volta para a lista — também desfaz um óbito lançado por engano. */
    @PatchMapping("/{idAnimal}/reativar")
    public AnimalResponseDto reativar(@PathVariable UUID idAnimal) {
        return reativarAnimalService.reativarAnimal(idAnimal);
    }

    /**
     * Exclui o cadastro de verdade — a única exclusão física do sistema.
     *
     * <p>Só passa enquanto o animal não tem nada preso a ele: consulta,
     * prontuário, qualquer histórico. A partir daí o banco recusa (chave
     * estrangeira) e a resposta é 409 explicando que o caminho é o óbito.
     *
     * <p>Existe porque cadastro criado por engano — duplicata, nome na ficha
     * errada — não é um estado do animal. Inventar um status "removido" para
     * isso deixaria lixo permanente na ficha do tutor.
     */
    @DeleteMapping("/{idAnimal}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID idAnimal) {
        excluirAnimalService.excluirAnimal(idAnimal);
    }
}
