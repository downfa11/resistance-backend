package com.ns.business.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)

public class FindUserDataCommand extends SelfValidating<FindUserDataCommand> {

    private final Long userId;
}