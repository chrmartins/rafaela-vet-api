package com.vetplanet.modules.cliente.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Endereço do tutor — colunas de {@code cliente.tutor}, não tabela própria.
 *
 * <p><b>Desvio deliberado</b> do {@code padrao-nomenclatura.md}, que previa
 * uma tabela {@code endereco_tutor}. Está inline porque no MVP cada tutor tem
 * um endereço, e porque o histórico não depende dele: a consulta guarda o
 * próprio {@code endereco_atendimento} em texto (ver o exemplo do §3 do
 * padrão), então isto aqui é só o valor padrão de novas consultas, não o
 * registro do que aconteceu. Tabela separada para uma linha garantida é join
 * sem contrapartida. Vira tabela no dia em que um tutor precisar de um
 * segundo endereço.
 *
 * <p>É {@code @Embeddable} para o endereço andar junto como uma coisa só no
 * código, sem espalhar sete campos soltos pela entidade.
 *
 * <p>Todos os campos são opcionais. Parece estranho para um atendimento
 * domiciliar, mas o cadastro costuma começar por telefone — o endereço chega
 * depois, e exigi-lo aqui empurraria a pessoa a inventar dado para conseguir
 * salvar.
 */
@Embeddable
public record EnderecoTutor(
        @Column(name = "cep") String cep,
        @Column(name = "logradouro") String logradouro,
        @Column(name = "numero") String numero,
        @Column(name = "complemento") String complemento,
        @Column(name = "bairro") String bairro,
        @Column(name = "cidade") String cidade,
        /** Sigla de dois caracteres, em maiúsculas — o banco tem check para isso. */
        @Column(name = "uf") String uf) {

    public EnderecoTutor {
        cep = normalizar(cep);
        logradouro = normalizar(logradouro);
        numero = normalizar(numero);
        complemento = normalizar(complemento);
        bairro = normalizar(bairro);
        cidade = normalizar(cidade);
        uf = uf == null || uf.isBlank() ? null : uf.trim().toUpperCase();
    }

    public static EnderecoTutor vazio() {
        return new EnderecoTutor(null, null, null, null, null, null, null);
    }

    /** Vazio quer dizer "ainda não informado", não "sem endereço". */
    public boolean estaVazio() {
        return cep == null
                && logradouro == null
                && numero == null
                && complemento == null
                && bairro == null
                && cidade == null
                && uf == null;
    }

    private static String normalizar(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
