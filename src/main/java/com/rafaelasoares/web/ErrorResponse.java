package com.rafaelasoares.web;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.MDC;

/**
 * Formato único de erro da API.
 *
 * <p>As mensagens são em português porque chegam ao usuário final pela tela
 * do painel.
 *
 * <p>O {@code requestId} é o mesmo que marca as linhas de log da requisição
 * (ver {@link RequestIdFilter}) — é o que permite pegar o id que apareceu na
 * tela e achar o rastro completo no log.
 */
public record ErrorResponse(
        String requestId,
        OffsetDateTime ocorridoEm,
        int status,
        String mensagem,
        /** Preenchido só quando há erro de validação campo a campo. */
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {}

    public static ErrorResponse de(int status, String mensagem) {
        return new ErrorResponse(requestIdAtual(), agora(), status, mensagem, List.of());
    }

    public static ErrorResponse deValidacao(
            int status, String mensagem, List<CampoInvalido> campos) {
        return new ErrorResponse(requestIdAtual(), agora(), status, mensagem, campos);
    }

    /**
     * Nome diferente do componente {@code requestId} de propósito: num record,
     * {@code requestId()} já é o acessor gerado e não pode ser redeclarado.
     */
    private static String requestIdAtual() {
        return MDC.get(RequestIdFilter.CHAVE_MDC);
    }

    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
