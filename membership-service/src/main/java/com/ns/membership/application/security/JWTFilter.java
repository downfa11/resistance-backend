package com.ns.membership.application.security;

import com.ns.membership.adapter.out.JwtTokenProvider;
import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization= request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("URL Request : "+request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("authorization now");
        String token = authorization.split(" ")[1];

        if (!jwtTokenProvider.validateJwtToken(token)) {
            System.out.println("token expired");
            filterChain.doFilter(request, response);
            return;
        }

        Long membershipId = jwtTokenProvider.getMembershipIdbyToken(token);
        String role = jwtTokenProvider.getMembershipRolebyToken(token);

        MembershipJpaEntity membershipJpaEntity = new MembershipJpaEntity();
        membershipJpaEntity.setMembershipId(membershipId);
        membershipJpaEntity.setPassword("temppassword");
        membershipJpaEntity.setRole(role);

        //UserDetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(membershipJpaEntity);

        //스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        //세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);
        System.out.println("SecurityContext Authentication : "+SecurityContextHolder.getContext().getAuthentication());
        filterChain.doFilter(request, response);
    }
}