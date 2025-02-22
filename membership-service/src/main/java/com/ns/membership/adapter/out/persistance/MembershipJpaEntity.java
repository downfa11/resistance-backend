package com.ns.membership.adapter.out.persistance;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name ="membership")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MembershipJpaEntity {

    @Id
    @GeneratedValue
    private Long membershipId;

    private String name;
    private String account;
    private String password;
    private String address;
    private String email;

    private boolean isValid;

    @ElementCollection
    private Set<Long> friends;

    @ElementCollection
    private Set<Long> wantedFriends;

    private String refreshToken;
    private String role;


    private String provider;
    private String providerId;

    public MembershipJpaEntity(String name, String account, String password, String address, String email, boolean isValid, Set<Long> friends, Set<Long> wantedFriends, String refreshToken, String role, String provider, String providerId) {
        this.name = name;
        this.account=account;
        this.password=password;
        this.address = address;
        this.email = email;
        this.isValid = isValid;
        this.friends = friends;
        this.wantedFriends = wantedFriends;
        this.refreshToken = refreshToken;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
    }

    @Override
    public String toString() {
        return "MembershipJpaEntity{" +
                "membershipId=" + membershipId +
                ", name='" + name + '\'' +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", address='" + address + '\'' +
                ", email='" + email + '\'' +
                ", isValid=" + isValid +
                ", friends=" + friends +
                ", wantedFriends=" + wantedFriends +
                ", refreshToken='" + refreshToken + '\'' +
                ", role='" + role + '\'' +
                ", provider='" + provider + '\'' +
                ", providerId='" + providerId + '\'' +
                '}';
    }
}
