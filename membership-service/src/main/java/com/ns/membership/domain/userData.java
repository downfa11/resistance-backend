package com.ns.membership.domain;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class userData {

    @Getter private Long membershipId;
    @Getter private String name;
    @Getter private int highScore;

    @Getter private int head;
    @Getter private int body;
    @Getter private int arm;

    @Getter private int health;
    @Getter private int attack;
    @Getter private int critical;
    @Getter private int durability;

}


