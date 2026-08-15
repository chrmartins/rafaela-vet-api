package com.rafaelasoares.acesso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Pessoa com acesso ao painel administrativo.
 *
 * <p>Não expõe setters: as mudanças de estado passam por métodos que dizem o
 * que está acontecendo ({@link #atualizarDados}, {@link #inativar}). Assim
 * fica difícil alterar um campo por engano e impossível zerar o hash da senha
 * sem querer.
 *
 * <p>O e-mail é sempre guardado em minúsculas — o índice único do banco é
 * sobre {@code lower(email)}, então normalizar aqui evita divergência entre
 * o que a aplicação acha que gravou e o que o banco considera duplicado.
 */
@Entity
@Table(name = "usuario", schema = "acesso")
public class Usuario {

    @Id
    @GeneratedValue
    @Column(name = "id_usuario", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "email", nullable = false)
    private String email;

    /** Hash BCrypt. Nunca a senha em texto puro. */
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil_acesso", nullable = false)
    private PerfilAcesso perfilAcesso;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    // Timestamps controlados pela própria entidade, e não por
    // @CreationTimestamp/@UpdateTimestamp: aquelas anotações só preenchem o
    // campo no flush (commit), então um DTO montado dentro do mesmo método
    // transacional enxergaria null. Fazendo aqui, o valor existe já no
    // construtor — e o comportamento fica testável sem banco.
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Exigido pelo JPA — não usar no código da aplicação. */
    protected Usuario() {}

    private Usuario(
            String nomeCompleto, String email, String senhaHash, PerfilAcesso perfilAcesso) {
        this.nomeCompleto = nomeCompleto.trim();
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.perfilAcesso = perfilAcesso;
        this.ativo = true;
        this.criadoEm = agora();
        this.atualizadoEm = this.criadoEm;
    }

    /** Sempre UTC — a exibição em America/Sao_Paulo é do frontend. */
    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void marcarAtualizacao() {
        this.atualizadoEm = agora();
    }

    /**
     * Cria um usuário ativo.
     *
     * @param senhaHash já codificada com BCrypt — este método não recebe nem
     *     conhece senha em texto puro.
     */
    public static Usuario criar(
            String nomeCompleto, String email, String senhaHash, PerfilAcesso perfilAcesso) {
        return new Usuario(nomeCompleto, email, senhaHash, perfilAcesso);
    }

    /** Atualiza os dados cadastrais. A senha tem caminho próprio. */
    public void atualizarDados(String nomeCompleto, String email, PerfilAcesso perfilAcesso) {
        this.nomeCompleto = nomeCompleto.trim();
        this.email = normalizarEmail(email);
        this.perfilAcesso = perfilAcesso;
        marcarAtualizacao();
    }

    public void trocarSenha(String novaSenhaHash) {
        this.senhaHash = novaSenhaHash;
        marcarAtualizacao();
    }

    /** Inativação lógica — usuário nunca é excluído fisicamente. */
    public void inativar() {
        this.ativo = false;
        marcarAtualizacao();
    }

    public void reativar() {
        this.ativo = true;
        marcarAtualizacao();
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    public UUID getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public PerfilAcesso getPerfilAcesso() {
        return perfilAcesso;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    /** Identidade pelo id — dois usuários só são o mesmo se têm o mesmo id. */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) return true;
        if (!(outro instanceof Usuario usuario)) return false;
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem senha nem hash no toString, para não vazar em log. */
    @Override
    public String toString() {
        return "Usuario{id=%s, email=%s, perfilAcesso=%s, ativo=%s}"
                .formatted(id, email, perfilAcesso, ativo);
    }
}
