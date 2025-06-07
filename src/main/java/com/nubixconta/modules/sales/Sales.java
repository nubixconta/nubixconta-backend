package com.nubixconta.modules.sales;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
//@Table(name="nombreTablaEnBD")
//estas 2 anotaciones sirven para no tener que escribir los getter y setter
@Getter
@Setter
public class Sales {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: Agregar campos necesarios (cliente, total, fecha, etc.)

    // TODO: Agregar relaciones (si aplica)

}
