package com.nubixconta.modules.administration.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
        @JoinColumn(name = "user_id", nullable = false)
        @JsonIgnoreProperties({
                "email",
                "password",
                "photo",
                "status",
                "role"
        })
        private User user;

        @ManyToOne
        @JoinColumn(name = "company_id", nullable = true)
        @JsonIgnoreProperties({
                "accountId",
                "companyStatus",
                "activeStatus",
                "creationDate"
        })
        private Company company;


        @NotNull(message = "La fecha es obligatorio")
        @Column(name="date")
        private LocalDateTime date;

        @Size(max = 3000, message = "La descripción no puede superar los 2000 caracteres")
        @Column(name = "action_performed", columnDefinition = "TEXT")
        private String actionPerformed;

        @NotNull(message = "El modulo es obligatorio")
        @Column(length = 50)
        private String moduleName;


}

