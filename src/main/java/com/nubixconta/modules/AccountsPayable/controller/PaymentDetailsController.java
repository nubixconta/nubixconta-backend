package com.nubixconta.modules.AccountsPayable.controller;

import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsCreateDTO;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsResponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsUpdateDTO;
import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.AccountsPayable.repository.AccountsPayableRepository;
import com.nubixconta.modules.AccountsPayable.service.PaymentDetailsService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payment-details")
public class PaymentDetailsController {

    private final PaymentDetailsService service;
    private final AccountsPayableRepository accountsPayableRepository;
    private final ModelMapper modelMapper;

    public PaymentDetailsController(PaymentDetailsService service, AccountsPayableRepository accountsReceivableRepository, ModelMapper modelMapper) {
        this.service = service;
        this.accountsPayableRepository = accountsReceivableRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping
    public List<PaymentDetailsResponseDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailsResponseDTO> getById(@PathVariable Integer id) {
        return service.findById(id)
                .map(paymentDetails -> modelMapper.map(paymentDetails, PaymentDetailsResponseDTO.class))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //Enpoint para eliminar un pago
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PaymentDetailsResponseDTO> partialUpdate(
            @PathVariable Integer id,
            @RequestBody @Valid PaymentDetailsUpdateDTO dto) {

        return service.findById(id)
                .map(existing -> {

                    Integer payableId = dto.getAccountsPayableId() != null
                            ? dto.getAccountsPayableId()
                            : existing.getAccountsPayable().getId();

                    if (dto.getReference() != null) existing.setReference(dto.getReference());
                    if (dto.getPaymentMethod() != null) existing.setPaymentMethod(dto.getPaymentMethod());
                    if (dto.getPaymentAmount() != null) existing.setPaymentAmount(dto.getPaymentAmount());
                    if (dto.getPaymentStatus() != null) existing.setPaymentStatus(dto.getPaymentStatus());
                    if (dto.getPaymentDetailDescription() != null) existing.setPaymentDetailDescription(dto.getPaymentDetailDescription());
                    if (dto.getAccountId() != null) existing.setAccountId(dto.getAccountId());

                    PaymentDetails actualizado = service.save(existing);

                    service.recalcularBalancePorPayableId(payableId);
                    // Mapear la entidad actualizada al DTO antes de devolverla
                    PaymentDetailsResponseDTO responseDto = modelMapper.map(actualizado, PaymentDetailsResponseDTO.class);
                    return ResponseEntity.ok(responseDto);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    //Metodo para registrar el pago
    @PostMapping("/make-payment")
    public ResponseEntity<PaymentDetailsResponseDTO> makePayment(
            @RequestBody @Valid PaymentDetailsCreateDTO dto) {

        PaymentDetails saved = service.makePayment(dto);
        PaymentDetailsResponseDTO response = modelMapper.map(saved, PaymentDetailsResponseDTO.class);
        return ResponseEntity.ok(response);
    }

    //Busca pagos por un rango de fechas
    @GetMapping("/search-by-date")
    public ResponseEntity<List<PaymentDetailsResponseDTO>> searchByDateRange(
            @RequestParam("start") String startStr,
            @RequestParam("end") String endStr) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Se convierte la fecha al inicio y fin del día
        LocalDate startDate = LocalDate.parse(startStr, formatter);
        LocalDateTime start = startDate.atStartOfDay();

        LocalDate endDate = LocalDate.parse(endStr, formatter);
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<PaymentDetails> results = service.findByDateRange(start, end);

        List<PaymentDetailsResponseDTO> responseDTOs = results.stream()
                .map(paymentDetails -> modelMapper.map(paymentDetails, PaymentDetailsResponseDTO.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }
}
