package com.rafaelasoares.web;

import com.rafaelasoares.acesso.exception.CredenciaisInvalidasException;
import com.rafaelasoares.common.exception.BusinessRuleException;
import com.rafaelasoares.common.exception.ConflictException;
import com.rafaelasoares.common.exception.NotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz exceção de domínio em resposta HTTP.
 *
 * <p>O tratamento é por <b>categoria</b> ({@code NotFoundException},
 * {@code ConflictException}, {@code BusinessRuleException}), não por classe
 * concreta. Assim, quando `cadastro` criar
 * {@code AnimalNaoEncontradoException} ou `agendamento` criar
 * {@code HorarioIndisponivelException}, elas já são tratadas — basta
 * estender a categoria certa. Este arquivo não cresce junto com o sistema.
 *
 * <p>Todo erro é logado. Não é preciso escrever o id da requisição na
 * mensagem: ele está no MDC e o padrão de log o imprime em toda linha (ver
 * {@link RequestIdFilter}).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Credencial inválida → 401.
     *
     * <p>Mensagem sempre igual para e-mail inexistente, senha errada e
     * usuário inativo: diferenciar permitiria descobrir quem tem conta.
     */
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErrorResponse> tratarCredenciaisInvalidas(
            CredenciaisInvalidasException erro) {
        return resposta(HttpStatus.UNAUTHORIZED, erro.getMessage());
    }

    /**
     * Autenticado, mas sem permissão → 403.
     *
     * <p>É o caso de um ATENDENTE tentando gerenciar usuários.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> tratarAcessoNegado(AccessDeniedException erro) {
        log.info("Acesso negado: {}", erro.getMessage());
        return resposta(
                HttpStatus.FORBIDDEN, "Você não tem permissão para executar esta operação.");
    }

    /** Recurso inexistente → 404. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> tratarNaoEncontrado(NotFoundException erro) {
        // Nível info: é desfecho esperado de negócio, não defeito do sistema.
        log.info("Recurso não encontrado: {}", erro.getMessage());
        return resposta(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    /** Conflito com dado existente (unicidade, concorrência) → 409. */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> tratarConflito(ConflictException erro) {
        log.info("Conflito: {}", erro.getMessage());
        return resposta(HttpStatus.CONFLICT, erro.getMessage());
    }

    /** Requisição bem formada, mas que fere regra de negócio → 422. */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> tratarRegraDeNegocio(BusinessRuleException erro) {
        log.info("Regra de negócio violada: {}", erro.getMessage());
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, erro.getMessage());
    }

    /** Falha do Bean Validation nos DTOs de entrada → 400, campo a campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> tratarValidacao(MethodArgumentNotValidException erro) {
        List<ErrorResponse.CampoInvalido> campos =
                erro.getBindingResult().getFieldErrors().stream()
                        .map(
                                campo ->
                                        new ErrorResponse.CampoInvalido(
                                                campo.getField(), campo.getDefaultMessage()))
                        .toList();

        log.debug("Requisição inválida: {}", campos);

        return ResponseEntity.badRequest()
                .body(
                        ErrorResponse.deValidacao(
                                HttpStatus.BAD_REQUEST.value(),
                                "Há campos inválidos na requisição.",
                                campos));
    }

    /**
     * Rede de segurança para a corrida entre dois cadastros com o mesmo dado
     * único: a checagem prévia no service pode passar nos dois, e aí quem
     * barra é a constraint do banco.
     *
     * <p>A mensagem devolvida é genérica de propósito — nome de constraint
     * expõe estrutura interna do banco. O detalhe fica no log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> tratarViolacaoDeIntegridade(
            DataIntegrityViolationException erro) {
        log.warn("Violação de integridade no banco", erro);
        return resposta(
                HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados.");
    }

    /**
     * Rede de segurança final: qualquer coisa não prevista vira 500 genérico.
     *
     * <p>Sem isto, um defeito de programação (um {@code NullPointerException},
     * por exemplo) devolveria stack trace ao cliente, entregando detalhe
     * interno do sistema. Aqui o cliente recebe só o id da requisição — e é
     * com ele que se acha o stack trace completo no log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> tratarErroInesperado(Exception erro) {
        log.error("Erro inesperado ao processar a requisição", erro);
        return resposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro inesperado. Informe o identificador da requisição ao suporte.");
    }

    private ResponseEntity<ErrorResponse> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ErrorResponse.de(status.value(), mensagem));
    }
}
