package com.admin.config;

import com.admin.exception.UnauthorizedException;
import com.admin.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("未登录或Token已过期，请重新登录");
        }

        // 将当前用户名存入 request，后续业务可直接获取
        String username = jwtUtil.getUsernameFromToken(token);
        request.setAttribute("currentUser", username);
        return true;
    }

    /**
     * 从请求头中提取 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
