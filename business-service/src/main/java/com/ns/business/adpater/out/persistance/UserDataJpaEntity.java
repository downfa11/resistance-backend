package com.ns.business.adpater.out.persistance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name ="userDatas")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDataJpaEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private String name;
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


    public UserDataJpaEntity(Long userId, String name,int gold, int highscore, int energy, int scenario, String head, String body, String arm, int health, int attack, int critical, int durability) {
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
    }

    @Override
    public String toString() {
        return "UserDataJpaEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", gold=" + gold +
                ", highscore=" + highscore +
                ", energy=" + energy +
                ", scenario='" + scenario + '\'' +
                ", head='" + head + '\'' +
                ", body='" + body + '\'' +
                ", arm='" + arm + '\'' +
                ", health=" + health +
                ", attack=" + attack +
                ", critical=" + critical +
                ", durability=" + durability +
                '}';
    }
}
