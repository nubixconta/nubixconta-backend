package com.nubixconta.modules.accountsreceivable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
//@Table(name="nombreTablaEnBD")
//estas 2 anotaciones sirven para no tener que escribir los getter y setter
@Getter
@Setter
public class AccountsReceivable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: Agregar campos como cliente, documento, monto, fechaVencimiento, etc.
}
