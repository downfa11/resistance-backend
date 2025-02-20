package com.ns.membership.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
public class LoginMembershipRequest {
    @NotBlank
    private String account;

    @NotBlank
    private String password;
}

