package com.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;

    private final Cache<String, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/ai/chat")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        String key = token != null ? token.hashCode() + "" : request.getRemoteAddr();

        AtomicInteger counter = rateLimitCache.get(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count > MAX_REQUESTS_PER_MINUTE) {
            log.warn("用户 {} 触发限流，1分钟内请求 {} 次", key, count);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            new ObjectMapper().writeValue(writer, Map.of("code", 429, "message", "请求过于频繁，请稍后再试"));
            return false;
        }

        return true;
    }
}
