package com.ns.membership.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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