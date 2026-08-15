package com.rafaelasoares.acesso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a {@code acesso.usuario}.
 *
 * <p>Os nomes dos métodos seguem a convenção do Spring Data (inglês), porque
 * é a própria biblioteca que os interpreta para gerar a consulta.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Busca por e-mail ignorando maiúsculas — espelha o índice único do banco,
     * que é sobre {@code lower(email)}.
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Usado ao trocar o e-mail: ignora o próprio usuário na checagem. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    List<Usuario> findAllByOrderByNomeCompletoAsc();
}
