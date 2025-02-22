package com.ns.dedicated.adpater.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AddBoardRequest {
    private String title;
    private String contents;
}