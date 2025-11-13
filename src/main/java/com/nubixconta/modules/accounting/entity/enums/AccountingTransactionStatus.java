package com.nubixconta.modules.accounting.entity.enums;

/**
 * Define el ciclo de vida de una transacción contable manual.
 * Sigue el mismo patrón que el estado de una Compra o Venta.
 */
public enum AccountingTransactionStatus {
    /**
     * La transacción ha sido creada pero aún no afecta la contabilidad.
     * En este estado, puede ser modificada o eliminada.
     */
    PENDIENTE,

    /**
     * La transacción ha sido validada y aplicada. Sus valores ya forman parte
     * de los saldos contables. En este estado, no puede ser modificada ni eliminada,
     * solo anulada.
     */
    APLICADA,

    /**
     * La transacción fue aplicada y posteriormente revertida. Es un estado final
     * y no se puede realizar ninguna otra acción sobre ella.
     */
    ANULADA
}