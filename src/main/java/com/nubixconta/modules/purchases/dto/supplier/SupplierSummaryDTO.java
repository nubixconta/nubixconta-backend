package com.nubixconta.modules.purchases.dto.supplier;

import lombok.Data;

@Data
public class SupplierSummaryDTO {
    private Integer idSupplier;
    private String supplierName;
    private String supplierLastName;
    private String supplierDui;
    private String supplierNit;
    private Integer creditDay;
}