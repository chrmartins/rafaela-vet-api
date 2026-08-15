package com.rafaelasoares.common.exception;

/**
 * Raiz de toda exceção de regra de negócio do sistema.
 *
 * <p>Existe para que o tratamento HTTP seja por <b>categoria</b> e não por
 * classe: o {@code ApiExceptionHandler} lida com as três subclasses diretas
 * ({@link NotFoundException}, {@link ConflictException},
 * {@link BusinessRuleException}) e, com isso, cobre todas as exceções que os
 * domínios criarem daqui pra frente sem ganhar um método novo a cada uma.
 *
 * <p>Cada domínio cria as suas em {@code <dominio>/exception/}, com nome que
 * descreve a situação em português — {@code UsuarioNaoEncontradoException},
 * {@code HorarioIndisponivelException} — estendendo a categoria certa.
 *
 * <p>Não estenda esta classe diretamente: escolha uma das três categorias.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String mensagem) {
        super(mensagem);
    }

    protected DomainException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
