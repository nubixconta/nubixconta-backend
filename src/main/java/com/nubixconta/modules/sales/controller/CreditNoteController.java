package com.nubixconta.modules.sales.controller;

import com.nubixconta.common.exception.BadRequestException;
import com.nubixconta.modules.sales.dto.creditnote.CreditNoteCreateDTO;
import com.nubixconta.modules.sales.dto.creditnote.CreditNoteResponseDTO;
import com.nubixconta.modules.sales.dto.creditnote.CreditNoteUpdateDTO;
import com.nubixconta.modules.sales.service.CreditNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nubixconta.modules.accounting.dto.AccountingEntryResponseDTO;
import com.nubixconta.modules.accounting.service.SalesAccountingService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-notes")
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;
    // Inyección del servicio de contabilidad a través del constructor de Lombok.
    private final SalesAccountingService salesAccountingService;
    /**
     * Obtener todas las notas de crédito con un ordenamiento específico.
     * @param sortBy (opcional) Criterio de ordenamiento.
     *               - 'status' (default): Agrupa por PENDIENTE, APLICADA, ANULADA y luego ordena por fecha.
     *               - 'date': Ordena estrictamente por fecha de creación descendente.
     * @return Una lista de notas de crédito ordenadas según el criterio.
     */
    @GetMapping
    public ResponseEntity<List<CreditNoteResponseDTO>> getAllCreditNotes(
            @RequestParam(name = "sortBy", defaultValue = "status") String sortBy
    ) {
        if (!"status".equalsIgnoreCase(sortBy) && !"date".equalsIgnoreCase(sortBy)) {
            throw new BadRequestException("Valor de 'sortBy' no válido. Use 'status' o 'date'.");
        }

        List<CreditNoteResponseDTO> creditNotes = creditNoteService.findAll(sortBy);
        return ResponseEntity.ok(creditNotes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditNoteResponseDTO> getCreditNoteById(@PathVariable Integer id) {
        return ResponseEntity.ok(creditNoteService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CreditNoteResponseDTO> createCreditNote(@Valid @RequestBody CreditNoteCreateDTO createDTO) {
        CreditNoteResponseDTO createdNote = creditNoteService.createCreditNote(createDTO);
        return new ResponseEntity<>(createdNote, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CreditNoteResponseDTO> updateCreditNote(@PathVariable Integer id, @Valid @RequestBody CreditNoteUpdateDTO updateDTO) {
        CreditNoteResponseDTO updatedNote = creditNoteService.updateCreditNote(id, updateDTO);
        return ResponseEntity.ok(updatedNote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCreditNote(@PathVariable Integer id) {
        creditNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-sale/{saleId}")
    public ResponseEntity<List<CreditNoteResponseDTO>> getCreditNotesBySale(@PathVariable Integer saleId) {
        return ResponseEntity.ok(creditNoteService.findBySaleId(saleId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CreditNoteResponseDTO>> searchByDateAndStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(creditNoteService.findByDateRangeAndStatus(start, end, status));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<CreditNoteResponseDTO>> getByStatus(@RequestParam String status) {
        return ResponseEntity.ok(creditNoteService.findByStatus(status));
    }
    //NUEVOS ENDPOINTS PARA EL CICLO DE VIDA ---

    /**
     * Aplica una nota de crédito que está en estado PENDIENTE.
     * Esto afectará el stock (incrementándolo) y cambiará el estado a APLICADA.
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<CreditNoteResponseDTO> applyCreditNote(@PathVariable Integer id) {
        CreditNoteResponseDTO appliedNote = creditNoteService.applyCreditNote(id);
        return ResponseEntity.ok(appliedNote);
    }

    /**
     * Anula una nota de crédito que está en estado APLICADA.
     * Esto revertirá la afectación al stock y cambiará el estado a ANULADA.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CreditNoteResponseDTO> cancelCreditNote(@PathVariable Integer id) {
        CreditNoteResponseDTO cancelledNote = creditNoteService.cancelCreditNote(id);
        return ResponseEntity.ok(cancelledNote);
    }

    // --- INICIO DE CÓDIGO AÑADIDO ---
    /**
     * Endpoint para obtener el asiento contable asociado a una nota de crédito específica.
     * @param id El ID de la nota de crédito.
     * @return Un ResponseEntity con el DTO del asiento contable.
     */
    @GetMapping("/{id}/accounting-entry")
    @PreAuthorize("hasAuthority('READ_CREDIT_NOTE')") // Asegura el endpoint con el permiso adecuado.
    public ResponseEntity<AccountingEntryResponseDTO> getCreditNoteAccountingEntry(@PathVariable Integer id) {
        AccountingEntryResponseDTO entryDto = salesAccountingService.getEntryForCreditNote(id);
        return ResponseEntity.ok(entryDto);
    }
    // --- FIN DE CÓDIGO AÑADIDO ---
}