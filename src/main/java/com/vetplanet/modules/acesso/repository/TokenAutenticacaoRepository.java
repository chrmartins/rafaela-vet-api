package com.vetplanet.modules.acesso.repository;

import com.vetplanet.modules.acesso.entity.TokenAutenticacaoEntity;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenAutenticacaoRepository extends JpaRepository<TokenAutenticacaoEntity, UUID> {

    /**
     * Busca a sessão pelo hash do token apresentado. É o caminho quente: roda
     * a cada request autenticado.
     *
     * <p>O {@code join fetch} traz o usuário na mesma consulta de propósito.
     * Sem ele, o filtro de autenticação precisaria de {@code @Transactional}
     * para acessar {@code sessao.getUsuario()} — e anotar um filtro como
     * transacional faz o Spring criar um proxy CGLIB dele, instanciado sem
     * construtor, o que deixa nulo o {@code logger} herdado de
     * {@code GenericFilterBean} e quebra a aplicação no boot.
     */
    @Query("select t from TokenAutenticacaoEntity t join fetch t.usuario where t.tokenHash = :tokenHash")
    Optional<TokenAutenticacaoEntity> buscarPorTokenHashComUsuario(@Param("tokenHash") String tokenHash);

    Optional<TokenAutenticacaoEntity> findByTokenHash(String tokenHash);

    /** Sessões ainda não revogadas de um usuário — usado ao derrubar acessos. */
    List<TokenAutenticacaoEntity> findByUsuarioAndRevogadoEmIsNull(UsuarioEntity usuario);
}
