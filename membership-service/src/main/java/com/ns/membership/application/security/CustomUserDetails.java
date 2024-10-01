package com.ns.membership.application.security;

import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OAuth2User {

    private final MembershipJpaEntity membershipJpaEntity;
    private Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    // 일반 로그인
    public CustomUserDetails(MembershipJpaEntity membershipJpaEntity) {
        this.membershipJpaEntity = membershipJpaEntity;
    }

    // OAuth 로그인
    public CustomUserDetails(MembershipJpaEntity membershipJpaEntity, Map<String, Object> attributes) {
        this.membershipJpaEntity = membershipJpaEntity;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Collection<GrantedAuthority> collection = new ArrayList<>();
        GrantedAuthority authority = new SimpleGrantedAuthority(membershipJpaEntity.getRole());
        collection.add(authority);

        System.out.println("Granted Authority: " + authority.getAuthority());

        String role = membershipJpaEntity.getRole();
        System.out.println("User Role: " + role);

        return collection;
    }

    @Override
    public String getPassword() {
        return membershipJpaEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return membershipJpaEntity.getAccount();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return null;
    }
}
