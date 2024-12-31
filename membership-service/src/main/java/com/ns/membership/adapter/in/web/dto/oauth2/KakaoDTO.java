package com.ns.membership.adapter.in.web.dto.oauth2;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class KakaoDTO {

    private long id;
    private String email;
    private String nickname;
    private String thumbnailImageUrl;

}