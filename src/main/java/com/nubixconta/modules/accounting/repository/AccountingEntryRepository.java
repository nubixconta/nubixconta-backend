package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.AccountingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingEntryRepository extends JpaRepository<AccountingEntry, Long> {
    // Por ahora, no se necesitan métodos personalizados aquí, ya que todas las operaciones
    // se gestionarán a través de la entidad raíz 'TransactionAccounting'.
    // Tener este repositorio es una buena práctica para futuras necesidades.
}