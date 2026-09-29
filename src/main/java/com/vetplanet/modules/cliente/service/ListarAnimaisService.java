package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.AnimalNaListaDto;
import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.entity.SituacaoAnimal;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.common.dto.PaginaDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A lista de animais, com o nome do tutor junto.
 *
 * <p><b>Uma lista só, e não "escolha o tutor, depois o bicho".</b> Ela pensa
 * "a Mel, da dona Ana" — e às vezes lembra do bicho sem lembrar de quem é.
 *
 * <p>Serve a dois lugares com a mesma consulta: o seletor do formulário de
 * agendamento, que chama sem parâmetro nenhum e recebe só quem está em
 * acompanhamento, e a tela de busca de animais, que passa o trecho do nome e
 * pode pedir os inativos.
 *
 * <p><b>Falecidos e inativos ficam de fora por padrão</b>, e isso é o que faz
 * o seletor continuar correto sem saber de nada: não se marca consulta para
 * quem não está em acompanhamento.
 */
@Service
public class ListarAnimaisService {

    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public ListarAnimaisService(
            AnimalRepository animalRepository, HistoricoDoAnimal historicoDoAnimal) {
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    /**
     * @param busca trecho do nome do animal; nulo ou vazio lista todos
     * @param incluirInativos traz também inativos, falecidos e bichos de tutor
     *     inativo — por padrão a lista mostra só quem está em acompanhamento
     */
    @Transactional(readOnly = true)
    public List<AnimalNaListaDto> listarAnimais(String busca, boolean incluirInativos) {
        return ordenarPorAtendimento(buscar(busca, incluirInativos));
    }

    /**
     * A mesma lista, recortada em páginas.
     *
     * <p>A ordenação vem de {@code agendamento} (a data do último atendimento),
     * então não cabe num {@code order by} sem uma consulta atravessar a
     * fronteira entre schemas. Ordena-se em memória e recorta-se depois — ver a
     * nota de escala em {@code PaginaDto.recortar}.
     */
    @Transactional(readOnly = true)
    public PaginaDto<AnimalNaListaDto> listarAnimais(
            String busca, boolean incluirInativos, int pagina, Integer tamanhoDaPagina) {
        List<AnimalNaListaDto> todos = listarAnimais(busca, incluirInativos);

        // **`tamanho` ausente devolve tudo numa página.** O seletor de animal da
        // Agenda lê esta mesma rota e precisa da lista inteira; um padrão
        // numérico truncaria em silêncio, e o bug seria "sumiu um animal do
        // menu" — o tipo que ninguém liga à paginação.
        if (tamanhoDaPagina == null) {
            return new PaginaDto<>(todos, 1, Math.max(1, todos.size()), todos.size());
        }
        return PaginaDto.recortar(todos, pagina, tamanhoDaPagina);
    }

    /**
     * Mais recente em cima, e quem nunca foi atendido por último, em ordem
     * alfabética.
     *
     * <p>Nunca atendido no fim, e não no topo: a lista responde "quem eu tenho
     * visto", e cadastro sem atendimento nenhum é justamente o que ainda não
     * entrou nessa conta. Entre eles, alfabético — porque não há nada que os
     * desempate, e ordem instável faz a lista mudar sozinha entre visitas.
     */
    private List<AnimalNaListaDto> ordenarPorAtendimento(List<AnimalEntity> animais) {
        Map<java.util.UUID, Instant> ultimoAtendimento =
                historicoDoAnimal.ultimoAtendimentoPorAnimal(
                        animais.stream().map(AnimalEntity::getId).toList());

        Comparator<AnimalEntity> maisRecentePrimeiro =
                Comparator.comparing(
                                (AnimalEntity a) -> ultimoAtendimento.get(a.getId()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AnimalEntity::getNome, String.CASE_INSENSITIVE_ORDER);

        return animais.stream().sorted(maisRecentePrimeiro).map(AnimalNaListaDto::de).toList();
    }

    private List<AnimalEntity> buscar(String busca, boolean incluirInativos) {
        // Vazio, e não nulo: parâmetro nulo dentro de `lower()` faz o Postgres
        // assumir `bytea` e a consulta estourar. Ver `buscarComTutor`.
        String trecho = busca == null ? "" : busca.trim();

        List<SituacaoAnimal> situacoes =
                incluirInativos
                        ? List.of(SituacaoAnimal.values())
                        : List.of(SituacaoAnimal.ATIVO);

        return animalRepository.buscarComTutor(trecho, situacoes, incluirInativos);
    }
}
