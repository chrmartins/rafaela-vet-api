package com.vetplanet.modules.cliente.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Animal atendido. Pertence a exatamente um tutor.
 *
 * <p><b>Não guarda peso.</b> Peso muda a cada visita: é medição clínica e
 * pertence ao prontuário (o {@code padrao-nomenclatura.md} já prevê
 * {@code prontuario.historico_peso}). No cadastro ele nasceria desatualizado
 * e ninguém confiaria nele depois.
 *
 * <p>A relação com o tutor é {@code LAZY} de propósito: listar cem animais
 * não deve disparar cem consultas de tutor. Quem precisa do nome do tutor
 * junto pede explicitamente com {@code join fetch} — mesmo cuidado que já
 * existe em {@code TokenAutenticacaoRepository}.
 */
@Entity
@Table(name = "animal", schema = "cliente")
public class AnimalEntity {

    @Id
    @GeneratedValue
    @Column(name = "id_animal", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tutor", nullable = false)
    private TutorEntity tutor;

    @Column(name = "nome_animal", nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "especie", nullable = false)
    private EspecieAnimal especie;

    @Column(name = "raca")
    private String raca;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo_animal", nullable = false)
    private SexoAnimal sexo;

    /** Opcional: animal resgatado costuma não ter data conhecida. */
    @Column(name = "data_nascimento_animal")
    private LocalDate dataNascimento;

    /** {@code null} quando não se sabe — diferente de "não é castrado". */
    @Column(name = "castrado")
    private Boolean castrado;

    @Column(name = "observacoes")
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_animal", nullable = false)
    private SituacaoAnimal situacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Exigido pelo JPA — não usar no código da aplicação. */
    protected AnimalEntity() {}

    private AnimalEntity(
            TutorEntity tutor,
            String nome,
            EspecieAnimal especie,
            String raca,
            SexoAnimal sexo,
            LocalDate dataNascimento,
            Boolean castrado,
            String observacoes) {
        this.tutor = tutor;
        this.nome = nome.trim();
        this.especie = especie;
        this.raca = normalizarTexto(raca);
        this.sexo = sexo;
        this.dataNascimento = dataNascimento;
        this.castrado = castrado;
        this.observacoes = normalizarTexto(observacoes);
        this.situacao = SituacaoAnimal.ATIVO;
        this.criadoEm = agora();
        this.atualizadoEm = this.criadoEm;
    }

    public static AnimalEntity criar(
            TutorEntity tutor,
            String nome,
            EspecieAnimal especie,
            String raca,
            SexoAnimal sexo,
            LocalDate dataNascimento,
            Boolean castrado,
            String observacoes) {
        return new AnimalEntity(
                tutor, nome, especie, raca, sexo, dataNascimento, castrado, observacoes);
    }

    public void atualizarDados(
            String nome,
            EspecieAnimal especie,
            String raca,
            SexoAnimal sexo,
            LocalDate dataNascimento,
            Boolean castrado,
            String observacoes) {
        this.nome = nome.trim();
        this.especie = especie;
        this.raca = normalizarTexto(raca);
        this.sexo = sexo;
        this.dataNascimento = dataNascimento;
        this.castrado = castrado;
        this.observacoes = normalizarTexto(observacoes);
        marcarAtualizacao();
    }

    /**
     * Transfere o animal para outro tutor — adoção, venda, o filho que assume
     * o cachorro dos pais.
     *
     * <p>O histórico não se mexe: cada consulta guarda o tutor daquele
     * atendimento, então o passado continua dizendo quem era o responsável na
     * época.
     */
    public void transferirPara(TutorEntity novoTutor) {
        this.tutor = novoTutor;
        marcarAtualizacao();
    }

    /**
     * Registra o óbito.
     *
     * <p>Não apaga nada: o histórico de consultas e prontuários continua
     * inteiro. O que muda é como a interface fala do animal daqui em diante.
     */
    public void registrarObito() {
        this.situacao = SituacaoAnimal.OBITO;
        marcarAtualizacao();
    }

    /**
     * Some da lista.
     *
     * <p>É o substituto da exclusão para o animal que já tem histórico. Não
     * significa "parou de aparecer" — ver {@link SituacaoAnimal}.
     */
    public void inativar() {
        this.situacao = SituacaoAnimal.INATIVO;
        marcarAtualizacao();
    }

    /** Volta para a lista — também desfaz um óbito lançado por engano. */
    public void reativar() {
        this.situacao = SituacaoAnimal.ATIVO;
        marcarAtualizacao();
    }

    /** Sempre UTC — a exibição em America/Sao_Paulo é do frontend. */
    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void marcarAtualizacao() {
        this.atualizadoEm = agora();
    }

    private static String normalizarTexto(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    public UUID getId() {
        return id;
    }

    public TutorEntity getTutor() {
        return tutor;
    }

    public String getNome() {
        return nome;
    }

    public EspecieAnimal getEspecie() {
        return especie;
    }

    public String getRaca() {
        return raca;
    }

    public SexoAnimal getSexo() {
        return sexo;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public Boolean getCastrado() {
        return castrado;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public SituacaoAnimal getSituacao() {
        return situacao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) return true;
        if (!(outro instanceof AnimalEntity animal)) return false;
        return id != null && id.equals(animal.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem tocar em {@code tutor}: acessá-lo aqui dispararia o lazy loading. */
    @Override
    public String toString() {
        return "AnimalEntity{id=%s, nome=%s, especie=%s, situacao=%s}"
                .formatted(id, nome, especie, situacao);
    }
}
