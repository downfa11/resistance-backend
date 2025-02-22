package com.ns.dedicated.adpater.out.persistance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name ="boards")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoardJpaEntity {

    @Id
    @GeneratedValue
    private Long boardId;

    private String title;
    private String contents;
    private Long hits;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public BoardJpaEntity(String title, String contents, Long hits, Timestamp createdAt, Timestamp updatedAt) {
        this.title = title;
        this.contents = contents;
        this.hits = hits;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "BoardJpaEntity{" +
                "boardId=" + boardId +
                ", title='" + title + '\'' +
                ", contents='" + contents + '\'' +
                ", hits=" + hits +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
