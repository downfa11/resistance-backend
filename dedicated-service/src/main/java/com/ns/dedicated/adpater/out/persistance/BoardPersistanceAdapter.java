package com.ns.dedicated.adpater.out.persistance;


import com.ns.dedicated.adpater.out.persistance.BoardJpaEntity;
import com.ns.common.PersistanceAdapter;
import com.ns.dedicated.application.port.out.FindBoardPort;
import com.ns.dedicated.application.port.out.ModifyBoardPort;
import com.ns.dedicated.application.port.out.RegisterBoardPort;
import com.ns.dedicated.application.port.out.DeleteBoardPort;
import com.ns.dedicated.domain.Board;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@PersistanceAdapter
@Slf4j
public class BoardPersistanceAdapter implements RegisterBoardPort, FindBoardPort, ModifyBoardPort, DeleteBoardPort {

    private final BoardRepository boardRepository;


    @Override
    public BoardJpaEntity createBoard(Board.BoardTitle boardTitle, Board.BoardContents boardContents) {

        if (boardContents == null || boardContents.getValue().equals(""))
            return null;
        if (boardTitle == null || boardTitle.getValue().equals(""))
            return null;

        BoardJpaEntity jpaEntity = new BoardJpaEntity(
                boardTitle.getValue(),
                boardContents.getValue(),
                0L,
                new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis())
        );
        boardRepository.save(jpaEntity);

        return new BoardJpaEntity(
                jpaEntity.getBoardId(),
                boardTitle.getValue(),
                boardContents.getValue(),
                jpaEntity.getHits(),
                jpaEntity.getCreatedAt(),
                jpaEntity.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public BoardJpaEntity modifyBoard(Board.BoardId boardId, Board.BoardTitle boardTitle, Board.BoardContents boardContents) {

        if (boardContents == null || boardContents.getValue().equals(""))
            return null;
        if (boardTitle == null || boardTitle.getValue().equals(""))
            return null;

        BoardJpaEntity jpaEntity = boardRepository.getById(boardId.getValue());
        jpaEntity.setTitle(boardTitle.getValue());
        jpaEntity.setContents(boardContents.getValue());
        jpaEntity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        boardRepository.save(jpaEntity);


        return new BoardJpaEntity(
                jpaEntity.getBoardId(),
                boardTitle.getValue(),
                boardContents.getValue(),
                jpaEntity.getHits(),
                jpaEntity.getCreatedAt(),
                jpaEntity.getUpdatedAt()
        );
    }


    @Override
    public void deleteBoard(Long boardId) {
        Optional<BoardJpaEntity> findBoard = boardRepository.findById(boardId);
        if (findBoard.isPresent()) {
            BoardJpaEntity board = findBoard.get();
            boardRepository.delete(board);
        } else {
            throw new RuntimeException("Board is not exist.");
        }
    }

    @Override
    public BoardJpaEntity findBoard(Board.BoardId boardId) {
        BoardJpaEntity findBoard = boardRepository.getById(boardId.getValue());
        if (findBoard!=null) {
            findBoard.setHits(findBoard.getHits() + 1); // 조회수 증가
            boardRepository.save(findBoard);
        }
        else throw new RuntimeException("boards is not exist.");

        return findBoard;
    }

    @Override
    public List<BoardJpaEntity> findBoardsAll(Long offset) {
        int pageSize = 10;

        int pageNumber = (offset != null) ? offset.intValue() : 0;
        Page<BoardJpaEntity> boards = fetchPagesAll(pageNumber, pageSize);

        if (boards.isEmpty()) {
            return Collections.emptyList();
        }

        return boards.getContent();
    }

    private Page<BoardJpaEntity> fetchPagesAll(int offset, int size) {
        PageRequest pageRequest = PageRequest.of(offset, size, Sort.by("createdAt").descending());
        return boardRepository.findAll(pageRequest);
    }


    @Override
    public BoardJpaEntity findTopByOrderByCreatedAtDesc() {
        Optional<BoardJpaEntity> boardOptional = boardRepository.findLatestBoard();
        return boardOptional.get();
    }

}
