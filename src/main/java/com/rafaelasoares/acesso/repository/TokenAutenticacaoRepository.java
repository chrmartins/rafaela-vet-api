package com.rafaelasoares.acesso.repository;

import com.rafaelasoares.acesso.entity.TokenAutenticacao;
import com.rafaelasoares.acesso.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenAutenticacaoRepository extends JpaRepository<TokenAutenticacao, UUID> {

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
    @Query("select t from TokenAutenticacao t join fetch t.usuario where t.tokenHash = :tokenHash")
    Optional<TokenAutenticacao> buscarPorTokenHashComUsuario(@Param("tokenHash") String tokenHash);

    Optional<TokenAutenticacao> findByTokenHash(String tokenHash);

    /** Sessões ainda não revogadas de um usuário — usado ao derrubar acessos. */
    List<TokenAutenticacao> findByUsuarioAndRevogadoEmIsNull(Usuario usuario);
}
