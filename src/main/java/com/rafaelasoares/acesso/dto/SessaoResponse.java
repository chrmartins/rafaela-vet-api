package com.rafaelasoares.acesso.dto;

import java.time.OffsetDateTime;

/**
 * Resposta do login.
 *
 * <p>O {@code token} aparece <b>uma única vez</b>, aqui — o banco guarda só o
 * hash. Quem consome é o servidor do Next, que o coloca num cookie httpOnly;
 * o navegador nunca vê este valor em JavaScript.
 */
public record SessaoResponse(String token, OffsetDateTime expiraEm, UsuarioResponse usuario) {}
