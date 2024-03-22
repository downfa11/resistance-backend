package com.ns.business.adpater.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModifyUserDataRequest {
    private Long userId;
    private int gold;
    private int highscore;
    private int energy;
    private int scenario;
    private String head;
    private String body;
    private String arm;
    private int health;
    private int attack;
    private int critical;
    private int durability;
}





