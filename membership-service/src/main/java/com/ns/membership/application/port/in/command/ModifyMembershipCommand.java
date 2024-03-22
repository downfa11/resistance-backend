package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import com.ns.membership.domain.userData;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)

public class ModifyMembershipCommand extends SelfValidating<ModifyMembershipCommand> {

    @NotNull
    private final String membershipId;
    @NotNull
    private final String name;
    @NotNull
    private final String email;
    @NotNull
    @NotBlank
    private final String address;

    @AssertTrue
    private final boolean isValid;

    private final List<Long> friends;
    private final List<Long> wantedFriends;

    private final String refreshToken;

    public ModifyMembershipCommand(String membershipId, String name, String email, String address, boolean isValid, List<Long> friends, List<Long> wantedFriends, String refreshToken) {
        this.membershipId = membershipId;
        this.name = name;
        this.email = email;
        this.address = address;
        this.isValid = isValid;
        this.friends = friends;
        this.wantedFriends = wantedFriends;
        this.refreshToken = refreshToken;
        this.validateSelf();
    }

}
