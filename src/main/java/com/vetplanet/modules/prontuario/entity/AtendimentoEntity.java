package com.vetplanet.modules.prontuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * O registro clínico de uma consulta — uma entrada do prontuário.
 *
 * <p><b>Consulta e atendimento não são a mesma coisa</b>, apesar de serem um
 * para um. A consulta existe antes de acontecer, pode ser cancelada e pode ser
 * marcada por uma atendente. O atendimento só existe depois, não se cancela
 * porque é fato ocorrido, e só um veterinário com CRMV assina. É a relação
 * entre pedido e nota fiscal.
 *
 * <p><b>Nasce rascunho.</b> Nota clínica é escrita em pedaços, durante a
 * visita — por isso quase tudo aceita ficar vazio enquanto {@code concluidoEm}
 * for nulo. Ao concluir, o banco exige exame e diagnóstico presuntivo (ver
 * {@code V010}).
 *
 * <p><b>Concluído, é imutável.</b> Correção passa a ser retificação: registro
 * novo, com data, autor e motivo, sem tocar no original. Essa é regra nossa,
 * não do CFMV — a norma não tem conceito de fechar prontuário —, e existe
 * porque documento clínico que se reescreve em silêncio não vale como
 * documento.
 *
 * <p>Ver {@code docs/prontuario.md} e a migração {@code V009}, onde cada campo
 * cita o inciso do Art. 9º da Resolução CFMV nº 1.321/2020 que atende.
 */
@Entity
@Table(name = "atendimento", schema = "prontuario")
public class AtendimentoEntity {

    @Id
    @GeneratedValue
    @Column(name = "id_atendimento", nullable = false, updatable = false)
    private UUID id;

    /** A âncora: dela vêm data, hora e o animal. Um atendimento por consulta. */
    @Column(name = "id_consulta", nullable = false, updatable = false)
    private UUID idConsulta;

    @Column(name = "id_animal", nullable = false, updatable = false)
    private UUID idAnimal;

    /**
     * Quem assina. Nome e CRMV são <b>cópia</b>, não referência: se mudarem no
     * cadastro, o documento antigo continua dizendo o que valia na época.
     * Mesma razão do {@code idTutor} na consulta.
     */
    @Column(name = "id_veterinario", nullable = false, updatable = false)
    private UUID idVeterinario;

    @Column(name = "nome_veterinario", nullable = false, updatable = false)
    private String nomeVeterinario;

    @Column(name = "crmv_veterinario", nullable = false, updatable = false)
    private String crmvVeterinario;

    /** Art. 9º, I. Obrigatório aqui embora opcional na consulta. */
    @Column(name = "local_atendimento", nullable = false)
    private String localAtendimento;

    /** Art. 9º, III — o que o responsável relatou. */
    @Column(name = "anamnese")
    private String anamnese;

    /** Art. 9º, IV e V — estado geral e achados, num campo só. */
    @Column(name = "exame_clinico")
    private String exameClinico;

    /** Art. 9º, VI. */
    @Column(name = "diagnostico_presuntivo")
    private String diagnosticoPresuntivo;

    /** Art. 9º, VII — "quando houver" é do texto da norma. */
    @Column(name = "diagnostico_conclusivo")
    private String diagnosticoConclusivo;

    @Column(name = "recomendacoes")
    private String recomendacoes;

    /**
     * Medicamentos prescritos, em texto livre.
     *
     * <p>Separado de {@code recomendacoes} porque só a prescrição vira
     * receituário — orientação clínica e prescrição são documentos diferentes,
     * com exigências diferentes. Ver a migração {@code V012}.
     */
    @Column(name = "prescricao")
    private String prescricao;

    /** Art. 9º, IV — parâmetros MENSURADOS. Todos opcionais: a norma manda
     * registrar o que foi medido, não medir tudo. */
    @Column(name = "peso_kg")
    private BigDecimal pesoKg;

    @Column(name = "temperatura_c")
    private BigDecimal temperaturaC;

    @Column(name = "frequencia_cardiaca")
    private Integer frequenciaCardiaca;

    @Column(name = "frequencia_respiratoria")
    private Integer frequenciaRespiratoria;

    /** Nulo = rascunho. Preenchido = fechado, e a partir daí só retificação. */
    @Column(name = "concluido_em")
    private OffsetDateTime concluidoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Exigido pelo JPA — não usar no código da aplicação. */
    protected AtendimentoEntity() {}

    private AtendimentoEntity(
            UUID idConsulta,
            UUID idAnimal,
            UUID idVeterinario,
            String nomeVeterinario,
            String crmvVeterinario,
            String localAtendimento) {
        this.idConsulta = idConsulta;
        this.idAnimal = idAnimal;
        this.idVeterinario = idVeterinario;
        this.nomeVeterinario = nomeVeterinario.trim();
        this.crmvVeterinario = crmvVeterinario.trim();
        this.localAtendimento = localAtendimento.trim();
        this.criadoEm = agora();
        this.atualizadoEm = this.criadoEm;
    }

    /**
     * Abre o rascunho. Só isto — sem conteúdo clínico ainda.
     *
     * <p>É o que o botão "Iniciar atendimento" faz: cria o registro vazio para
     * ela começar a escrever. Nada de estado "em atendimento" na consulta, nem
     * hora de chegada: aqui não há recepção nem check-in.
     */
    public static AtendimentoEntity abrir(
            UUID idConsulta,
            UUID idAnimal,
            UUID idVeterinario,
            String nomeVeterinario,
            String crmvVeterinario,
            String localAtendimento) {
        return new AtendimentoEntity(
                idConsulta,
                idAnimal,
                idVeterinario,
                nomeVeterinario,
                crmvVeterinario,
                localAtendimento);
    }

    /** O conteúdo clínico, salvo quantas vezes for preciso enquanto é rascunho. */
    public void registrar(
            String localAtendimento,
            String anamnese,
            String exameClinico,
            String diagnosticoPresuntivo,
            String diagnosticoConclusivo,
            String recomendacoes,
            String prescricao,
            BigDecimal pesoKg,
            BigDecimal temperaturaC,
            Integer frequenciaCardiaca,
            Integer frequenciaRespiratoria) {
        this.localAtendimento = localAtendimento.trim();
        this.anamnese = normalizar(anamnese);
        this.exameClinico = normalizar(exameClinico);
        this.diagnosticoPresuntivo = normalizar(diagnosticoPresuntivo);
        this.diagnosticoConclusivo = normalizar(diagnosticoConclusivo);
        this.recomendacoes = normalizar(recomendacoes);
        this.prescricao = normalizar(prescricao);
        this.pesoKg = pesoKg;
        this.temperaturaC = temperaturaC;
        this.frequenciaCardiaca = frequenciaCardiaca;
        this.frequenciaRespiratoria = frequenciaRespiratoria;
        marcarAtualizacao();
    }

    /** Fecha. Ponto sem volta: daqui em diante, só retificação. */
    public void concluir() {
        this.concluidoEm = agora();
        marcarAtualizacao();
    }

    public boolean estaConcluido() {
        return concluidoEm != null;
    }

    /** Tem o mínimo que o banco exige para fechar? Ver {@code V010}. */
    public boolean podeSerConcluido() {
        return preenchido(exameClinico) && preenchido(diagnosticoPresuntivo);
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String normalizar(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /** Sempre UTC — a exibição em America/Sao_Paulo é do frontend. */
    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void marcarAtualizacao() {
        this.atualizadoEm = agora();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdConsulta() {
        return idConsulta;
    }

    public UUID getIdAnimal() {
        return idAnimal;
    }

    public UUID getIdVeterinario() {
        return idVeterinario;
    }

    public String getNomeVeterinario() {
        return nomeVeterinario;
    }

    public String getCrmvVeterinario() {
        return crmvVeterinario;
    }

    public String getLocalAtendimento() {
        return localAtendimento;
    }

    public String getAnamnese() {
        return anamnese;
    }

    public String getExameClinico() {
        return exameClinico;
    }

    public String getDiagnosticoPresuntivo() {
        return diagnosticoPresuntivo;
    }

    public String getDiagnosticoConclusivo() {
        return diagnosticoConclusivo;
    }

    public String getRecomendacoes() {
        return recomendacoes;
    }

    public String getPrescricao() {
        return prescricao;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public BigDecimal getTemperaturaC() {
        return temperaturaC;
    }

    public Integer getFrequenciaCardiaca() {
        return frequenciaCardiaca;
    }

    public Integer getFrequenciaRespiratoria() {
        return frequenciaRespiratoria;
    }

    public OffsetDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
