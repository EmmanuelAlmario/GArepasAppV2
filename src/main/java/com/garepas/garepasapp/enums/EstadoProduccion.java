package com.garepas.garepasapp.enums;

/**
 * Estado de una producción:
 * - PENDIENTE: hay insumos insuficientes, no se descontó stock ni se creó gasto.
 * - COMPLETADA: todos los insumos son suficientes, stock descontado y gasto creado.
 */
public enum EstadoProduccion {
    PENDIENTE,
    COMPLETADA
}
