package com.vetplanet.modules.prontuario.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * O conteúdo clínico de um atendimento.
 *
 * <p><b>Quase tudo é opcional aqui, e isso é o desenho.</b> Nota clínica é
 * escrita em pedaços, durante a visita — se o corpo exigisse tudo, salvar o
 * rascunho pela metade seria impossível. Quem exige completude é a conclusão,
 * não o salvamento.
 *
 * <p>A separação entre {@code anamnese} e {@code exameClinico} não é capricho:
 * "a tutora disse que ela está vomitando" e "mucosas hipocoradas" são relato de
 * terceiro e constatação profissional. Num documento que pode ser pedido numa
 * disputa, não podem morar no mesmo parágrafo.
 */
public record RegistrarAtendimentoRequestDto(
        @NotBlank(message = "Informe onde o atendimento aconteceu.")
                @Size(max = 400, message = "Endereço muito longo.")
                String localAtendimento,
        @Size(max = 5000) String anamnese,
        @Size(max = 5000) String exameClinico,
        @Size(max = 500) String diagnosticoPresuntivo,
        @Size(max = 500) String diagnosticoConclusivo,
        @Size(max = 5000) String recomendacoes,
        @Size(max = 5000) String prescricao,
        @DecimalMin(value = "0.001", message = "Peso inválido.")
                @DecimalMax(value = "999.999", message = "Peso inválido.")
                BigDecimal pesoKg,
        @DecimalMin(value = "20.0", message = "Temperatura fora do plausível.")
                @DecimalMax(value = "45.0", message = "Temperatura fora do plausível.")
                BigDecimal temperaturaC,
        @Min(value = 1, message = "Frequência cardíaca inválida.") Integer frequenciaCardiaca,
        @Min(value = 1, message = "Frequência respiratória inválida.")
                Integer frequenciaRespiratoria) {}
