package com.ns.membership.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetRequest {
    private String membershipId;
    private String newPassword;
    private String verify;
}

