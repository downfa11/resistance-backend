package com.ns.dedicated.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import java.sql.Timestamp;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Board {
    @Getter private Long boardId;
    @Getter private String title;
    @Getter private String contents;
    @Getter private Long hits;
    @Getter private Timestamp createdAt;
    @Getter private Timestamp updatedAt;

    public static Board generateBoard(BoardId boardId, BoardTitle title, BoardContents contents, BoardHits hits,
                                      BoardCreatedAt createdAt, BoardUpdatedAt updatedAt) {
        return new Board(boardId.getValue(), title.getValue(), contents.getValue(), hits.getValue(),
                createdAt.getValue(), updatedAt.getValue());
    }

    @Value
    public static class BoardId {
        private Long value;

        public BoardId(Long value) {
            this.value = value;
        }
    }

    @Value
    public static class BoardTitle {
        private String value;

        public BoardTitle(String value) {
            this.value = value;
        }
    }

    @Value
    public static class BoardContents {
        private String value;

        public BoardContents(String value) {
            this.value = value;
        }
    }

    @Value
    public static class BoardHits {
        private Long value;

        public BoardHits(Long value) {
            this.value = value;
        }
    }

    @Value
    public static class BoardCreatedAt {
        private Timestamp value;

        public BoardCreatedAt(Timestamp value) {
            this.value = value;
        }
    }

    @Value
    public static class BoardUpdatedAt {
        private Timestamp value;

        public BoardUpdatedAt(Timestamp value) {
            this.value = value;
        }
    }
}

