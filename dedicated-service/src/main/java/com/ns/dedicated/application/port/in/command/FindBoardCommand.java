package com.ns.dedicated.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)

public class FindBoardCommand extends SelfValidating<FindBoardCommand> {
    private final Long boardId;
}