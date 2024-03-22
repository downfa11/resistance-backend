package com.ns.dedicated.application.port.in;


import com.ns.dedicated.application.port.in.command.RegisterBoardCommand;
import com.ns.dedicated.domain.Board;

public interface RegisterBoardUseCase {
    Board registerBoard(RegisterBoardCommand command);
}
