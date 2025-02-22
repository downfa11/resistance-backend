package com.ns.membership.adapter.in.web.dto;

import com.ns.membership.domain.userData;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class userDataCommands {
    private List<userData> userDataCommandList;
}
