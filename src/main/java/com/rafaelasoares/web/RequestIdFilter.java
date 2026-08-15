package com.rafaelasoares.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Dá um identificador a cada requisição, para ligar o erro que o usuário viu
 * à linha exata do log.
 *
 * <p>O id entra no {@link MDC}, então <b>toda</b> linha logada durante a
 * requisição sai marcada com ele (ver {@code logging.pattern.level} no
 * application.yml). Também volta no header {@code X-Request-Id} e no corpo
 * das respostas de erro. Assim, quando a Dra. Rafaela disser "deu erro", o id
 * da tela leva direto ao rastro completo no log.
 *
 * <p>Se o chamador já mandar {@code X-Request-Id}, esse valor é reaproveitado
 * — é o que permite correlacionar frontend e backend numa mesma requisição.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String CHAVE_MDC = "requestId";

    /** Limite defensivo: header vem de fora, não pode inflar o log. */
    private static final int TAMANHO_MAXIMO = 64;

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {

        String requestId = resolverRequestId(requisicao);
        MDC.put(CHAVE_MDC, requestId);
        resposta.setHeader(HEADER, requestId);

        try {
            cadeia.doFilter(requisicao, resposta);
        } finally {
            // Sem isto o id vaza para a próxima requisição atendida pela
            // mesma thread do pool.
            MDC.remove(CHAVE_MDC);
        }
    }

    private String resolverRequestId(HttpServletRequest requisicao) {
        String recebido = requisicao.getHeader(HEADER);
        if (recebido == null || recebido.isBlank()) {
            return UUID.randomUUID().toString();
        }
        // Só caracteres seguros, para não permitir injeção de quebra de linha
        // no log (log forging).
        String limpo = recebido.replaceAll("[^A-Za-z0-9\\-_]", "");
        if (limpo.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return limpo.length() > TAMANHO_MAXIMO ? limpo.substring(0, TAMANHO_MAXIMO) : limpo;
    }
}
