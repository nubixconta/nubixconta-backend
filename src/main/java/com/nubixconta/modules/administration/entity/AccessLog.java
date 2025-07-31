package com.nubixconta.modules.administration.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_log")
@Data
public class AccessLog {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_log_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({
            "email",
            "password",
            "photo",
            "status",
            "role",
            "firstName",
            "lastName",
            "userName",
            "companies",
            "changesMade",
            "accessLogs",
            "customers"
    })
    private User user;

    @NotNull(message = "La fecha es obligatorio")
    @Column(name="date_start")
    private LocalDateTime dateStart;

    @Column(name="date_end")
    private LocalDateTime dateEnd;

    public AccessLog(User user, LocalDateTime dateStart) {
        this.user = user;
        this.dateStart = dateStart;
    }

    public AccessLog() {

    }

}
