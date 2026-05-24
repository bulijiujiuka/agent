package com.admin.dto;

import com.admin.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private User userInfo;
    
    public LoginResponse(String token, User userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}
