package com.ademarli.municipality_service.model.dto.auth;


import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String emailOrPhone;
    @NotBlank
    private String password;


    public LoginRequest() {
    }


    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

