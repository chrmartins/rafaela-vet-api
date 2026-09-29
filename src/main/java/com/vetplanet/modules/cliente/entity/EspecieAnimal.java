package com.vetplanet.modules.cliente.entity;

/**
 * Espécies atendidas.
 *
 * <p>Fechado de propósito: a clínica é de cães e gatos (ver CLAUDE.md).
 * Atender outra espécie é decisão de negócio — abrir este enum exige
 * migration, o que é justamente a fricção que faz a conversa acontecer.
 */
public enum EspecieAnimal {
    CAO,
    GATO
}
