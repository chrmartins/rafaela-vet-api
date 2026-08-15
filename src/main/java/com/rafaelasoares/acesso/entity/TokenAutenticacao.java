package com.rafaelasoares.acesso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Uma sessão ativa do painel.
 *
 * <p>Guarda o <b>hash</b> do token, nunca o token. O valor original existe só
 * uma vez, na resposta do login — nem o banco nem o log o conhecem.
 */
@Entity
@Table(name = "token_autenticacao", schema = "acesso")
public class TokenAutenticacao {

    @Id
    @GeneratedValue
    @Column(name = "id_token", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, updatable = false)
    private String tokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private OffsetDateTime expiraEm;

    @Column(name = "revogado_em")
    private OffsetDateTime revogadoEm;

    /** Exigido pelo JPA — não usar no código da aplicação. */
    protected TokenAutenticacao() {}

    private TokenAutenticacao(Usuario usuario, String tokenHash, Duration validade) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
        this.expiraEm = this.criadoEm.plus(validade);
    }

    public static TokenAutenticacao criar(Usuario usuario, String tokenHash, Duration validade) {
        return new TokenAutenticacao(usuario, tokenHash, validade);
    }

    /** Logout: a sessão para de autenticar imediatamente. */
    public void revogar() {
        if (revogadoEm == null) {
            this.revogadoEm = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    /**
     * Só autentica sessão que não expirou, não foi revogada e cujo dono ainda
     * está ativo — inativar um usuário derruba as sessões dele na hora.
     */
    public boolean estaValida() {
        return revogadoEm == null
                && expiraEm.isAfter(OffsetDateTime.now(ZoneOffset.UTC))
                && usuario.isAtivo();
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public OffsetDateTime getExpiraEm() {
        return expiraEm;
    }

    /** Sem o hash no toString, para não vazar em log. */
    @Override
    public String toString() {
        return "TokenAutenticacao{id=%s, idUsuario=%s, expiraEm=%s, revogado=%s}"
                .formatted(id, usuario == null ? null : usuario.getId(), expiraEm, revogadoEm != null);
    }
}
