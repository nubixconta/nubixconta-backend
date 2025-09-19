package com.nubixconta.modules.AccountsPayable.controller;

import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsResponseDTO;
import com.nubixconta.modules.AccountsPayable.dto.PaymentDetails.PaymentDetailsUpdateDTO;
import com.nubixconta.modules.AccountsPayable.entity.PaymentDetails;
import com.nubixconta.modules.AccountsPayable.repository.AccountsPayableRepository;
import com.nubixconta.modules.AccountsPayable.service.PaymentDetailsService;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailResponseDTO;
import com.nubixconta.modules.accountsreceivable.dto.collectiondetail.CollectionDetailUpdateDTO;
import com.nubixconta.modules.accountsreceivable.entity.CollectionDetail;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

                   //TODO: Falta  service.recalcularBalancePorReceivableId(receivableId);
                    // Mapear la entidad actualizada al DTO antes de devolverla
                    PaymentDetailsResponseDTO responseDto = modelMapper.map(actualizado, PaymentDetailsResponseDTO.class);
                    return ResponseEntity.ok(responseDto);
                })
                .orElse(ResponseEntity.notFound().build());
    }


}
