package com.ns.dedicated.application.port.in;


import com.ns.dedicated.application.port.in.command.ModifyBoardCommand;
import com.ns.dedicated.domain.Board;

public interface ModifyBoardUseCase {
    Board modifyBoard(ModifyBoardCommand command);
}
