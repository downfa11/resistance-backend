package com.ns.dedicated.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)

public class ModifyBoardCommand extends SelfValidating<ModifyBoardCommand> {

    @NotNull
    private final Long boardId;
    @NotNull
    private final String title;
    @NotNull
    private final String contents;


    public ModifyBoardCommand(Long boardId, String title, String contents) {
        this.boardId = boardId;
        this.title = title;
        this.contents = contents;
        this.validateSelf();
    }


}
