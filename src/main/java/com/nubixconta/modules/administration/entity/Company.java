package com.nubixconta.modules.administration.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer companyId;

    private String companyName;
    private String companyDui;
    private String companyNit;
    private String companyNrc;
    private LocalDateTime creationDate;

    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
}
