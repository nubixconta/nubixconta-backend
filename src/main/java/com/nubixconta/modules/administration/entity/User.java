package com.nubixconta.modules.administration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer userId;
    private String lastName;
    private String username;
    private String password;
    private String photo;
    private String firstName;
    private Boolean role;
    private Boolean status;
}
