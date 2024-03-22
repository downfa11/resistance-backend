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

public class RegisterBoardCommand extends SelfValidating<RegisterBoardCommand> {

    @NotNull
    private final String title;
    @NotNull
    private final String contents;

    public RegisterBoardCommand(String title, String contents) {

        this.title = title;
        this.contents = contents;
        this.validateSelf();
    }
}

