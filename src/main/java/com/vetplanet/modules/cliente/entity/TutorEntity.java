package com.vetplanet.modules.cliente.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Pessoa responsável por um ou mais animais.
 *
 * <p>Segue o mesmo desenho de {@code UsuarioEntity}: sem setters públicos, as
 * mudanças passam por métodos que dizem o que está acontecendo, e os
 * timestamps são da própria entidade (e não de {@code @CreationTimestamp},
 * que só preenche no flush e deixaria um DTO montado na mesma transação com
 * campo nulo).
 *
 * <p><b>Não guarda CPF.</b> É o campo que todo cadastro brasileiro tem por
 * reflexo, e hoje não teria uso: faturamento é pós-MVP. Guardar documento sem
 * finalidade é coleta excessiva sob a LGPD — risco sem contrapartida. Entra
 * quando o faturamento existir.
 *
 * <p>Não expõe a lista de animais. O acesso é pelo {@code AnimalRepository},
 * com o id do tutor: mapear {@code @OneToMany} aqui convida a carregar a
 * coleção inteira em toda leitura de tutor, inclusive na listagem.
 */
@Entity
@Table(name = "tutor", schema = "cliente")
public class TutorEntity {

    @Id
    @GeneratedValue
    @Column(name = "id_tutor", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    /** Obrigatório: todo o contato do projeto passa por WhatsApp. */
    @Column(name = "telefone_whatsapp", nullable = false)
    private String telefoneWhatsapp;

    @Column(name = "email")
    private String email;

    @Embedded private EnderecoTutor endereco;

    @Column(name = "observacoes")
    private String observacoes;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Exigido pelo JPA — não usar no código da aplicação. */
    protected TutorEntity() {}

    private TutorEntity(
            String nomeCompleto,
            String telefoneWhatsapp,
            String email,
            EnderecoTutor endereco,
            String observacoes) {
        this.nomeCompleto = nomeCompleto.trim();
        this.telefoneWhatsapp = telefoneWhatsapp.trim();
        this.email = normalizarEmail(email);
        this.endereco = endereco == null ? EnderecoTutor.vazio() : endereco;
        this.observacoes = normalizarTexto(observacoes);
        this.ativo = true;
        this.criadoEm = agora();
        this.atualizadoEm = this.criadoEm;
    }

    public static TutorEntity criar(
            String nomeCompleto,
            String telefoneWhatsapp,
            String email,
            EnderecoTutor endereco,
            String observacoes) {
        return new TutorEntity(nomeCompleto, telefoneWhatsapp, email, endereco, observacoes);
    }

    public void atualizarDados(
            String nomeCompleto,
            String telefoneWhatsapp,
            String email,
            EnderecoTutor endereco,
            String observacoes) {
        this.nomeCompleto = nomeCompleto.trim();
        this.telefoneWhatsapp = telefoneWhatsapp.trim();
        this.email = normalizarEmail(email);
        this.endereco = endereco == null ? EnderecoTutor.vazio() : endereco;
        this.observacoes = normalizarTexto(observacoes);
        marcarAtualizacao();
    }

    /** Inativação lógica — tutor nunca é excluído fisicamente. */
    public void inativar() {
        this.ativo = false;
        marcarAtualizacao();
    }

    public void reativar() {
        this.ativo = true;
        marcarAtualizacao();
    }

    /** Sempre UTC — a exibição em America/Sao_Paulo é do frontend. */
    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void marcarAtualizacao() {
        this.atualizadoEm = agora();
    }

    /** Campo opcional vazio vira null, para não gravar string em branco. */
    private static String normalizarTexto(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private static String normalizarEmail(String email) {
        String limpo = normalizarTexto(email);
        return limpo == null ? null : limpo.toLowerCase();
    }

    public UUID getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getTelefoneWhatsapp() {
        return telefoneWhatsapp;
    }

    public String getEmail() {
        return email;
    }

    public EnderecoTutor getEndereco() {
        return endereco;
    }

    public String getObservacoes() {
        return observacoes;
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

    /** Identidade pelo id — dois tutores só são o mesmo se têm o mesmo id. */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) return true;
        if (!(outro instanceof TutorEntity tutor)) return false;
        return id != null && id.equals(tutor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Sem telefone, e-mail nem endereço: são dados pessoais e o toString cai
     * em log (ver LGPD no CLAUDE.md).
     */
    @Override
    public String toString() {
        return "TutorEntity{id=%s, ativo=%s}".formatted(id, ativo);
    }
}
