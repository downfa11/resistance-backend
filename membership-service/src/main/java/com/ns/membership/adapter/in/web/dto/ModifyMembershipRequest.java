package com.ns.membership.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
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