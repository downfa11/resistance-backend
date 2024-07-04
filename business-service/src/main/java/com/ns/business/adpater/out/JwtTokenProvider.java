package com.ns.business.adpater.out;

import io.jsonwebtoken.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Component
public class JwtTokenProvider {

    private String jwtSecret; // secret key

    public JwtTokenProvider(){
        this.jwtSecret="NYd4nEtyLtcU7cpS/1HTFVmQJd7MmrP+HafWoXZjWNOL7qKccOOUfQNEx5yvG6dfdpuBeyMs9eEbRmdBrPQCNg==";
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

        try {
            String token = accessToken.replace("Bearer ", "");
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String membershipIdString = claims.get("sub", String.class);
            return Long.parseLong(membershipIdString);
        } catch (ExpiredJwtException ex) {
            throw ex;
        }
    }

}