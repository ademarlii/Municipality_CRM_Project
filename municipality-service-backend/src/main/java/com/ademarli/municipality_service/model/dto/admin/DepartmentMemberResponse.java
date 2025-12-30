package com.ademarli.municipality_service.model.dto.admin;

public class DepartmentMemberResponse {
    private Long userId;
    private String email;
    private String phone;
    private String memberRole;
    private boolean active;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
