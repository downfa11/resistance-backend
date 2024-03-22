package com.ns.business.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)

public class FindUserDataCommand extends SelfValidating<FindUserDataCommand> {

    private final Long userId;
}