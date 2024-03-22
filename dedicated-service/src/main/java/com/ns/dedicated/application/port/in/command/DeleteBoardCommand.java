package com.ns.dedicated.application.port.in.command;

import com.ns.common.SelfValidating;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)

public class DeleteBoardCommand extends SelfValidating<DeleteBoardCommand> {

    private final Long boardId;
}