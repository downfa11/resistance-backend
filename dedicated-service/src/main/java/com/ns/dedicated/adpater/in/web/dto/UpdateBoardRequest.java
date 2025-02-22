package com.ns.dedicated.adpater.in.web.dto;


import lombok.Getter;

@Getter
public class UpdateBoardRequest {
    private Long boardId;
    private String title;
    private String contents;
}
