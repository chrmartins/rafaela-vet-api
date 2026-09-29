package com.vetplanet.modules.cliente.repository;

import com.vetplanet.modules.cliente.entity.TutorEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@code cliente.tutor}.
 *
 * <p>Sem unicidade de e-mail ou telefone: dois moradores da mesma casa podem
 * dividir o número, e o e-mail é opcional. Duplicata de pessoa se resolve na
 * busca da tela, não com constraint que trava um caso legítimo.
 */
public interface TutorRepository extends JpaRepository<TutorEntity, UUID> {

    List<TutorEntity> findAllByOrderByNomeCompletoAsc();

    List<TutorEntity> findByAtivoTrueOrderByNomeCompletoAsc();

    /** Busca por parte do nome, ignorando maiúsculas — o campo de busca da tela. */
    List<TutorEntity> findByNomeCompletoContainingIgnoreCaseOrderByNomeCompletoAsc(String trecho);
}
