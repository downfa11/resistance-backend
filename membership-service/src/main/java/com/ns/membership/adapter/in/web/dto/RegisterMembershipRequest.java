package com.ns.membership.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RegisterMembershipRequest {

    @NotBlank
    private String name;
    @NotBlank
    private String account;
    @NotBlank
    private String password;
    @NotBlank
    private String address;
    @NotBlank
    private String email;
    @NotBlank
    private String verify;
}