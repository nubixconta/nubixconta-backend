package com.nubixconta.modules.accounting.service;

import com.nubixconta.common.exception.BusinessRuleException;
import com.nubixconta.modules.accounting.dto.catalog.MasterAccountNodeDTO;
import com.nubixconta.modules.accounting.entity.Account;
import com.nubixconta.modules.accounting.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Iterator;
import com.nubixconta.common.util.AccountingCodeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MasterAccountService {

    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<MasterAccountNodeDTO> getTree() {
        List<Account> allAccounts = accountRepository.findAll();

        // El mapeo a DTO ahora se hace en un paso explícito para más claridad
        List<MasterAccountNodeDTO> allNodes = allAccounts.stream()
                .map(account -> {
                    MasterAccountNodeDTO dto = new MasterAccountNodeDTO();
                    dto.setId(account.getId());
                    dto.setName(account.getAccountName());
                    // --- ¡CAMBIO APLICADO AQUÍ! ---
                    dto.setCode(AccountingCodeFormatter.format(account.getGeneratedCode()));
                    dto.setPostable(account.isPostable());
                    return dto;
                })
                .collect(Collectors.toList());

        Map<Integer, MasterAccountNodeDTO> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(MasterAccountNodeDTO::getId, node -> node));

        List<MasterAccountNodeDTO> rootNodes = new ArrayList<>();

        allAccounts.forEach(account -> {
            MasterAccountNodeDTO node = nodeMap.get(account.getId());
            if (account.getParentAccount() != null) {
                MasterAccountNodeDTO parentNode = nodeMap.get(account.getParentAccount().getId());
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                }
            } else {
                rootNodes.add(node);
            }
        });

        return rootNodes;
    }

    @Transactional
    public void processMasterCatalogUpload(MultipartFile file) throws Exception {
        // Para una carga maestra, la estrategia más segura es borrar todo y empezar de nuevo.
        // ¡ADVERTENCIA: ESTO BORRA TODOS LOS CATÁLOGOS DE LAS EMPRESAS!
        // En un sistema real, necesitarías una lógica de migración más compleja.
        // Por ahora, asumimos que es para la carga inicial del sistema.
        // catalogRepository.deleteAll(); // ¡Cuidado con esta línea!
        accountRepository.deleteAll();

        InputStream inputStream = file.getInputStream();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        // Omitir la fila de encabezado
        if (rows.hasNext()) {
            rows.next();
        }

        Map<String, Account> createdAccountsByCode = new HashMap<>();

        while (rows.hasNext()) {
            Row currentRow = rows.next();
            String code = currentRow.getCell(0).getStringCellValue();
            String name = currentRow.getCell(1).getStringCellValue();
            String parentCode = currentRow.getCell(2) != null ? currentRow.getCell(2).getStringCellValue() : null;
            String type = currentRow.getCell(3).getStringCellValue();
            boolean isPostable = "SI".equalsIgnoreCase(currentRow.getCell(4).getStringCellValue());

            Account account = new Account();
            account.setGeneratedCode(code);
            account.setAccountName(name);
            account.setAccountType(type.toUpperCase()); // Asegurar mayúsculas para el Enum
            account.setPostable(isPostable);

            // Asignar el padre si existe
            if (parentCode != null && !parentCode.isBlank()) {
                Account parentAccount = createdAccountsByCode.get(parentCode);
                if (parentAccount == null) {
                    throw new BusinessRuleException("Error en la fila para el código '" + code + "': El código padre '" + parentCode + "' no fue encontrado en las filas anteriores del archivo.");
                }
                account.setParentAccount(parentAccount);
            }

            Account savedAccount = accountRepository.save(account);
            createdAccountsByCode.put(savedAccount.getGeneratedCode(), savedAccount);
        }
        workbook.close();
    }
}