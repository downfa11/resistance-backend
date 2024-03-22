package com.ns.dedicated.adpater.in.web;

import com.ns.dedicated.adpater.in.web.dto.AddBoardRequest;
import com.ns.dedicated.adpater.in.web.dto.UpdateBoardRequest;
import com.ns.dedicated.application.port.in.DeleteBoardUseCase;
import com.ns.dedicated.application.port.in.FindBoardUseCase;
import com.ns.dedicated.application.port.in.ModifyBoardUseCase;
import com.ns.dedicated.application.port.in.RegisterBoardUseCase;
import com.ns.dedicated.application.port.in.command.DeleteBoardCommand;
import com.ns.dedicated.application.port.in.command.FindBoardCommand;
import com.ns.dedicated.application.port.in.command.ModifyBoardCommand;
import com.ns.dedicated.application.port.in.command.RegisterBoardCommand;
import com.ns.dedicated.domain.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@RestController
@RequiredArgsConstructor
@RequestMapping(path="/board")
public class BoardController {

    private final RegisterBoardUseCase registerBoardUseCase;
    private final ModifyBoardUseCase modifyBoardUseCase;
    private final FindBoardUseCase findBoardUseCase;
    private final DeleteBoardUseCase deleteBoardUseCase;

    @PostMapping("/add")
    public Board add(@RequestBody AddBoardRequest request) {
        RegisterBoardCommand command = RegisterBoardCommand.builder()
                .title(request.getTitle())
                .contents(request.getContents())
                .build();

        return registerBoardUseCase.registerBoard(command);
    }

    @PostMapping("/add/temp")
    public Board addTemp() {
        Random random = new Random();

        RegisterBoardCommand command = RegisterBoardCommand.builder()
                .title("test title: " + random.nextInt(1000))
                .contents("test contents: " + random.nextInt(1000))
                .build();

        return registerBoardUseCase.registerBoard(command);
    }

    @PatchMapping("/update")
    public ResponseEntity<Board> updateBoard(@RequestBody UpdateBoardRequest request) {
        ModifyBoardCommand command = ModifyBoardCommand.builder()
                .boardId(request.getBoardId())
                .title(request.getTitle())
                .contents(request.getContents())
                .build();

        return ResponseEntity.ok(modifyBoardUseCase.modifyBoard(command));
    }


    @GetMapping(path="/{boardId}")
    ResponseEntity<Board> findBoardByBaordId(@PathVariable Long boardId){

        FindBoardCommand command = FindBoardCommand.builder()
                .boardId(boardId)
                .build();

        return ResponseEntity.ok(findBoardUseCase.findBoard(command));
    }

    @GetMapping("/list/{lastboardId}")
    public ResponseEntity<List<Board>> getBoardsAll(@PathVariable Long lastboardId) {
        List<Board> boards = findBoardUseCase.getBoardsAll(lastboardId);
        return ResponseEntity.ok(boards);
    }

    @DeleteMapping("{boardId}")
    void deleteBoardByBoardId(@PathVariable Long boardId){
        DeleteBoardCommand command = DeleteBoardCommand.builder()
        .boardId(boardId).build();

        deleteBoardUseCase.deleteBoard(command);
    }

}
