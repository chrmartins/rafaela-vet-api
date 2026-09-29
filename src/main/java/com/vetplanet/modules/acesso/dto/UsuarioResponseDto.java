package com.vetplanet.modules.acesso.dto;

import com.vetplanet.modules.acesso.entity.PerfilAcesso;
import com.vetplanet.modules.acesso.entity.UsuarioEntity;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Usuário como sai da API.
 *
 * <p><b>Nunca inclua {@code senhaHash} aqui.</b> Este record existe
 * justamente para que a entidade não seja serializada direto — assim é
 * impossível o hash vazar por descuido numa resposta.
 */
public record UsuarioResponseDto(
        UUID idUsuario,
        String nomeCompleto,
        String email,
        PerfilAcesso perfilAcesso,
        /** Nulo para quem não é veterinário. */
        String crmv,
        /** O rodapé do receituário. Todos opcionais — ver a migração V013. */
        String telefoneContato,
        String instagram,
        String site,
        String cidadeAtuacao,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    public static UsuarioResponseDto de(UsuarioEntity usuario) {
        return new UsuarioResponseDto(
                usuario.getId(),
                usuario.getNomeCompleto(),
                usuario.getEmail(),
                usuario.getPerfilAcesso(),
                usuario.getCrmv(),
                usuario.getTelefoneContato(),
                usuario.getInstagram(),
                usuario.getSite(),
                usuario.getCidadeAtuacao(),
                usuario.isAtivo(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm());
    }
}
