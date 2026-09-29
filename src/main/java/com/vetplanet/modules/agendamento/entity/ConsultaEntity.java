package com.vetplanet.modules.agendamento.entity;

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
 * Um atendimento marcado.
 *
 * <p><b>Guarda os ids de tutor e animal como UUID puro, sem
 * {@code @ManyToOne}.</b> Mapear as entidades de `cliente` aqui daria a
 * `agendamento` uma porta para navegar e alterar dados do vizinho, que é
 * exatamente o que a regra de fronteira proíbe. Nome de animal e de tutor vêm
 * do application service de `cliente` ({@code ResumirAnimaisService}).
 *
 * <p>A chave estrangeira existe no banco mesmo assim — referência declarativa
 * não é acesso, e é ela que impede apagar um animal com histórico.
 *
 * <p><b>Guarda o tutor além do animal</b> porque tutor muda: adoção, venda, o
 * filho que assume o cachorro dos pais. Sem isso, o histórico de dois anos
 * atrás passaria a exibir o dono de hoje.
 */
@Entity
@Table(name = "consulta", schema = "agendamento")
public class ConsultaEntity {

    @Id
    @GeneratedValue
    @Column(name = "id_consulta", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_tutor", nullable = false)
    private UUID idTutor;

    @Column(name = "id_animal", nullable = false)
    private UUID idAnimal;

    /** Sempre UTC. A exibição em America/Sao_Paulo é do frontend. */
    @Column(name = "data_hora_consulta", nullable = false)
    private OffsetDateTime dataHora;

    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    @Column(name = "motivo_consulta", nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_consulta", nullable = false)
    private StatusConsulta status;

    /** Atributo, não status — convive com solicitada e confirmada. */
    @Column(name = "urgente", nullable = false)
    private boolean urgente;

    /** Nulo enquanto não se sabe para onde ir. */
    @Column(name = "endereco_atendimento")
    private String enderecoAtendimento;

    @Column(name = "observacoes")
    private String observacoes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    /** Exigido pelo JPA — não usar no código da aplicação. */
    protected ConsultaEntity() {}

    private ConsultaEntity(
            UUID idTutor,
            UUID idAnimal,
            OffsetDateTime dataHora,
            int duracaoMinutos,
            String motivo,
            StatusConsulta status,
            boolean urgente,
            String enderecoAtendimento,
            String observacoes) {
        this.idTutor = idTutor;
        this.idAnimal = idAnimal;
        this.dataHora = dataHora;
        this.duracaoMinutos = duracaoMinutos;
        this.motivo = motivo.trim();
        this.status = status;
        this.urgente = urgente;
        this.enderecoAtendimento = normalizar(enderecoAtendimento);
        this.observacoes = normalizar(observacoes);
        this.criadoEm = agora();
        this.atualizadoEm = this.criadoEm;
    }

    public static ConsultaEntity criar(
            UUID idTutor,
            UUID idAnimal,
            OffsetDateTime dataHora,
            int duracaoMinutos,
            String motivo,
            StatusConsulta status,
            boolean urgente,
            String enderecoAtendimento,
            String observacoes) {
        return new ConsultaEntity(
                idTutor,
                idAnimal,
                dataHora,
                duracaoMinutos,
                motivo,
                status,
                urgente,
                enderecoAtendimento,
                observacoes);
    }

    public void reagendar(OffsetDateTime novaDataHora, int novaDuracao) {
        this.dataHora = novaDataHora;
        this.duracaoMinutos = novaDuracao;
        marcarAtualizacao();
    }

    public void atualizarDados(
            String motivo, boolean urgente, String enderecoAtendimento, String observacoes) {
        this.motivo = motivo.trim();
        this.urgente = urgente;
        this.enderecoAtendimento = normalizar(enderecoAtendimento);
        this.observacoes = normalizar(observacoes);
        marcarAtualizacao();
    }

    /**
     * Muda o status. Não há exclusão: cancelar é um status, não um delete.
     *
     * <p>Sem máquina de estados por enquanto — a veterinária é a única
     * operadora e corrigir um clique errado precisa ser trivial. Se um dia
     * houver mais gente mexendo, aí vale impedir transição sem sentido.
     */
    public void alterarStatus(StatusConsulta novoStatus) {
        this.status = novoStatus;
        marcarAtualizacao();
    }

    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void marcarAtualizacao() {
        this.atualizadoEm = agora();
    }

    private static String normalizar(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdTutor() {
        return idTutor;
    }

    public UUID getIdAnimal() {
        return idAnimal;
    }

    public OffsetDateTime getDataHora() {
        return dataHora;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public String getMotivo() {
        return motivo;
    }

    public StatusConsulta getStatus() {
        return status;
    }

    public boolean isUrgente() {
        return urgente;
    }

    public String getEnderecoAtendimento() {
        return enderecoAtendimento;
    }

    public String getObservacoes() {
        return observacoes;
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
        if (!(outro instanceof ConsultaEntity consulta)) return false;
        return id != null && id.equals(consulta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem endereço nem observações: são dados pessoais e o toString cai em log. */
    @Override
    public String toString() {
        return "ConsultaEntity{id=%s, dataHora=%s, status=%s}".formatted(id, dataHora, status);
    }
}
