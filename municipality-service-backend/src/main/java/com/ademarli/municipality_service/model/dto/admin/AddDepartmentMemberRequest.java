package com.ademarli.municipality_service.model.dto.admin;

import jakarta.validation.constraints.NotNull;


public class AddDepartmentMemberRequest {
    @NotNull
    private Long userId;

    private String memberRole; // MEMBER / MANAGER

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
}
