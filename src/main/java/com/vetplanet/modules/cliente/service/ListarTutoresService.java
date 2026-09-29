package com.vetplanet.modules.cliente.service;

import com.vetplanet.modules.cliente.dto.TutorResponseDto;
import com.vetplanet.modules.cliente.entity.AnimalEntity;
import com.vetplanet.modules.cliente.entity.TutorEntity;
import com.vetplanet.modules.cliente.repository.AnimalRepository;
import com.vetplanet.modules.cliente.repository.TutorRepository;
import com.vetplanet.common.dto.PaginaDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lista tutores, com busca opcional por nome. */
@Service
public class ListarTutoresService {

    private final TutorRepository tutorRepository;
    private final AnimalRepository animalRepository;
    private final HistoricoDoAnimal historicoDoAnimal;

    public ListarTutoresService(
            TutorRepository tutorRepository,
            AnimalRepository animalRepository,
            HistoricoDoAnimal historicoDoAnimal) {
        this.tutorRepository = tutorRepository;
        this.animalRepository = animalRepository;
        this.historicoDoAnimal = historicoDoAnimal;
    }

    /**
     * @param busca trecho do nome; nulo ou em branco lista todos
     * @param incluirInativos falso devolve só quem está ativo
     */
    @Transactional(readOnly = true)
    public List<TutorResponseDto> listarTutores(String busca, boolean incluirInativos) {
        return ordenarPorAtendimento(buscar(busca, incluirInativos));
    }

    /** A mesma lista, recortada em páginas. Ver {@code ListarAnimaisService}. */
    @Transactional(readOnly = true)
    public PaginaDto<TutorResponseDto> listarTutores(
            String busca, boolean incluirInativos, int pagina, Integer tamanhoDaPagina) {
        List<TutorResponseDto> todos = listarTutores(busca, incluirInativos);

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
     * Tutor mais recentemente atendido em cima.
     *
     * <p><b>"Atendimento do tutor" é o do animal dele mais recente.</b> Tutor
     * não é atendido — o bicho é. Quem tem três animais aparece pela visita
     * mais próxima entre as três, que é como ela lembra da clientela.
     *
     * <p>Aproveita a varredura para matar o N+1 que existia aqui: a contagem de
     * animais era uma consulta por tutor, e agora sai da mesma leitura que
     * descobre as datas.
     */
    private List<TutorResponseDto> ordenarPorAtendimento(List<TutorEntity> tutores) {
        List<UUID> idsDeTutores = tutores.stream().map(TutorEntity::getId).toList();
        List<AnimalEntity> animais =
                idsDeTutores.isEmpty() ? List.of() : animalRepository.findByTutorIdIn(idsDeTutores);

        Map<UUID, Instant> porAnimal =
                historicoDoAnimal.ultimoAtendimentoPorAnimal(
                        animais.stream().map(AnimalEntity::getId).toList());

        Map<UUID, Long> quantidadePorTutor = new HashMap<>();
        Map<UUID, Instant> ultimoPorTutor = new HashMap<>();
        for (AnimalEntity animal : animais) {
            UUID idTutor = animal.getTutor().getId();
            quantidadePorTutor.merge(idTutor, 1L, Long::sum);

            Instant doAnimal = porAnimal.get(animal.getId());
            if (doAnimal == null) continue;
            ultimoPorTutor.merge(idTutor, doAnimal, (a, b) -> a.isAfter(b) ? a : b);
        }

        Comparator<TutorEntity> maisRecentePrimeiro =
                Comparator.comparing(
                                (TutorEntity t) -> ultimoPorTutor.get(t.getId()),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TutorEntity::getNomeCompleto, String.CASE_INSENSITIVE_ORDER);

        return tutores.stream()
                .sorted(maisRecentePrimeiro)
                .map(
                        tutor ->
                                TutorResponseDto.resumo(
                                        tutor, quantidadePorTutor.getOrDefault(tutor.getId(), 0L)))
                .toList();
    }

    private List<TutorEntity> buscar(String busca, boolean incluirInativos) {
        if (busca != null && !busca.isBlank()) {
            List<TutorEntity> encontrados =
                    tutorRepository.findByNomeCompletoContainingIgnoreCaseOrderByNomeCompletoAsc(
                            busca.trim());
            return incluirInativos ? encontrados : encontrados.stream().filter(TutorEntity::isAtivo).toList();
        }
        return incluirInativos
                ? tutorRepository.findAllByOrderByNomeCompletoAsc()
                : tutorRepository.findByAtivoTrueOrderByNomeCompletoAsc();
    }
}
