package com.nubixconta.modules.AccountsPayable.dto.PaymentDetails;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDetailsResponseDTO {
    private Integer id;
    private String paymentStatus;
    private String paymentDetailDescription;
    private LocalDateTime paymentDetailsDate;
    private String paymentMethod;
    private BigDecimal paymentAmount;

    // Constructor sin argumentos (necesario para ModelMapper)
    public PaymentDetailsResponseDTO() {
    }
    public PaymentDetailsResponseDTO(Integer id, String paymentStatus, String paymentDetailsDescription,
                                     LocalDateTime paymentDate, String paymentMethod, BigDecimal paymentAmount) {
        this.id = id;
        this.paymentStatus = paymentStatus;
        this.paymentDetailDescription = paymentDetailsDescription;
        this.paymentDetailsDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.paymentAmount = paymentAmount;
    }
}
