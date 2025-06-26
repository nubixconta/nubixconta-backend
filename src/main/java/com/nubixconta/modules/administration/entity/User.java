package com.nubixconta.modules.administration.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    @Column(name = "first_name", length = 50)
    private String firstName;

    @NotNull(message = "El apellido es obligatorio")
    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotNull(message = "El userName es obligatorio")
    @JsonProperty("userName") // <- Esta línea es clave
    @Column(name = "user_name", length = 32, unique = true)
    private String userName;

    @NotNull(message = "El correo es obligatorio")
    @Column(length = 100)
    private String email;

    @NotNull(message = "La contraseña es obligatorio")
    @JsonProperty("password") // <- Esta línea también es necesaria
    @Column(name = "pass_word", length = 255)
    private String password;

    @Column(length = 255)
    private String photo;

    // ... imports y anotaciones de clase
    @Column(name = "user_status", nullable = false)
    private Boolean status = Boolean.TRUE; // <- default

    @Column(name = "role", nullable = false)
    private Boolean role = Boolean.FALSE; // <- default


    @JsonManagedReference
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Company> companies;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ChangeHistory> changesMade;

}
