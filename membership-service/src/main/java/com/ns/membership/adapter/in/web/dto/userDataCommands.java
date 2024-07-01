package com.ns.membership.adapter.in.web.dto;

import com.ns.membership.domain.userData;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class userDataCommands {
    private List<userData> userDataCommandList;
}
