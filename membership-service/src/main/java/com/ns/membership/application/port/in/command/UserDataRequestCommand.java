package com.ns.membership.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
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
