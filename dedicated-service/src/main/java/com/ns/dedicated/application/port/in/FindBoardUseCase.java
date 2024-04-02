package com.ns.dedicated.application.port.in;


import com.ns.dedicated.application.port.in.command.FindBoardCommand;
import com.ns.dedicated.domain.Board;

import java.util.List;

public interface FindBoardUseCase {
    Board findBoard(FindBoardCommand command);
    List<Board> getBoardsAll(int offset);
}
