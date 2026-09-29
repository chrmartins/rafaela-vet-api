package com.vetplanet.modules.cliente.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Invariantes do animal, sem banco nem Spring.
 *
 * <p>O que se testa aqui é sobretudo a <b>situação</b>, porque ela carrega uma
 * decisão de produto que é fácil desfazer sem perceber: {@code INATIVO} quer
 * dizer "sumiu da lista", não "parou de aparecer", e o óbito precisa ter
 * desfazer.
 */
class AnimalEntityTest {

    private static TutorEntity umTutor() {
        return TutorEntity.criar("Ana Prado", "21999990000", "ana@exemplo.com", null, null);
    }

    private static AnimalEntity umAnimal() {
        return AnimalEntity.criar(
                umTutor(),
                "  Mel  ",
                EspecieAnimal.GATO,
                "   ",
                SexoAnimal.FEMEA,
                LocalDate.parse("2020-05-10"),
                true,
                null);
    }

    @Test
    @DisplayName("nasce em acompanhamento")
    void nasceAtivo() {
        assertThat(umAnimal().getSituacao()).isEqualTo(SituacaoAnimal.ATIVO);
    }

    @Test
    @DisplayName("apara o nome e transforma texto em branco em nulo")
    void normalizaTextos() {
        AnimalEntity animal = umAnimal();

        assertThat(animal.getNome()).isEqualTo("Mel");
        // Raça só com espaços vira nulo: "" e "não sei a raça" são coisas
        // diferentes, e a tela precisa distinguir para não escrever " · " solto.
        assertThat(animal.getRaca()).isNull();
        assertThat(animal.getObservacoes()).isNull();
    }

    @Test
    @DisplayName("castrado aceita os três estados — sim, não e não sei")
    void castradoTemTresEstados() {
        assertThat(umAnimal().getCastrado()).isTrue();

        AnimalEntity semSaber =
                AnimalEntity.criar(
                        umTutor(), "Thor", EspecieAnimal.CAO, null, SexoAnimal.MACHO, null,
                        null, null);

        // Nulo é "não se sabe", diferente de `false`, que é "não é castrado".
        assertThat(semSaber.getCastrado()).isNull();
    }

    @Test
    @DisplayName("registrar óbito não apaga nada, só muda a situação")
    void obitoPreservaOCadastro() {
        AnimalEntity animal = umAnimal();

        animal.registrarObito();

        assertThat(animal.getSituacao()).isEqualTo(SituacaoAnimal.OBITO);
        assertThat(animal.getNome()).isEqualTo("Mel");
        assertThat(animal.getDataNascimento()).isEqualTo(LocalDate.parse("2020-05-10"));
    }

    @Test
    @DisplayName("reativar desfaz óbito e inativação — o desfazer de um clique errado")
    void reativarDesfazOsDois() {
        AnimalEntity animal = umAnimal();

        animal.registrarObito();
        animal.reativar();
        assertThat(animal.getSituacao()).isEqualTo(SituacaoAnimal.ATIVO);

        animal.inativar();
        animal.reativar();
        assertThat(animal.getSituacao()).isEqualTo(SituacaoAnimal.ATIVO);
    }

    @Test
    @DisplayName("transferir troca o tutor sem mexer no resto")
    void transferirTrocaSoOTutor() {
        // Adoção, venda, o filho que assume o cachorro dos pais. O histórico
        // não se mexe: cada consulta guarda o tutor daquele atendimento.
        AnimalEntity animal = umAnimal();
        TutorEntity novo = TutorEntity.criar("Joaquim Lima", "21988887777", null, null, null);

        animal.transferirPara(novo);

        assertThat(animal.getTutor().getNomeCompleto()).isEqualTo("Joaquim Lima");
        assertThat(animal.getNome()).isEqualTo("Mel");
        assertThat(animal.getSituacao()).isEqualTo(SituacaoAnimal.ATIVO);
    }

    @Test
    @DisplayName("atualizarDados não mexe na situação")
    void atualizarNaoMexeNaSituacao() {
        // Editar e mudar de situação são coisas separadas, na tela como aqui:
        // registrar um óbito não pode acontecer por descuido no meio de uma
        // correção de raça.
        AnimalEntity animal = umAnimal();
        animal.inativar();

        animal.atualizarDados(
                "Mel", EspecieAnimal.GATO, "SRD", SexoAnimal.FEMEA, null, false, "arisca");

        assertThat(animal.getSituacao()).isEqualTo(SituacaoAnimal.INATIVO);
        assertThat(animal.getRaca()).isEqualTo("SRD");
    }

    @Test
    @DisplayName("os três estados de situação, e só eles")
    void situacoesDeclaradas() {
        assertThat(SituacaoAnimal.values())
                .containsExactly(
                        SituacaoAnimal.ATIVO, SituacaoAnimal.INATIVO, SituacaoAnimal.OBITO);
    }
}
