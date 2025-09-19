package com.nubixconta.modules.AccountsPayable.entity;
import com.nubixconta.modules.administration.entity.Company;
import com.nubixconta.modules.purchases.entity.Purchase;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts_payable")
@Data
public class AccountsPayable {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "account_payable_id")
        private Integer id;

        @NotNull(message = "La compra es obligatoria")
        @Column(name = "purcharse_id", nullable = false)
        private Integer purcharseId;

        // --- ¡Añadido el campo de la empresa! ---
        @NotNull(message = "La empresa es obligatoria")
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "company_id", nullable = false)
        private Company company;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "purcharse_id", insertable = false, updatable = false)
        private Purchase purcharse;

        @NotNull(message = "El saldo es obligatorio")
        @Column(precision = 10, scale = 2)
        @Digits(integer = 8, fraction = 2, message = "El saldo puede tener hasta 8 dígitos enteros y 2 decimales")
        @DecimalMin(value="0.00",inclusive=true,message = "El Saldo no puede ser negativo")
        private BigDecimal balance;

        @NotNull(message = "El modulo es obligatorio")
        @Size(max = 30, message = "La el modulo no puede tener mas de 30 caracteres")
        @Column(name = "module_type", length = 30)
        private String moduleType;

        @OneToMany(mappedBy = "accountsPayable", cascade = CascadeType.ALL)
        private List<PaymentDetails> paymentDetails;

        public void addCollectionDetail(PaymentDetails detail) {
            if (this.paymentDetails == null) {
                this.paymentDetails = new ArrayList<>();
            }
            this.paymentDetails.add(detail);
            detail.setAccountsPayable(this);
        }

        public void removeCollectionDetail(PaymentDetails detail) {
            if (this.paymentDetails != null) {
                this.paymentDetails.remove(detail);
                detail.setAccountsPayable(null);
            }
        }


    }

