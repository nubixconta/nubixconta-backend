package com.nubixconta.modules.accounting.dto;


import java.math.BigDecimal;

/**
 * DTO que representa una única línea (movimiento) dentro de la respuesta de un asiento contable.
 * Es un registro inmutable que contiene los datos esenciales de un movimiento.
 */
public record AccountingEntryLineDTO(
        String accountCode,
        String accountName,
        BigDecimal debit,
        BigDecimal credit
) {}