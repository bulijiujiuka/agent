package com.admin.controller;

import com.admin.annotation.OperLog;
import com.admin.dto.*;
import com.admin.entity.User;
import com.admin.exception.UnauthorizedException;
import com.admin.service.UserService;
import com.admin.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        String actualToken = token.replace("Bearer ", "");
        if (!jwtUtil.validateToken(actualToken)) {
            throw new UnauthorizedException("Token无效或已过期");
        }
        String username = jwtUtil.getUsernameFromToken(actualToken);
        User user = userService.getUserByUsername(username);
        return Result.success(user);
    }

    @OperLog(module = "个人中心", description = "修改个人信息")
    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                       HttpServletRequest httpRequest) {
        String username = (String) httpRequest.getAttribute("currentUser");
        User updated = userService.updateProfile(username, request);
        return Result.success(updated);
    }

    @OperLog(module = "个人中心", description = "修改密码")
    @PostMapping("/changePassword")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
