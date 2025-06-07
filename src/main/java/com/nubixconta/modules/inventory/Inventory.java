package com.nubixconta.modules.inventory;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
//@Table(name="nombreTablaEnBD")
//estas 2 anotaciones sirven para no tener que escribir los getter y setter
@Getter
@Setter
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: Agregar campos como códigoProducto, nombre, cantidad, precioUnitario, etc.
}
