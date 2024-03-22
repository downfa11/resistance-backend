package com.ns.business.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)

public class RegisterUserDataCommand extends SelfValidating<RegisterUserDataCommand> {

    @NotNull private final Long userId;
    @NotNull private final String name;
    @NotNull private final int gold;
    @NotNull private final int highscore;
    @NotNull private final int energy;
    @NotNull private final int scenario;
    @NotNull private final String head;
    @NotNull private final String body;
    @NotNull private final String arm;
    @NotNull private final int health;
    @NotNull private final int attack;
    @NotNull private final int critical;
    @NotNull private final int durability;

    public RegisterUserDataCommand(Long userId, String name, int gold, int highscore, int energy, int scenario, String head, String body, String arm, int health, int attack, int critical, int durability) {
        this.userId = userId;
        this.name = name;
        this.gold = gold;
        this.highscore = highscore;
        this.energy = energy;
        this.scenario = scenario;
        this.head = head;
        this.body = body;
        this.arm = arm;
        this.health = health;
        this.attack = attack;
        this.critical = critical;
        this.durability = durability;

        this.validateSelf();
    }
}

