package com.syswiki.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        // 登录和注册接口放行
        if (uri.contains("/auth/login") || uri.contains("/auth/register") || uri.contains("/templates/")) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.warn("认证失败: 未携带Token, uri={}, ip={}", uri, request.getRemoteAddr());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
            return false;
        }

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
            log.warn("认证失败: Token无效或已过期, uri={}, ip={}", uri, request.getRemoteAddr());
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\",\"data\":null}");
            return false;
        }

        // 将用户信息存入request attribute
        request.setAttribute("currentUserId", jwtUtil.getUserId(token));
        request.setAttribute("currentUsername", jwtUtil.getUsername(token));
        request.setAttribute("currentRole", jwtUtil.getRole(token));
        log.debug("认证成功: userId={}, username={}, uri={}", jwtUtil.getUserId(token), jwtUtil.getUsername(token), uri);
        return true;
    }
}
