package com.vetplanet.modules.cliente.entity;

/**
 * Sexo do animal.
 *
 * <p>{@code INDEFINIDO} existe para filhote recém-resgatado, em que a
 * determinação ainda não foi feita — sem ele, o cadastro obrigaria um chute
 * que depois ninguém corrige.
 */
public enum SexoAnimal {
    MACHO,
    FEMEA,
    INDEFINIDO
}
