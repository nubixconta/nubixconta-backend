package com.nubixconta.modules.administration.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.nubixconta.modules.sales.entity.Customer;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Integer id;

    @NotNull(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El firstName no puede tener mas de 50 caracteres")
    @Column(name = "first_name", length = 50)
    private String firstName;

    @NotNull(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El lastName  no puede tener mas de 50 caracteres")
    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotNull(message = "El userName es obligatorio")
    @Size(max = 32, message = "La userName no puede tener mas de 32 caracteres")
    @Column(name = "user_name", length = 32, unique = true)
    private String userName;

    @NotNull(message = "El correo es obligatorio")
    @Size(max = 50, message = "La email no puede tener mas de 50 caracteres")
    @Email(message = "El correo no tiene un formato válido")
    @Column(length = 50,unique = true)
    private String email;

    @NotNull(message = "La contraseña es obligatorio")
    @Column(name = "pass_word", length = 255)
    private String password;


    @Column(length = 255)
    private String photo;


    @Column(name="user_status")
    private Boolean status;

    @NotNull(message = "El estado es obligatorio")
    @Column(name="role")
    private Boolean role;


    @JsonManagedReference
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Company> companies;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ChangeHistory> changesMade;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Customer> customers;
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = true; // Estableciendo a true (usuario se encuentra en estado activo por defecto)
        }
    }
}
