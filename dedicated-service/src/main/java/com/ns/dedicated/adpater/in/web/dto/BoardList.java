package com.ns.dedicated.adpater.in.web.dto;

import com.ns.dedicated.domain.Board;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardList {
    List<Board> boards;
}
