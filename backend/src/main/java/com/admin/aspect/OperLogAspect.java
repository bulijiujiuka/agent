package com.admin.aspect;

import com.admin.annotation.OperLog;
import com.admin.mapper.OperLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final OperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.admin.annotation.OperLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperLog annotation = method.getAnnotation(OperLog.class);

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 构建日志对象
        com.admin.entity.OperLog operLog = new com.admin.entity.OperLog();
        operLog.setModule(annotation.module());
        operLog.setDescription(annotation.description());
        operLog.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName());

        if (request != null) {
            operLog.setRequestMethod(request.getMethod());
            operLog.setRequestUrl(request.getRequestURI());
            operLog.setOperUser((String) request.getAttribute("currentUser"));
            operLog.setOperIp(getClientIp(request));
        }

        // 记录请求参数（排除文件类型）
        try {
            String params = getRequestParams(signature, joinPoint.getArgs());
            operLog.setRequestParams(truncate(params, 2000));
        } catch (Exception e) {
            operLog.setRequestParams("参数序列化失败");
        }

        // 执行目标方法
        Object result = null;
        try {
            result = joinPoint.proceed();
            operLog.setStatus(1);
            // 记录响应结果
            try {
                String resultStr = objectMapper.writeValueAsString(result);
                operLog.setResponseResult(truncate(resultStr, 2000));
            } catch (Exception ignored) {
            }
        } catch (Throwable e) {
            operLog.setStatus(0);
            operLog.setErrorMsg(truncate(e.getMessage(), 2000));
            throw e;
        } finally {
            operLog.setCostTime(System.currentTimeMillis() - startTime);
            // 异步保存日志（不影响主业务）
            try {
                operLogMapper.insert(operLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }

    private String getRequestParams(MethodSignature signature, Object[] args) throws Exception {
        String[] paramNames = signature.getParameterNames();
        Map<String, Object> params = new LinkedHashMap<>();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if (args[i] instanceof MultipartFile) {
                    params.put(paramNames[i], "[文件]");
                } else if (args[i] instanceof MultipartFile[]) {
                    params.put(paramNames[i], "[文件数组]");
                } else {
                    params.put(paramNames[i], args[i]);
                }
            }
        }
        return objectMapper.writeValueAsString(params);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
