package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accounting.dto.accounting.TransactionAccountingCreateDTO;
import com.nubixconta.modules.accounting.repository.TransactionAccountingRepository;
import com.nubixconta.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionAccountingService {

    private final TransactionAccountingRepository transactionRepository;
    private final CatalogService catalogService; // Para validar y obtener las cuentas

    // Helper para obtener el companyId de forma segura
    private Integer getCompanyIdFromContext() {
        return TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("No se puede determinar la empresa del contexto de seguridad."));
    }

    @Transactional
    public void createTransaction(TransactionAccountingCreateDTO dto) {
        Integer companyId = getCompanyIdFromContext();

        // 1. Validar la partida doble
        validatePartidaDoble(dto);

        // 2. Aquí iría la lógica para mapear el DTO a las entidades
        //    TransactionAccounting y AccountingEntry.

        // 3. Se asigna el estado inicial PENDIENTE.

        // 4. Se guarda la transacción y sus líneas.
        //    transactionRepository.save(newTransaction);
    }

    @Transactional
    public void applyTransaction(Long transactionId) {
        Integer companyId = getCompanyIdFromContext();
        // 1. Buscar la transacción, asegurando que pertenezca a la empresa.
        // 2. Validar que su estado sea PENDIENTE.
        // 3. Cambiar el estado a APLICADA.
        // 4. ¡ACCIÓN CLAVE! Aquí es donde se podría, en el futuro,
        //    actualizar una tabla de saldos para reportes rápidos (Mayorización).
        //    Por ahora, el simple cambio de estado es suficiente.
        // 5. Guardar la transacción actualizada.
    }

    @Transactional
    public void cancelTransaction(Long transactionId) {
        Integer companyId = getCompanyIdFromContext();
        // 1. Buscar la transacción.
        // 2. Validar que su estado sea APLICADA.
        // 3. Cambiar el estado a ANULADA.
        // 4. ¡ACCIÓN CLAVE! Revertir el impacto contable si se implementó
        //    una tabla de saldos en el paso 'apply'.
        // 5. Guardar la transacción actualizada.
    }

    // --- Métodos de Ayuda para Validaciones ---

    private void validatePartidaDoble(TransactionAccountingCreateDTO dto) {
        BigDecimal totalDebe = dto.getEntries().stream()
                .map(e -> e.getDebit().setScale(2, BigDecimal.ROUND_HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalHaber = dto.getEntries().stream()
                .map(e -> e.getCredit().setScale(2, BigDecimal.ROUND_HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new BusinessRuleException("Partida descuadrada. Total Debe: " + totalDebe + ", Total Haber: " + totalHaber);
        }

        if (totalDebe.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("La partida debe tener un valor mayor a cero.");
        }
    }
}