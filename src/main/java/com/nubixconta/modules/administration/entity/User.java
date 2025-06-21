package com.nubixconta.modules.administration.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "user")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer id;

    @NotNull(message = "El nombre es obligatorio")
    @Column(name = "first_name", length = 50)
    private String firstName;

    @NotNull(message = "El apellido es obligatorio")
    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotNull(message = "El correo es obligatorio")
    @Column(length = 100)
    private String email;

    @NotNull(message = "La contraseña es obligatorio")
    @Column(name = "pass_word", length = 255)
    private String password;


    @Column(length = 255)
    private String photo;

    @NotNull(message = "El estado es obligatorio")
    @Column
    private Boolean status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Company> companies;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<ChangeHistory> changesMade;

    // Getters y setters...
}
