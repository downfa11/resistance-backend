package com.ns.dedicated.adpater.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@Builder
public class PollingDto {
    private final Timestamp date;
}
