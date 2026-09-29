package com.vetplanet.modules.agendamento.repository;

import com.vetplanet.modules.agendamento.entity.BloqueioEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acesso a {@code agendamento.bloqueio}. */
public interface BloqueioRepository extends JpaRepository<BloqueioEntity, UUID> {

    /**
     * Todos os bloqueios, semanais primeiro.
     *
     * <p><b>Sem filtro por período, de propósito.</b> São poucas linhas — um
     * almoço, um congresso, um feriado — e o semanal não tem data para
     * filtrar. Buscar tudo é uma consulta só, e a tela cruza com o dia que
     * está desenhando. Se um dia virarem centenas, o filtro entra aqui.
     *
     * <p>Semanais primeiro (sem data), depois os períodos em ordem de início.
     * <b>Não ordena por dia da semana</b>: virou array, e ordenar por array no
     * banco não diria nada — a tela separa as duas listas de qualquer forma.
     */
    @Query(
            "select b from BloqueioEntity b "
                    + "order by b.dataInicio asc nulls first, b.horaInicio asc nulls first")
    List<BloqueioEntity> listarOrdenados();
}
