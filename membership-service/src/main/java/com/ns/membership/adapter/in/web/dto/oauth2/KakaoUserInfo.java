package com.ns.membership.adapter.in.web.dto.oauth2;

import java.util.LinkedHashMap;
import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo{

    private Map<String, Object> attributes; // getAttributes()
    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Object object = attributes.get("kakao_account");
        LinkedHashMap accountMap = (LinkedHashMap) object;
        return (String) accountMap.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }
}
