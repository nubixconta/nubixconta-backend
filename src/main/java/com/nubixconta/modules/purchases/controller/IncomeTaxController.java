package com.nubixconta.modules.purchases.controller;
import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.accounting.dto.AccountingEntryResponseDTO;
import com.nubixconta.modules.accounting.service.PurchasesAccountingService;
import com.nubixconta.modules.purchases.dto.incometax.IncomeTaxCreateDTO;
import com.nubixconta.modules.purchases.dto.incometax.IncomeTaxResponseDTO;
import com.nubixconta.modules.purchases.dto.incometax.IncomeTaxUpdateDTO;
import com.nubixconta.modules.purchases.service.IncomeTaxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController
@RequestMapping("/api/v1/income-taxes")
@RequiredArgsConstructor
public class IncomeTaxController {
    private final IncomeTaxService incomeTaxService;
    private final PurchasesAccountingService purchasesAccountingService;

    @GetMapping
    public ResponseEntity<List<IncomeTaxResponseDTO>> getAllIncomeTaxes(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy) {
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }
        List<IncomeTaxResponseDTO> incomeTaxes = incomeTaxService.findAll(sortBy);
        return ResponseEntity.ok(incomeTaxes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncomeTaxResponseDTO> getIncomeTaxById(@PathVariable Integer id) {
        return ResponseEntity.ok(incomeTaxService.findById(id));
    }

    @GetMapping("/by-purchase/{purchaseId}")
    public ResponseEntity<List<IncomeTaxResponseDTO>> getIncomeTaxesByPurchase(@PathVariable Integer purchaseId) {
        return ResponseEntity.ok(incomeTaxService.findByPurchaseId(purchaseId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<IncomeTaxResponseDTO>> searchByDateAndStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(incomeTaxService.findByDateRangeAndStatus(start, end, status));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<IncomeTaxResponseDTO>> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok(incomeTaxService.findByStatus(status));
    }

    @PostMapping
    public ResponseEntity<IncomeTaxResponseDTO> createIncomeTax(@Valid @RequestBody IncomeTaxCreateDTO createDTO) {
        IncomeTaxResponseDTO createdIncomeTax = incomeTaxService.createIncomeTax(createDTO);
        return new ResponseEntity<>(createdIncomeTax, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IncomeTaxResponseDTO> updateIncomeTax(@PathVariable Integer id, @Valid @RequestBody IncomeTaxUpdateDTO updateDTO) {
        IncomeTaxResponseDTO updatedIncomeTax = incomeTaxService.updateIncomeTax(id, updateDTO);
        return ResponseEntity.ok(updatedIncomeTax);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncomeTax(@PathVariable Integer id) {
        incomeTaxService.deleteIncomeTax(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<IncomeTaxResponseDTO> applyIncomeTax(@PathVariable Integer id) {
        IncomeTaxResponseDTO appliedIncomeTax = incomeTaxService.applyIncomeTax(id);
        return ResponseEntity.ok(appliedIncomeTax);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<IncomeTaxResponseDTO> cancelIncomeTax(@PathVariable Integer id) {
        IncomeTaxResponseDTO cancelledIncomeTax = incomeTaxService.cancelIncomeTax(id);
        return ResponseEntity.ok(cancelledIncomeTax);
    }

    @GetMapping("/{id}/accounting-entry")
    public ResponseEntity<AccountingEntryResponseDTO> getIncomeTaxAccountingEntry(@PathVariable Integer id) {
        // Esta llamada ahora funciona porque ya implementamos el método en el servicio
        AccountingEntryResponseDTO entryDto = purchasesAccountingService.getEntryForIncomeTax(id);
        return ResponseEntity.ok(entryDto);
    }




}
