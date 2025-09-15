package com.nubixconta.modules.administration.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
    @JoinColumn(name = "user_id")
    private User user;



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


    @Column(name = "company_status")
    private Boolean companyStatus;


    @Column(name = "active_status")
    private Boolean activeStatus;

    @NotNull(message = "La fecha es obligatorio")
    @Column(name = "creation_date",nullable = false)
    private LocalDateTime creationDate;

    @Size(max = 512, message = "El turn no puede superar los 512 caracteres")
    @Column(name = "turn_company", columnDefinition = "TEXT")
    private String turnCompany;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 100, message = "La dirección puede tener máximo 100 caracteres")
    @Column(name = "address", length = 100, nullable = false)
    private String address;


    @Size(max = 255, message = "La url de la imagen puede tener máximo 255 caracteres")
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ChangeHistory> changeHistories;

    @PrePersist
    protected void onCreate() {
        if (companyStatus == null) {
            companyStatus = false; // Estableciendo a false (empresa no asignada) por defecto
        }
        if (activeStatus == null) {
            activeStatus = true; // Estableciendo a true (empresa activa) una empresa activa por defecto
        }
    }
}
