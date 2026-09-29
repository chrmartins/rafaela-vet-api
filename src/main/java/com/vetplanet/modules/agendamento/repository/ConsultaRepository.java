package com.vetplanet.modules.agendamento.repository;

import com.vetplanet.modules.agendamento.entity.ConsultaEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acesso a {@code agendamento.consulta}. */
public interface ConsultaRepository extends JpaRepository<ConsultaEntity, UUID> {

    /**
     * As consultas de um intervalo — é como a Agenda sempre lê: o mês na
     * grade, o dia no painel lateral. O índice `consulta_por_data` sustenta.
     */
    List<ConsultaEntity> findByDataHoraBetweenOrderByDataHoraAsc(
            OffsetDateTime inicio, OffsetDateTime fim);

    /** O histórico de um animal, do mais recente para o mais antigo. */
    List<ConsultaEntity> findByIdAnimalOrderByDataHoraDesc(UUID idAnimal);

    /** Usado para saber se um animal pode ser excluído. */
    boolean existsByIdAnimal(UUID idAnimal);

    /**
     * O último atendimento de cada animal, em lote.
     *
     * <p><b>Só {@code CONCLUIDA} conta.</b> A ordem que a tela quer é "quem eu
     * vi mais recentemente" — consulta marcada para semana que vem não é
     * atendimento, e cancelada é justamente o que não aconteceu.
     *
     * <p>Devolve {@code Object[]} de {id, instante} porque o retorno é uma
     * agregação, não uma entidade. Quem chama monta o mapa.
     */
    @Query(
            "select c.idAnimal, max(c.dataHora) from ConsultaEntity c "
                    + "where c.idAnimal in :ids "
                    + "and c.status = com.vetplanet.modules.agendamento.entity.StatusConsulta.CONCLUIDA "
                    + "group by c.idAnimal")
    List<Object[]> ultimoAtendimentoPorAnimal(java.util.Collection<UUID> ids);
}