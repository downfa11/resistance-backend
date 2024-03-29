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
import java.util.Set;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
public class UserDataRequestCommand extends SelfValidating<UserDataRequestCommand> {

    @NotNull private final String membershipId;

    @NotNull
    private final Set<Long> targetIdList;

    public UserDataRequestCommand(String membershipId,Set<Long> targetIdList) {
        this.membershipId=membershipId;
        this.targetIdList = targetIdList;
        this.validateSelf();
    }


}
