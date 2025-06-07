package com.nubixconta.modules.administration;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
//@Table(name="nombreTablaEnBD")
//estas 2 anotaciones sirven para no tener que escribir los getter y setter
@Getter
@Setter
public class Administration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: Agregar campos como nombreUsuario, rol, empresaAsignada, etc.
}
