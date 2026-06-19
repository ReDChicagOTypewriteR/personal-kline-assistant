package com.alexjoker.quant.auth.service;

import com.alexjoker.quant.auth.dto.AuthSessionDTO;
import com.alexjoker.quant.auth.dto.LoginRequest;
import com.alexjoker.quant.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String LOCAL_ADMIN_TOKEN = "personal-kline-local-admin-token";

    public AuthSessionDTO login(LoginRequest request) {
        if (!ADMIN_USERNAME.equals(request.getUsername()) || !ADMIN_PASSWORD.equals(request.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        return new AuthSessionDTO(LOCAL_ADMIN_TOKEN, ADMIN_USERNAME, "本地管理员");
    }

    public boolean isValidToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return false;
        }
        String token = authorizationHeader.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        return LOCAL_ADMIN_TOKEN.equals(token);
    }

    public AuthSessionDTO currentSession() {
        return new AuthSessionDTO(LOCAL_ADMIN_TOKEN, ADMIN_USERNAME, "本地管理员");
    }
}
