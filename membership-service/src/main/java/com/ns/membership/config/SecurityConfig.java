package com.ns.membership.config;

import com.ns.membership.adapter.out.JwtTokenProvider;
import com.ns.membership.application.security.JWTFilter;
import com.ns.membership.application.security.LoginFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    //private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //csrf disable
        http.csrf((auth) -> auth.disable());

        //http basic 인증 방식 disable
        http.httpBasic((auth) -> auth.disable());

        http.formLogin(form -> {
            form.loginPage("/loginForm")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/");
        });


        //경로별 인가 작업
        http.authorizeHttpRequests((auth) -> auth
//                .requestMatchers("/actuator/**",
//                        "/membership/login",
//                        "/membership/register/temp",
//                        "/",
//                        "/join",
//                        "/membership/register/**",
//                         "/swagger-ui.html",
//                         "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**"
//                        ).permitAll()
//                .requestMatchers("/manager/**").hasAnyRole("MANAGER", "ADMIN")
//                .requestMatchers("/admin/**").hasAnyRole("ADMIN")
                .anyRequest().permitAll());


     //   http.addFilterBefore(new JWTFilter(jwtTokenProvider), LoginFilter.class);
       // http.addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        //세션 설정
        http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
