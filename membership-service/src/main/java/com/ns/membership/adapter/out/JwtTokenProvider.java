package com.ns.membership.adapter.out;

import com.ns.membership.application.port.out.AuthMembershipPort;
import com.ns.membership.domain.Membership;
import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider implements AuthMembershipPort {

    private String jwtSecret; // secret key
    private long jwtExpirationInMs;
    private long refreshTokenExpirationInMs;

    public JwtTokenProvider(){
        this.jwtSecret="NYd4nEtyLtcU7cpS/1HTFVmQJd7MmrP+HafWoXZjWNOL7qKccOOUfQNEx5yvG6dfdpuBeyMs9eEbRmdBrPQCNg==";
        this.jwtExpirationInMs= 1000L * 300;
        this.refreshTokenExpirationInMs = 1000L * 360;
    }

    public String getJwtToken(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return request.getHeader("Authorization");
    }
    public Long getMembershipIdbyToken() {
        String accessToken = getJwtToken();
        if(accessToken == null || accessToken.length() == 0){
            throw new RuntimeException("JwtToken is Invalid.");
        }

        String token = accessToken.replace("Bearer ", "");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        log.info("claims: "+claims);

        String membershipIdString = claims.get("sub", String.class);
        Long membershipId = Long.parseLong(membershipIdString);
        return membershipId;
    }

    @Override
    public String generateJwtToken(Membership.MembershipId membershipId, Membership.MembershipRole membershipRole) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String token = Jwts.builder()
                .setSubject(membershipId.getMembershipId())
                .setHeaderParam("type", "jwt")
                .claim("id", membershipId.getMembershipId())
                .claim("role",membershipRole.getMembershipRole())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();

        return "Bearer " + token;
    }

    @Override
    public String generateRefreshToken(Membership.MembershipId membershipId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationInMs);

        return Jwts.builder()
                .setSubject(membershipId.getMembershipId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }


    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException ex) {
            // Invalid JWT token: 유효하지 않은 JWT 토큰일 때 발생하는 예외
            // Expired JWT token: 토큰의 유효기간이 만료된 경우 발생하는 예외
            // Unsupported JWT token: 지원하지 않는 JWT 토큰일 때 발생하는 예외
            // JWT claims string is empty: JWT 토큰이 비어있을 때 발생하는 예외
            log.info("[ERROR] jwtToken error : "+ex);
        }
        return false;
    }

    public Membership.MembershipId parseMembershipIdFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
        return new Membership.MembershipId(claims.getSubject());
    }

    public String getMembershipRolebyToken(String token) {
        if (token == null || token.length() == 0) {
            throw new RuntimeException("JwtToken is Invalid.");
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    public Long getMembershipIdbyToken(String token) {
        if(token == null || token.length() == 0){
            throw new RuntimeException("JwtToken is Invalid.");
        }

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String membershipIdString = claims.get("sub", String.class);
        Long membershipId = Long.parseLong(membershipIdString);
        return membershipId;
    }

}