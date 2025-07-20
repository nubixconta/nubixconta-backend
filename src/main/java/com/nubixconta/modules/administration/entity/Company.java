package com.nubixconta.modules.administration.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "company")
@Data
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer id;

    @JsonBackReference
    @ManyToOne
    @NotNull(message = "El usuaro es obligatorio")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_id")
    private Integer accountId;

    @NotNull(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El companyName no puede tener mas de 50 caracteres")
    @Column(name = "company_name", length = 100, nullable=false, unique = true)
    private String companyName;


    @Size(max = 10, message = "El companyDui no puede tener mas de 50 caracteres")
    @Column(name = "company_dui", length = 10, unique = true)
    private String companyDui;

    @Size(max = 17, message = "El companyNit no puede tener mas de 50 caracteres")
    @Column(name = "company_nit", length = 17, unique = true)
    private String companyNit;

    @Column(name = "company_nrc", length = 14, unique = true)
    private String companyNrc;

    @NotNull(message = "El campo companyStatus es obligatorio")
    @Column(name = "company_status",nullable = false)
    private Boolean companyStatus;

    @NotNull(message = "La activeStatus es obligatorio")
    @Column(name = "active_status",nullable = false)
    private Boolean activeStatus;

    @NotNull(message = "La fecha es obligatorio")
    @Column(name = "creation_date",nullable = false)
    private LocalDateTime creationDate;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ChangeHistory> changeHistories;


}
