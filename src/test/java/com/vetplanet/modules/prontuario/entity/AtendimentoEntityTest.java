package com.vetplanet.modules.prontuario.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Invariantes do registro clínico, sem banco nem Spring.
 *
 * <p>O que se testa aqui é sobretudo o par rascunho/concluído, porque é dele
 * que depende o valor do documento: rascunho aceita estar pela metade,
 * concluído exige o mínimo da Res. CFMV 1.321/2020.
 */
class AtendimentoEntityTest {

    private static AtendimentoEntity umRascunho() {
        return AtendimentoEntity.abrir(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "  Rafaela Soares  ",
                "  CRMV-RJ 12345  ",
                "  Rua das Acácias, 210  ");
    }

    private static void preencherOMinimo(AtendimentoEntity atendimento) {
        atendimento.registrar(
                "Rua das Acácias, 210",
                "Tutora relata apetite reduzido.",
                "Estado geral bom, tártaro em molares.",
                "Doença periodontal grau 2",
                null,
                "Profilaxia dentária.",
                "Dipirona 500mg, 1 comprimido de 12/12h por 3 dias.",
                new BigDecimal("4.200"),
                new BigDecimal("38.6"),
                null,
                null);
    }

    @Test
    @DisplayName("nasce rascunho e vazio — o botão só abre a tela")
    void nasceRascunho() {
        AtendimentoEntity atendimento = umRascunho();

        assertThat(atendimento.estaConcluido()).isFalse();
        assertThat(atendimento.getConcluidoEm()).isNull();
        assertThat(atendimento.getAnamnese()).isNull();
        assertThat(atendimento.getDiagnosticoPresuntivo()).isNull();
    }

    @Test
    @DisplayName("apara nome, CRMV e local ao abrir")
    void aparaOsTextos() {
        AtendimentoEntity atendimento = umRascunho();

        assertThat(atendimento.getNomeVeterinario()).isEqualTo("Rafaela Soares");
        assertThat(atendimento.getCrmvVeterinario()).isEqualTo("CRMV-RJ 12345");
        assertThat(atendimento.getLocalAtendimento()).isEqualTo("Rua das Acácias, 210");
    }

    @Test
    @DisplayName("rascunho pela metade é válido — nota clínica se escreve em pedaços")
    void rascunhoAceitaEstarIncompleto() {
        AtendimentoEntity atendimento = umRascunho();

        atendimento.registrar(
                "Rua das Acácias, 210",
                "Só o que a tutora falou, por enquanto.",
                null, null, null, null, null, null, null, null, null);

        assertThat(atendimento.getAnamnese()).isEqualTo("Só o que a tutora falou, por enquanto.");
        assertThat(atendimento.podeSerConcluido()).isFalse();
    }

    @Test
    @DisplayName("texto em branco vira nulo — vazio num documento legal parece omissão")
    void brancoViraNulo() {
        AtendimentoEntity atendimento = umRascunho();

        atendimento.registrar(
                "Rua das Acácias, 210", "   ", "", null, null, "  ", "  ", null, null, null, null);

        assertThat(atendimento.getAnamnese()).isNull();
        assertThat(atendimento.getExameClinico()).isNull();
        assertThat(atendimento.getRecomendacoes()).isNull();
    }

    @Test
    @DisplayName("só pode concluir com exame e diagnóstico presuntivo")
    void exigeOMinimoParaConcluir() {
        AtendimentoEntity atendimento = umRascunho();
        assertThat(atendimento.podeSerConcluido()).isFalse();

        // Só o exame não basta: o diagnóstico presuntivo é inciso VI, sem
        // "quando houver" — diferente do conclusivo, que é VII.
        atendimento.registrar(
                "Rua das Acácias, 210", null, "Tártaro em molares.", null, null, null,
                null, null, null, null, null);
        assertThat(atendimento.podeSerConcluido()).isFalse();

        preencherOMinimo(atendimento);
        assertThat(atendimento.podeSerConcluido()).isTrue();
    }

    @Test
    @DisplayName("concluir marca a hora e fecha")
    void concluirFecha() {
        AtendimentoEntity atendimento = umRascunho();
        preencherOMinimo(atendimento);

        atendimento.concluir();

        assertThat(atendimento.estaConcluido()).isTrue();
        assertThat(atendimento.getConcluidoEm()).isNotNull();
    }

    @Test
    @DisplayName("guarda nome e CRMV como cópia — o documento diz o que valia na época")
    void guardaAAssinaturaDaEpoca() {
        // Se o CRMV mudar no cadastro, o atendimento antigo continua dizendo o
        // número de quando foi assinado. Mesma razão do id_tutor na consulta.
        AtendimentoEntity atendimento = umRascunho();
        preencherOMinimo(atendimento);
        atendimento.concluir();

        assertThat(atendimento.getCrmvVeterinario()).isEqualTo("CRMV-RJ 12345");
        assertThat(atendimento.getNomeVeterinario()).isEqualTo("Rafaela Soares");
    }

    @Test
    @DisplayName("registrar não mexe na consulta nem no animal")
    void registrarNaoTrocaDeDono() {
        AtendimentoEntity atendimento = umRascunho();
        UUID consulta = atendimento.getIdConsulta();
        UUID animal = atendimento.getIdAnimal();

        preencherOMinimo(atendimento);

        assertThat(atendimento.getIdConsulta()).isEqualTo(consulta);
        assertThat(atendimento.getIdAnimal()).isEqualTo(animal);
    }
}
