package com.ns.dedicated.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;

@Builder
@Data
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
