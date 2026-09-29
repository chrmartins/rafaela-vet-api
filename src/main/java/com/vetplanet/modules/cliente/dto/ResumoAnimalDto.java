package com.vetplanet.modules.cliente.dto;

import java.util.UUID;

/**
 * O mínimo que outro domínio precisa saber sobre um animal: quem é ele e de
 * quem é.
 *
 * <p>Existe para `agendamento` conseguir escrever "09:00 · Thor · J. Lima" na
 * Agenda sem tocar nas tabelas de `cliente` — a fronteira entre domínios é
 * atravessada por application service, nunca por join.
 */
public record ResumoAnimalDto(UUID idAnimal, String nomeAnimal, UUID idTutor, String nomeTutor) {}
