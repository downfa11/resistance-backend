package com.ns.dedicated.application.port.in;


import com.ns.dedicated.application.port.in.command.DeleteBoardCommand;
import com.ns.dedicated.application.port.in.command.FindBoardCommand;
import com.ns.dedicated.domain.Board;

import java.util.List;

public interface DeleteBoardUseCase {
    void deleteBoard(DeleteBoardCommand command);
}
