package com.ns.dedicated.adpater.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddBoardRequest {
    private String title;
    private String contents;
}