package com.vetplanet.modules.acesso.entity;

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
public class UsuarioEntity {

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

    /**
     * Número de inscrição no CRMV.
     *
     * <p><b>Obrigatório para {@code VETERINARIO}, nulo nos demais.</b> A
     * Resolução CFMV nº 1.321/2020, Art. 9º, II e VIII, exige que o prontuário
     * identifique o profissional por nome completo e número de CRMV — sem isto
     * não há como registrar atendimento em conformidade.
     *
     * <p>Nulo para administrador e atendente porque <b>perfil não é
     * profissão</b>: preencher para eles seria dizer que assinam prontuário, e
     * não assinam.
     */
    @Column(name = "crmv")
    private String crmv;

    /**
     * Contato do rodapé do receituário.
     *
     * <p>São dados de vitrine, todos opcionais: quem não tem Instagram não
     * deve ser impedido de emitir receita, e receituário sem telefone continua
     * válido — o que a norma exige, nome e CRMV, está acima.
     */
    @Column(name = "telefone_contato")
    private String telefoneContato;

    @Column(name = "instagram")
    private String instagram;

    @Column(name = "site")
    private String site;

    @Column(name = "cidade_atuacao")
    private String cidadeAtuacao;

    /** Preenchido pelo próprio profissional, na tela de dados dele. */
    public void atualizarDadosDoReceituario(
            String telefoneContato, String instagram, String site, String cidadeAtuacao) {
        this.telefoneContato = emBrancoViraNulo(telefoneContato);
        // O @ é enfeite de exibição, não parte do identificador. Guardar sem
        // ele evita "@@rafaela" quando a tela acrescenta o próprio.
        this.instagram = removerArroba(emBrancoViraNulo(instagram));
        this.site = emBrancoViraNulo(site);
        this.cidadeAtuacao = emBrancoViraNulo(cidadeAtuacao);
        marcarAtualizacao();
    }

    private static String emBrancoViraNulo(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private static String removerArroba(String valor) {
        return valor == null ? null : valor.replaceFirst("^@+", "");
    }

    public String getTelefoneContato() {
        return telefoneContato;
    }

    public String getInstagram() {
        return instagram;
    }

    public String getSite() {
        return site;
    }

    public String getCidadeAtuacao() {
        return cidadeAtuacao;
    }

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
    protected UsuarioEntity() {}

    private UsuarioEntity(
            String nomeCompleto,
            String email,
            String senhaHash,
            PerfilAcesso perfilAcesso,
            String crmv) {
        this.nomeCompleto = nomeCompleto.trim();
        this.email = normalizarEmail(email);
        this.senhaHash = senhaHash;
        this.perfilAcesso = perfilAcesso;
        this.crmv = crmvDoPerfil(perfilAcesso, crmv);
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
    public static UsuarioEntity criar(
            String nomeCompleto,
            String email,
            String senhaHash,
            PerfilAcesso perfilAcesso,
            String crmv) {
        return new UsuarioEntity(nomeCompleto, email, senhaHash, perfilAcesso, crmv);
    }

    /** Atualiza os dados cadastrais. A senha tem caminho próprio. */
    public void atualizarDados(
            String nomeCompleto, String email, PerfilAcesso perfilAcesso, String crmv) {
        this.nomeCompleto = nomeCompleto.trim();
        this.email = normalizarEmail(email);
        this.perfilAcesso = perfilAcesso;
        this.crmv = crmvDoPerfil(perfilAcesso, crmv);
        marcarAtualizacao();
    }

    /**
     * O CRMV que este perfil pode ter.
     *
     * <p><b>Limpa o número ao deixar de ser veterinário.</b> Sem isto, rebaixar
     * um veterinário para atendente deixaria o CRMV para trás, e a linha
     * violaria o check do banco — o erro apareceria como 500 no meio de uma
     * edição comum, em vez de o dado simplesmente ficar certo.
     *
     * <p>Não valida a presença: quem exige CRMV de veterinário é o Bean
     * Validation no DTO, onde a mensagem chega ao lado do campo.
     */
    private static String crmvDoPerfil(PerfilAcesso perfil, String crmv) {
        if (perfil != PerfilAcesso.VETERINARIO) return null;
        return crmv == null || crmv.isBlank() ? null : crmv.trim();
    }

    /** Assina prontuário? Só veterinário com CRMV — Res. CFMV 1.321/2020. */
    public boolean podeAssinarProntuario() {
        return perfilAcesso == PerfilAcesso.VETERINARIO && crmv != null && !crmv.isBlank();
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

    public String getCrmv() {
        return crmv;
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
        if (!(outro instanceof UsuarioEntity usuario)) return false;
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem senha nem hash no toString, para não vazar em log. */
    @Override
    public String toString() {
        return "UsuarioEntity{id=%s, email=%s, perfilAcesso=%s, ativo=%s}"
                .formatted(id, email, perfilAcesso, ativo);
    }
}
