package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.administration.entity.Company;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "cierre_mensual", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"company_id", "anio", "mes"})
})
@Data
@NoArgsConstructor
public class CierreMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int mes;

    // Almacenamos el último día del mes para facilitar las consultas
    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @Column(nullable = false)
    private boolean cerrado = false;

    public CierreMensual(Company company, int anio, int mes) {
        this.company = company;
        this.anio = anio;
        this.mes = mes;
        this.fechaCierre = LocalDate.of(anio, mes, 1).withDayOfMonth(
                LocalDate.of(anio, mes, 1).lengthOfMonth()
        );
    }
}