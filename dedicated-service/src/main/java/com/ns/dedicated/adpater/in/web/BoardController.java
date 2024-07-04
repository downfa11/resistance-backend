package com.ns.dedicated.adpater.in.web;

import com.ns.dedicated.adpater.in.web.dto.AddBoardRequest;
import com.ns.dedicated.adpater.in.web.dto.BoardList;
import com.ns.dedicated.adpater.in.web.dto.UpdateBoardRequest;
import com.ns.dedicated.adpater.out.JwtTokenProvider;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path="/board")
public class BoardController {

    private final RegisterBoardUseCase registerBoardUseCase;
    private final ModifyBoardUseCase modifyBoardUseCase;
    private final FindBoardUseCase findBoardUseCase;
    private final DeleteBoardUseCase deleteBoardUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/add")
    public Board add(@RequestBody AddBoardRequest request) {
        Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        RegisterBoardCommand command = RegisterBoardCommand.builder()
                .title(request.getTitle())
                .contents(request.getContents())
                .build();

        return registerBoardUseCase.registerBoard(command);
    }

    @PostMapping("/add/temp")
    public Board addTemp() {
        Long memberId = jwtTokenProvider.getMembershipIdbyToken();
        Random random = new Random();

        RegisterBoardCommand command = RegisterBoardCommand.builder()
                .title("test title: " + random.nextInt(1000))
                .contents("test contents: " + random.nextInt(1000))
                .build();

        return registerBoardUseCase.registerBoard(command);
    }

    @PatchMapping("/update")
    public ResponseEntity<Board> updateBoard(@RequestBody UpdateBoardRequest request) {
        Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        ModifyBoardCommand command = ModifyBoardCommand.builder()
                .boardId(request.getBoardId())
                .title(request.getTitle())
                .contents(request.getContents())
                .build();

        return ResponseEntity.ok(modifyBoardUseCase.modifyBoard(command));
    }


    @GetMapping(path="/{boardId}")
    ResponseEntity<Board> findBoardByBaordId(@PathVariable Long boardId){
        //Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        FindBoardCommand command = FindBoardCommand.builder()
                .boardId(boardId)
                .build();

        return ResponseEntity.ok(findBoardUseCase.findBoard(command));
    }

    @GetMapping("/list/{offset}")
    public ResponseEntity<BoardList> getBoardsAll(@PathVariable Long offset) {
        // Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        List<Board> boards = findBoardUseCase.getBoardsAll(offset);
        return ResponseEntity.ok(
                BoardList.builder()
                        .boards(boards).build());
    }

    @DeleteMapping("{boardId}")
    void deleteBoardByBoardId(@PathVariable Long boardId){
        Long memberId = jwtTokenProvider.getMembershipIdbyToken();

        DeleteBoardCommand command = DeleteBoardCommand.builder()
        .boardId(boardId).build();

        deleteBoardUseCase.deleteBoard(command);
    }

    @GetMapping("/polling")
    public ResponseEntity<String> PostPolling(){

        return ResponseEntity.ok().body(findBoardUseCase.findLatestPostTimeStamp());
    }
}
