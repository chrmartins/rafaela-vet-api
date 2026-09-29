package com.vetplanet.modules.prontuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Uma alteração num campo do registro clínico.
 *
 * <p><b>É o que substituiu a imutabilidade.</b> O prontuário é sempre
 * editável — a veterinária lembra do nódulo depois de sair da casa, e travar
 * isso empobreceria o documento em vez de protegê-lo. O que dá valor ao
 * registro não é a impossibilidade de mudar; é o rastro de quem mudou o quê.
 *
 * <p>Uma linha por campo, gravada sozinha. Ela não escreve motivo nem preenche
 * formulário de correção: só edita, e o sistema anota.
 */
@Entity
@Table(name = "alteracao_atendimento", schema = "prontuario")
public class AlteracaoAtendimentoEntity {

    @Id
    @GeneratedValue
    @Column(name = "id_alteracao", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_atendimento", nullable = false, updatable = false)
    private UUID idAtendimento;

    /** Nome e CRMV copiados: o rastro diz o que valia na época. */
    @Column(name = "id_veterinario", nullable = false, updatable = false)
    private UUID idVeterinario;

    @Column(name = "nome_veterinario", nullable = false, updatable = false)
    private String nomeVeterinario;

    @Column(name = "crmv_veterinario", nullable = false, updatable = false)
    private String crmvVeterinario;

    /** Nome do campo como na entidade — {@code anamnese}, {@code pesoKg}… */
    @Column(name = "campo", nullable = false, updatable = false)
    private String campo;

    @Column(name = "valor_anterior", updatable = false)
    private String valorAnterior;

    @Column(name = "valor_novo", updatable = false)
    private String valorNovo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    /** Exigido pelo JPA — não usar no código da aplicação. */
    protected AlteracaoAtendimentoEntity() {}

    private AlteracaoAtendimentoEntity(
            UUID idAtendimento,
            UUID idVeterinario,
            String nomeVeterinario,
            String crmvVeterinario,
            String campo,
            String valorAnterior,
            String valorNovo) {
        this.idAtendimento = idAtendimento;
        this.idVeterinario = idVeterinario;
        this.nomeVeterinario = nomeVeterinario;
        this.crmvVeterinario = crmvVeterinario;
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
        this.criadoEm = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static AlteracaoAtendimentoEntity registrar(
            UUID idAtendimento,
            UUID idVeterinario,
            String nomeVeterinario,
            String crmvVeterinario,
            String campo,
            String valorAnterior,
            String valorNovo) {
        return new AlteracaoAtendimentoEntity(
                idAtendimento,
                idVeterinario,
                nomeVeterinario,
                crmvVeterinario,
                campo,
                valorAnterior,
                valorNovo);
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdAtendimento() {
        return idAtendimento;
    }

    public String getNomeVeterinario() {
        return nomeVeterinario;
    }

    public String getCrmvVeterinario() {
        return crmvVeterinario;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
