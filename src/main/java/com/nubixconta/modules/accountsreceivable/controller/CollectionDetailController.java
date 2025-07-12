package com.nubixconta.modules.accountsreceivable.controller;

import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailCreateDTO;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailResponseDTO;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailUpdateDTO;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import com.nubixconta.modules.accountsreceivable.repository.AccountsReceivableRepository;
import com.nubixconta.modules.accountsreceivable.service.CollectionDetailService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collection-detail")
public class CollectionDetailController {

    private final CollectionDetailService service;
    private final AccountsReceivableRepository accountsReceivableRepository;
    private final ModelMapper modelMapper;

    public CollectionDetailController(CollectionDetailService service, AccountsReceivableRepository accountsReceivableRepository,ModelMapper modelMapper) {
        this.service = service;
        this.accountsReceivableRepository = accountsReceivableRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public List<CollectionDetail> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionDetail> getById(@PathVariable Integer id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CollectionDetail> partialUpdate(
            @PathVariable Integer id,
            @RequestBody @Valid CollectionDetailUpdateDTO dto) {

        return service.findById(id)
                .map(existing -> {

                    Integer receivableId = dto.getAccountReceivableId() != null
                            ? dto.getAccountReceivableId()
                            : existing.getAccountReceivable().getId();

                    if (dto.getReference() != null) existing.setReference(dto.getReference());
                    if (dto.getPaymentMethod() != null) existing.setPaymentMethod(dto.getPaymentMethod());
                    if (dto.getPaymentAmount() != null) existing.setPaymentAmount(dto.getPaymentAmount());
                    if (dto.getPaymentStatus() != null) existing.setPaymentStatus(dto.getPaymentStatus());
                    if (dto.getPaymentDetailDescription() != null) existing.setPaymentDetailDescription(dto.getPaymentDetailDescription());
                    if (dto.getAccountId() != null) existing.setAccountId(dto.getAccountId());

                    CollectionDetail actualizado = service.save(existing);

                    service.recalcularBalancePorReceivableId(receivableId);
                    return ResponseEntity.ok(actualizado);
                })
                .orElse(ResponseEntity.notFound().build());
    }



    //Busca cobros por un rango de fechas
    @GetMapping("/search-by-date")
    public ResponseEntity<List<CollectionDetail>> searchByDateRange(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Se convierte la fecha al inicio y fin del día
        LocalDate startDate = LocalDate.parse(startStr, formatter);
        LocalDateTime start = startDate.atStartOfDay();

        LocalDate endDate = LocalDate.parse(endStr, formatter);
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<CollectionDetail> results = service.findByDateRange(start, end);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/register-payment")
    public ResponseEntity<CollectionDetailResponseDTO> registerPayment(
            @RequestBody @Valid CollectionDetailCreateDTO dto) {

        CollectionDetail saved = service.registerPayment(dto);
        CollectionDetailResponseDTO response = modelMapper.map(saved, CollectionDetailResponseDTO.class);
        return ResponseEntity.ok(response);
    }
    /*
    @PostMapping
    public ResponseEntity<CollectionDetail> create(@RequestBody CollectionDetail detail) {
        return ResponseEntity.ok(service.save(detail));
    }
*/
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
