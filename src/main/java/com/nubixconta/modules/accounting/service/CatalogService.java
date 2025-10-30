package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.common.exception.NotFoundException;
import com.nubixconta.common.util.AccountingCodeFormatter;
import com.nubixconta.modules.accounting.dto.catalog.CatalogSummaryDTO; // El DTO que ya creamos
import com.nubixconta.modules.accounting.dto.catalog.CompanyCatalogNodeDTO;
import com.nubixconta.modules.accounting.dto.catalog.UpdateCatalogDTO;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.entity.Catalog;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import com.nubixconta.modules.accounting.repository.CatalogRepository;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.administration.repository.CompanyRepository;
import com.nubixconta.security.TenantContext;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final AccountRepository accountRepository; // <-- INYECTAR
    private final CompanyRepository companyRepository; // <-- INYECTAR

    // ModelMapper no es necesario aquí, el mapeo es simple y explícito.

    /**
     * MÉTODO CRÍTICO para PurchaseService.
     * Busca una entrada del catálogo por su ID.
     * Lanza una excepción si no se encuentra o no está activa.
     * La seguridad multi-tenant es manejada por el filtro de Hibernate.
     *
     * @param id El ID de la entrada del catálogo.
     * @return La entidad Catalog completa si es válida.
     */
    public Catalog findEntityById(Integer id) {
        Catalog catalog = catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La cuenta contable con ID de catálogo " + id + " no fue encontrada."));

        if (!catalog.isActive() || !catalog.getAccount().isPostable()) {
            throw new BusinessRuleException("La cuenta contable '" + catalog.getAccount().getAccountName() + "' no está activa o no permite movimientos.");
        }

        return catalog;
    }

    /**
     * Busca cuentas activas y que aceptan movimientos para la empresa actual, basándose en un término de búsqueda.
     * Este método será consumido por el frontend.
     *
     * @param term El término a buscar (puede ser vacío para traer todo).
     * @return Lista de CatalogSummaryDTO.
     */
    public List<CatalogSummaryDTO> searchActiveByTerm(String term) {
        Integer companyId = TenantContext.getCurrentTenant()
                .orElseThrow(() -> new IllegalStateException("No se ha seleccionado una empresa en el contexto."));

        String searchTerm = (term == null) ? "" : term.trim();

        List<Catalog> catalogs = catalogRepository.searchActiveAndPostableByTerm(companyId, searchTerm);

        return catalogs.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    // Helper privado para mapear la entidad a un DTO seguro para la API.
    private CatalogSummaryDTO mapToSummaryDTO(Catalog catalog) {
        CatalogSummaryDTO dto = new CatalogSummaryDTO();
        dto.setId(catalog.getId()); // ID de la tabla 'catalog'
        // DESPUÉS (Usa los nuevos métodos de la entidad):
        dto.setAccountCode(AccountingCodeFormatter.format(catalog.getEffectiveCode()));
        dto.setAccountName(catalog.getEffectiveName());
        return dto;
    }

    // --- ¡INICIO DE NUEVOS MÉTODOS DE GESTIÓN! ---

    /**
     * Construye y devuelve el árbol del catálogo personalizado para la empresa actual.
     */
    @Transactional(readOnly = true)
    public List<CompanyCatalogNodeDTO> getCompanyTree() {
        Integer companyId = TenantContext.getCurrentTenant().orElseThrow();
        List<Catalog> companyAccounts = catalogRepository.findByCompany_IdWithAccount(companyId);

        Map<Integer, CompanyCatalogNodeDTO> nodeMap = companyAccounts.stream()
                .map(this::mapToCompanyNodeDTO)
                .collect(Collectors.toMap(CompanyCatalogNodeDTO::getId, Function.identity()));

        List<CompanyCatalogNodeDTO> rootNodes = new ArrayList<>();
        companyAccounts.forEach(catalog -> {
            CompanyCatalogNodeDTO node = nodeMap.get(catalog.getId());
            if (catalog.getParent() != null) {
                CompanyCatalogNodeDTO parentNode = nodeMap.get(catalog.getParent().getId());
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                }
            } else {
                rootNodes.add(node);
            }
        });
        return rootNodes;
    }

    // REEMPLAZA ESTE MÉTODO COMPLETO EN CatalogService.java

    // REEMPLAZA ESTE MÉTODO EN CatalogService.java

    @Transactional
    public void activateAccounts(List<Integer> masterAccountIds) {
        Integer companyId = TenantContext.getCurrentTenant().orElseThrow();
        Company company = companyRepository.findById(companyId).orElseThrow();

        // 1. Obtener el estado actual del catálogo de la empresa.
        Map<Integer, Catalog> existingCompanyCatalogMap = catalogRepository.findByCompany_IdWithAccount(companyId)
                .stream()
                .collect(Collectors.toMap(c -> c.getAccount().getId(), Function.identity()));

        // 2. ¡NUEVA LÓGICA! Encontrar todas las cuentas a activar, incluyendo sus ancestros.
        Set<Integer> allIdsToEnsureAreActive = new HashSet<>(masterAccountIds);
        List<Account> initialAccounts = accountRepository.findAllById(masterAccountIds);

        for (Account account : initialAccounts) {
            Account current = account;
            // Recorre hacia arriba el árbol maestro hasta llegar a la raíz.
            while (current.getParentAccount() != null) {
                current = current.getParentAccount();
                allIdsToEnsureAreActive.add(current.getId());
            }
        }

        // 3. Ahora, filtramos de este set completo las que realmente necesitamos procesar.
        List<Account> accountsToProcess = accountRepository.findAllById(allIdsToEnsureAreActive);

        // 4. Procedemos con la lógica de activación/reactivación que ya teníamos.
        List<Catalog> entriesToCreate = new ArrayList<>();
        List<Catalog> entriesToUpdate = new ArrayList<>();
        Map<Integer, Catalog> newlyCreatedCatalogMap = new HashMap<>();

        // Ordenar por jerarquía para asegurar que los padres se creen primero.
        accountsToProcess.sort((a1, a2) ->
                Integer.compare(a1.getGeneratedCode().length(), a2.getGeneratedCode().length())
        );

        for (Account accountToActivate : accountsToProcess) {
            if (existingCompanyCatalogMap.containsKey(accountToActivate.getId())) {
                Catalog existingEntry = existingCompanyCatalogMap.get(accountToActivate.getId());
                if (!existingEntry.isActive()) {
                    existingEntry.setActive(true);
                    entriesToUpdate.add(existingEntry);
                }
            } else {
                // Lógica para crear una nueva entrada (sin cambios)
                Catalog newEntry = new Catalog();
                newEntry.setAccount(accountToActivate);
                newEntry.setCompany(company);

                if (accountToActivate.getParentAccount() != null) {
                    Integer parentAccountId = accountToActivate.getParentAccount().getId();
                    Catalog parentCatalog = existingCompanyCatalogMap.get(parentAccountId);
                    if (parentCatalog == null) {
                        parentCatalog = newlyCreatedCatalogMap.get(parentAccountId);
                    }
                    if (parentCatalog != null) {
                        newEntry.setParent(parentCatalog);
                    }
                }

                entriesToCreate.add(newEntry);
                newlyCreatedCatalogMap.put(accountToActivate.getId(), newEntry);
            }
        }

        if (!entriesToCreate.isEmpty()) {
            catalogRepository.saveAll(entriesToCreate);
        }
        if (!entriesToUpdate.isEmpty()) {
            catalogRepository.saveAll(entriesToUpdate);
        }
    }
    /**
     * Actualiza los campos personalizados de una entrada del catálogo.
     */
    @Transactional
    public CompanyCatalogNodeDTO updateCustomFields(Integer catalogId, UpdateCatalogDTO dto) {
        Integer companyId = TenantContext.getCurrentTenant().orElseThrow();
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new NotFoundException("Entrada de catálogo con ID " + catalogId + " no encontrada."));

        // Verificación de seguridad crucial
        if (!catalog.getCompany().getId().equals(companyId)) {
            throw new BusinessRuleException("No tiene permiso para modificar esta cuenta contable.");
        }

        catalog.setCustomName(dto.getCustomName());
        catalog.setCustomCode(dto.getCustomCode());

        Catalog savedCatalog = catalogRepository.save(catalog);
        return mapToCompanyNodeDTO(savedCatalog);
    }

    /**
     * Desactiva una lista de cuentas del catálogo de la empresa, incluyendo todos sus descendientes.
     */
    @Transactional
    public void deactivateAccounts(List<Integer> catalogIds) {
        Integer companyId = TenantContext.getCurrentTenant().orElseThrow();

        // 1. Cargar el catálogo completo de la empresa UNA SOLA VEZ para eficiencia.
        List<Catalog> allCompanyAccounts = catalogRepository.findByCompany_IdWithAccount(companyId);
        Map<Integer, Catalog> catalogMap = allCompanyAccounts.stream()
                .collect(Collectors.toMap(Catalog::getId, Function.identity()));

        Set<Integer> idsToDeactivate = new HashSet<>();

        // 2. Para cada ID solicitado, encontrarlo y añadirlo a él y a todos sus hijos a la lista de desactivación.
        for (Integer id : catalogIds) {
            Catalog accountToDeactivate = catalogMap.get(id);
            if (accountToDeactivate != null && accountToDeactivate.getCompany().getId().equals(companyId)) {
                findAllDescendants(accountToDeactivate, allCompanyAccounts, idsToDeactivate);
            }
        }

        // 3. Filtrar las entidades que realmente necesitan ser desactivadas.
        List<Catalog> entriesToUpdate = allCompanyAccounts.stream()
                .filter(catalog -> idsToDeactivate.contains(catalog.getId()) && catalog.isActive())
                .collect(Collectors.toList());

        // 4. Si hay algo que actualizar, cambiar el estado y guardar.
        if (!entriesToUpdate.isEmpty()) {
            entriesToUpdate.forEach(catalog -> catalog.setActive(false));
            catalogRepository.saveAll(entriesToUpdate);
        }
    }

    /**
     * Método recursivo helper para encontrar todos los descendientes de un nodo de catálogo.
     * @param parent El nodo del que se parte.
     * @param allAccounts La lista completa de cuentas de la empresa para evitar consultas a la BD.
     * @param descendantsSet El Set donde se acumulan los IDs.
     */
    private void findAllDescendants(Catalog parent, List<Catalog> allAccounts, Set<Integer> descendantsSet) {
        // Añadir el propio padre a la lista
        descendantsSet.add(parent.getId());

        // Encontrar los hijos directos
        allAccounts.stream()
                .filter(account -> account.getParent() != null && account.getParent().getId().equals(parent.getId()))
                .forEach(child -> findAllDescendants(child, allAccounts, descendantsSet)); // Llamada recursiva para cada hijo
    }

    private CompanyCatalogNodeDTO mapToCompanyNodeDTO(Catalog catalog) {
        CompanyCatalogNodeDTO dto = new CompanyCatalogNodeDTO();
        dto.setId(catalog.getId());
        dto.setEffectiveName(catalog.getEffectiveName());
        dto.setEffectiveCode(AccountingCodeFormatter.format(catalog.getEffectiveCode()));
        dto.setActive(catalog.isActive());

        // Aseguramos que la entidad 'account' no sea nula para evitar NullPointerExceptions
        if (catalog.getAccount() != null) {
            // Poblamos el ID de la cuenta maestra
            dto.setMasterAccountId(catalog.getAccount().getId());

            // --- ¡ESTA ES LA LÍNEA CORREGIDA! ---
            // Asignamos correctamente el valor de isPostable desde la entidad Account
            // al campo isPostable del DTO usando el setter correcto.
            dto.setPostable(catalog.getAccount().isPostable());
        } else {
            // En el improbable caso de que una entrada de catálogo no tenga una cuenta maestra
            // asociada (lo que indicaría datos corruptos), establecemos un valor por defecto seguro.
            dto.setMasterAccountId(null);
            dto.setPostable(false); // Una cuenta sin datos maestros no debería ser editable.
        }

        return dto;
    }
    @Transactional
    public void processCompanyCatalogUpload(MultipartFile file) throws Exception {
        Integer companyId = TenantContext.getCurrentTenant().orElseThrow();

        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        // Omitir encabezado
        if (rows.hasNext()) {
            rows.next();
        }

        while (rows.hasNext()) {
            Row currentRow = rows.next();
            String masterCode = currentRow.getCell(0).getStringCellValue();
            String customCode = currentRow.getCell(1) != null ? currentRow.getCell(1).getStringCellValue() : null;
            String customName = currentRow.getCell(2) != null ? currentRow.getCell(2).getStringCellValue() : null;

            // 1. Encontrar la cuenta maestra por su código.
            Account masterAccount = accountRepository.findByGeneratedCode(masterCode) // <-- Necesitarás crear este método en AccountRepository
                    .orElseThrow(() -> new BusinessRuleException("El MasterCode '" + masterCode + "' no existe en el catálogo maestro."));

            // 2. Activar la cuenta (nuestra lógica existente previene duplicados)
            this.activateAccounts(List.of(masterAccount.getId()));

            // 3. Si hay personalización, aplicarla.
            if ((customCode != null && !customCode.isBlank()) || (customName != null && !customName.isBlank())) {
                // Encontrar la entrada de catálogo que acabamos de crear
                Catalog catalogToUpdate = catalogRepository.findByCompany_IdAndAccount_Id(companyId, masterAccount.getId()).get();

                UpdateCatalogDTO updateDTO = new UpdateCatalogDTO();
                updateDTO.setCustomCode(customCode);
                updateDTO.setCustomName(customName);

                this.updateCustomFields(catalogToUpdate.getId(), updateDTO);
            }
        }
        workbook.close();
    }
}