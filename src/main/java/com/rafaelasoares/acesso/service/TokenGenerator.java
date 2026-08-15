package com.rafaelasoares.acesso.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Gera e resume os tokens de sessão.
 *
 * <p>Duas decisões que valem explicação:
 *
 * <ul>
 *   <li><b>{@link SecureRandom}</b>, não {@code Random}: token de sessão
 *       precisa ser imprevisível. {@code Random} é previsível a partir de
 *       algumas saídas, o que permitiria forjar a sessão de outra pessoa.
 *   <li><b>SHA-256 para o hash</b>, não BCrypt: aqui o hash é <i>chave de
 *       busca</i> e precisa ser determinístico — BCrypt gera salt novo a cada
 *       chamada e não serviria para localizar a linha. O motivo de BCrypt
 *       existir (ser lento contra força bruta em senha humana) não se aplica:
 *       um token de 256 bits aleatórios não é adivinhável por força bruta.
 * </ul>
 */
@Component
public class TokenGenerator {

    /** 32 bytes = 256 bits de entropia. */
    private static final int TAMANHO_EM_BYTES = 32;

    private final SecureRandom aleatorio = new SecureRandom();

    /** Valor entregue ao cliente. Existe só na resposta do login. */
    public String gerar() {
        byte[] bytes = new byte[TAMANHO_EM_BYTES];
        aleatorio.nextBytes(bytes);
        // Sem padding e seguro para URL/header — evita '+' e '/'.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Resumo determinístico — é isto que vai para o banco. */
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resumo = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException erro) {
            // SHA-256 é obrigatório em toda JVM; se faltar, o ambiente está quebrado.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", erro);
        }
    }
}
