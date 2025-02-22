package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)

public class ModifyMembershipCommand extends SelfValidating<ModifyMembershipCommand> {

    @NotNull
    private final String membershipId;
    @NotNull
    private final String name;
    @NotNull
    private final String account;
    @NotNull
    private final String password;
    @NotNull
    private final String email;
    @NotNull
    @NotBlank
    private final String address;

    @AssertTrue
    private final boolean isValid;

    private final Set<Long> friends;
    private final Set<Long> wantedFriends;

    private final String refreshToken;

    public ModifyMembershipCommand(String membershipId, String name, String account, String password, String email, String address, boolean isValid, Set<Long> friends, Set<Long> wantedFriends, String refreshToken) {
        this.membershipId = membershipId;
        this.name = name;
        this.account=account;
        this.password=password;
        this.email = email;
        this.address = address;
        this.isValid = isValid;
        this.friends = friends;
        this.wantedFriends = wantedFriends;
        this.refreshToken = refreshToken;
        this.validateSelf();
    }

}
