package com.ns.dedicated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.dedicated.adpater.out.persistance.BoardJpaEntity;
import com.ns.dedicated.adpater.out.persistance.BoardMapper;
import com.ns.dedicated.application.port.in.command.DeleteBoardCommand;
import com.ns.dedicated.application.port.in.command.FindBoardCommand;
import com.ns.dedicated.application.port.in.command.ModifyBoardCommand;
import com.ns.dedicated.application.port.in.command.RegisterBoardCommand;
import com.ns.dedicated.application.port.out.DeleteBoardPort;
import com.ns.dedicated.application.port.out.FindBoardPort;
import com.ns.dedicated.application.port.out.ModifyBoardPort;
import com.ns.dedicated.application.port.out.RegisterBoardPort;
import com.ns.dedicated.application.service.BoardService;
import com.ns.dedicated.domain.Board;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class BoardServiceTest {

    @InjectMocks private BoardService boardService;
    @Mock private RegisterBoardPort registerBoardPort;
    @Mock private ModifyBoardPort modifyBoardPort;
    @Mock private DeleteBoardPort deleteBoardPort;
    @Mock private FindBoardPort findBoardPort;
    @Mock private BoardMapper boardMapper;

    private Board board;


    @BeforeEach
    void init() {
        Board.BoardId boardId = new Board.BoardId(1L);
        Board.BoardTitle title = new Board.BoardTitle("title");
        Board.BoardContents contents = new Board.BoardContents("contents");
        Board.BoardHits hits = new Board.BoardHits(100L);
        Board.BoardCreatedAt createdAt = new Board.BoardCreatedAt(new Timestamp(System.currentTimeMillis()));
        Board.BoardUpdatedAt updatedAt = new Board.BoardUpdatedAt(new Timestamp(System.currentTimeMillis()));

        board = Board.generateBoard(boardId, title, contents, hits, createdAt, updatedAt);
    }


    @Test
    void 게시글을_등록하는_메서드() {
        // given
        RegisterBoardCommand command = RegisterBoardCommand.builder()
                .title("title")
                .contents("contents")
                .build();

        BoardJpaEntity jpaEntity = mock(BoardJpaEntity.class);

        when(registerBoardPort.createBoard(any(), any())).thenReturn(jpaEntity);
        when(boardMapper.mapToDomainEntity(jpaEntity)).thenReturn(board);

        // when
        Board result = boardService.registerBoard(command);

        // then
        assertNotNull(result);
        assertEquals("title", result.getTitle());
        assertEquals("contents", result.getContents());
        verify(registerBoardPort).createBoard(any(), any());
        verify(boardMapper).mapToDomainEntity(jpaEntity);
    }

    @Test
    void 게시글을_수정하는_메서드() {
        // given
        ModifyBoardCommand command = ModifyBoardCommand.builder()
                .boardId(1L)
                .title("title2")
                .contents("contents2")
                .build();

        BoardJpaEntity jpaEntity = mock(BoardJpaEntity.class);

        when(modifyBoardPort.modifyBoard(any(), any(), any())).thenReturn(jpaEntity);
        when(boardMapper.mapToDomainEntity(jpaEntity)).thenReturn(board);

        // when
        Board result = boardService.modifyBoard(command);

        // then
        assertNotNull(result);
        assertEquals("title2", result.getTitle());
        assertEquals("contents2", result.getContents());
        verify(modifyBoardPort).modifyBoard(any(), any(), any());
        verify(boardMapper).mapToDomainEntity(jpaEntity);
    }

    @Test
    void 게시글을_삭제하는_메서드() {
        // given
        Long boardId = 1L;
        DeleteBoardCommand command = DeleteBoardCommand.builder()
                .boardId(boardId)
                .build();

        // when
        boardService.deleteBoard(command);

        // then
        verify(deleteBoardPort).deleteBoard(boardId);
    }

    @Test
    void 게시글을_조회하는_메서드() {
        // given
        FindBoardCommand command = FindBoardCommand.builder()
                .boardId(1L)
                .build();

        BoardJpaEntity jpaEntity = mock(BoardJpaEntity.class);

        when(findBoardPort.findBoard(any())).thenReturn(jpaEntity);
        when(boardMapper.mapToDomainEntity(jpaEntity)).thenReturn(board);

        // when
        Board result = boardService.findBoard(command);

        // then
        assertNotNull(result);
        assertEquals("title", result.getTitle());
        assertEquals("contents", result.getContents());
        verify(findBoardPort).findBoard(any());
        verify(boardMapper).mapToDomainEntity(jpaEntity);
    }

    @Test
    void 모든_게시글을_조회하는_메서드() {
        // given
        List<BoardJpaEntity> jpaEntities = List.of(mock(BoardJpaEntity.class), mock(BoardJpaEntity.class));
        List<Board> boards = List.of(board);

        when(findBoardPort.findBoardsAll(anyLong())).thenReturn(jpaEntities);
        when(boardMapper.mapToDomainEntity(any(BoardJpaEntity.class))).thenReturn(boards.get(0));

        // when
        List<Board> result = boardService.getBoardsAll(0L);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("title", result.get(0).getTitle());
        verify(findBoardPort).findBoardsAll(anyLong());
        verify(boardMapper, times(2)).mapToDomainEntity(any(BoardJpaEntity.class));
    }

    @Test
    void 가장_최근에_올라온_게시글을_조회하는_메서드() {
        // given
        BoardJpaEntity jpaEntity = mock(BoardJpaEntity.class);
        when(findBoardPort.findTopByOrderByCreatedAtDesc()).thenReturn(jpaEntity);
        when(boardMapper.mapToDomainEntity(jpaEntity)).thenReturn(board);

        // when
        String result = boardService.findLatestPostTimeStamp();

        // then
        assertNotNull(result);
        verify(findBoardPort).findTopByOrderByCreatedAtDesc();
        verify(boardMapper).mapToDomainEntity(jpaEntity);
    }
}
