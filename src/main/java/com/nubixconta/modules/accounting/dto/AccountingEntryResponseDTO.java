package com.nubixconta.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO principal para la respuesta de la API de asientos contables.
 * Contiene toda la información necesaria para que el frontend renderice la vista
 * del asiento contable sin necesidad de hacer consultas adicionales.
 */
public record AccountingEntryResponseDTO(
        /**
         * ID único del asiento contable (ej. el ID de la primera línea en SaleEntry).
         * Corresponde al campo "Asiento contable #" en la vista.
         */
        Integer accountingEntryId,

        /**
         * Número del documento padre (ej. "F-001-002").
         * Corresponde al campo "Venta número" o "Nota de Crédito número" en la vista.
         */
        String documentNumber,

        /**
         * Tipo de documento de origen (ej. "Venta", "Nota de Crédito").
         * Ayuda al frontend a renderizar las etiquetas correctas.
         */
        String documentType,

        /**
         * Estado actual del documento de origen (ej. "APLICADA", "ANULADA").
         * Corresponde al campo "Estado" en la vista.
         */
        String documentStatus,

        /**
         * Etiqueta para el socio de negocio (ej. "Cliente").
         */
        String partnerLabel,

        /**
         * Nombre completo del socio de negocio (ej. "Juan Pérez").
         * Corresponde al campo "Proveedor/Cliente" en la vista.
         */
        String partnerName,

        /**
         * Fecha y hora en que se generó el asiento contable (cuando se aplicó la transacción).
         * Corresponde al campo "Fecha" en la cabecera.
         */
        LocalDateTime entryDate,

        /**
         * Descripción general de la transacción.
         */
        String description,

        /**
         * Lista de todas las líneas de movimiento (Debe/Haber) del asiento.
         */
        List<AccountingEntryLineDTO> lines,

        /**
         * Suma total de todos los débitos del asiento.
         */
        BigDecimal totalDebits,

        /**
         * Suma total de todos los créditos del asiento.
         */
        BigDecimal totalCredits
) {}
