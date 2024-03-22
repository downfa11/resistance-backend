package com.ns.dedicated.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)

public class FindBoardCommand extends SelfValidating<FindBoardCommand> {

    private final Long boardId;
}