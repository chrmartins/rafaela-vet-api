package com.rafaelasoares.acesso.dto;

import com.rafaelasoares.acesso.PerfilAcesso;
import com.rafaelasoares.acesso.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Usuário como sai da API.
 *
 * <p><b>Nunca inclua {@code senhaHash} aqui.</b> Este record existe
 * justamente para que a entidade não seja serializada direto — assim é
 * impossível o hash vazar por descuido numa resposta.
 */
public record UsuarioResponse(
        UUID idUsuario,
        String nomeCompleto,
        String email,
        PerfilAcesso perfilAcesso,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNomeCompleto(),
                usuario.getEmail(),
                usuario.getPerfilAcesso(),
                usuario.isAtivo(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm());
    }
}
