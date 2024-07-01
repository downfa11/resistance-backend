package com.ns.dedicated.adpater.in.web.dto;

import com.ns.dedicated.domain.Board;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BoardList {
    List<Board> boards;
}
