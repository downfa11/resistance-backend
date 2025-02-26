package com.ns.dedicated.application.port.in.command;

import com.ns.common.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
public class RegisterBoardCommand extends SelfValidating<RegisterBoardCommand> {

    @NotNull private final String title;
    @NotNull private final String contents;

    public RegisterBoardCommand(String title, String contents) {

        this.title = title;
        this.contents = contents;
        this.validateSelf();
    }
}

