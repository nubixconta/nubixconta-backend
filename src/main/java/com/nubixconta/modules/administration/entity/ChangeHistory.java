package com.nubixconta.modules.administration.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_history")
@Data
public class ChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotNull(message = "La fecha es obligatorio")
    @Column
    private LocalDateTime date;

    @NotNull(message = "Es obligatoria la accion que se realizo ")
    @Column(name = "action_performed", length = 100)
    private String actionPerformed;

    @NotNull(message = "El modulo es obligatorio")
    @Column(length = 50)
    private String module;

    // Getters y setters...
}

