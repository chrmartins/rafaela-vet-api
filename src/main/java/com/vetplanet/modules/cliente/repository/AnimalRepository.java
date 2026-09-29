package com.vetplanet.modules.cliente.repository;

import com.vetplanet.modules.cliente.entity.AnimalEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acesso a {@code cliente.animal}. */
public interface AnimalRepository extends JpaRepository<AnimalEntity, UUID> {

    List<AnimalEntity> findByTutorIdOrderByNomeAsc(UUID idTutor);

    long countByTutorId(UUID idTutor);

    /**
     * Animal com o tutor já carregado.
     *
     * <p>A relação é LAZY, então quem precisa do tutor junto pede aqui. Sem o
     * {@code join fetch}, montar o DTO fora da transação estoura com
     * {@code LazyInitializationException} — e dentro dela vira consulta extra
     * a cada animal.
     */
    @Query("select a from AnimalEntity a join fetch a.tutor where a.id = :idAnimal")
    Optional<AnimalEntity> buscarComTutor(UUID idAnimal);

    /**
     * Vários animais com o tutor junto, numa consulta só.
     *
     * <p>Usado por {@code ResumirAnimaisService} para a Agenda montar
     * "Thor · J. Lima" sem disparar uma consulta por linha.
     */
    @Query("select a from AnimalEntity a join fetch a.tutor where a.id in :ids")
    List<AnimalEntity> buscarComTutorPorIds(java.util.Collection<UUID> ids);

    /** Em acompanhamento, com o tutor junto — alimenta o seletor da Agenda. */
    @Query(
            "select a from AnimalEntity a join fetch a.tutor "
                    + "where a.situacao = :situacao and a.tutor.ativo = true "
                    + "order by a.nome asc")
    List<AnimalEntity> buscarAtivosComTutor(com.vetplanet.modules.cliente.entity.SituacaoAnimal situacao);

    /**
     * A busca da tela de animais — "a gata Mel", sem saber o tutor.
     *
     * <p><b>{@code trecho} vazio lista todos</b> — e vazio, não nulo, de
     * propósito. Com {@code (:trecho is null or lower(...) like ...)} o
     * Postgres não tem como inferir o tipo do parâmetro nulo, assume
     * {@code bytea} e a consulta estoura com "function lower(bytea) does not
     * exist". String vazia vira {@code like '%%'}, que casa com tudo, e o
     * problema deixa de existir em vez de ser contornado com cast.
     *
     * <p>As situações vêm de fora porque a tela decide o que mostrar: só quem
     * está em acompanhamento, por padrão, ou também inativos e falecidos
     * quando ela pede.
     *
     * <p>O tutor inativo cai junto: se ela parou de atender a dona Ana, os
     * bichos da Ana não deveriam aparecer na busca do dia a dia.
     */
    @Query(
            "select a from AnimalEntity a join fetch a.tutor "
                    + "where lower(a.nome) like lower(concat('%', :trecho, '%')) "
                    + "and a.situacao in :situacoes "
                    + "and (:incluirInativos = true or a.tutor.ativo = true) "
                    + "order by a.nome asc")
    List<AnimalEntity> buscarComTutor(
            String trecho,
            java.util.Collection<com.vetplanet.modules.cliente.entity.SituacaoAnimal> situacoes,
            boolean incluirInativos);

    /**
     * Os animais de vários tutores de uma vez.
     *
     * <p>Substitui a contagem por tutor que a lista fazia — uma consulta por
     * linha — e ainda dá os ids necessários para descobrir o último atendimento
     * de cada tutor em lote.
     */
    List<AnimalEntity> findByTutorIdIn(java.util.Collection<UUID> idsDeTutores);
}