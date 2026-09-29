package com.vetplanet.modules.prontuario.repository;

import com.vetplanet.modules.prontuario.entity.AtendimentoEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso a {@code prontuario.atendimento}. */
public interface AtendimentoRepository extends JpaRepository<AtendimentoEntity, UUID> {

    /** O atendimento de uma consulta — é como a Agenda pergunta "já registrou?". */
    Optional<AtendimentoEntity> findByIdConsulta(UUID idConsulta);

    /** O histórico clínico do animal, do mais recente para o mais antigo. */
    List<AtendimentoEntity> findByIdAnimalOrderByCriadoEmDesc(UUID idAnimal);

    /**
     * Existe atendimento CONCLUÍDO para esta consulta?
     *
     * <p>É o que responde se a consulta ainda pode ser remarcada. Rascunho não
     * trava nada: ela abriu a tela e desistiu, e isso não é fato registrado.
     */
    boolean existsByIdConsultaAndConcluidoEmNotNull(UUID idConsulta);

    /** Quais destas consultas já têm atendimento — para a Agenda marcar o cartão. */
    @org.springframework.data.jpa.repository.Query(
            "select a.idConsulta from AtendimentoEntity a where a.idConsulta in :idsConsulta")
    List<UUID> idsDeConsultaComAtendimento(java.util.Collection<UUID> idsConsulta);
}
