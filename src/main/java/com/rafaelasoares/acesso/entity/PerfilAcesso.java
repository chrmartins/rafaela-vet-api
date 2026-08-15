package com.rafaelasoares.acesso.entity;

/**
 * Perfis de acesso ao painel administrativo.
 *
 * <p>Fixos de propósito: as permissões são conferidas em código, então um
 * perfil novo exigiria deploy de qualquer forma. Por isso é enum aqui e
 * {@code check} constraint no banco, em vez de tabela `perfil_acesso`.
 */
public enum PerfilAcesso {

    /** Acesso total, incluindo gerenciar usuários. */
    ADMINISTRADOR,

    /** Atende, registra prontuário e gerencia a própria agenda. */
    VETERINARIO,

    /** Apoio: cadastro e agendamento, sem acesso a prontuário. */
    ATENDENTE
}
