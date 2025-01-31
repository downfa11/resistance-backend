package com.ns.membership.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModifyMembershipRequest {

    @NotBlank
    private String membershipId;

    private String name;
    private String account;
    private String password;
    private String address;
    private String email;
    private boolean isValid;
}