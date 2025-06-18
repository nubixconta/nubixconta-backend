package com.nubixconta.modules.administration.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ChangeHistory {
@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer historyId;

    private LocalDateTime date;
    private String actionPerformed;
    private String module;

    @ManyToOne
    @JoinColumn(name="client_id")
    private User client;

    @ManyToOne
    @JoinColumn(name="company_id")
    private Company company;
}
