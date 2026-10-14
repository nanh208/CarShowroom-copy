package com.ares.shop.web;

import com.ares.shop.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Current {
    private Current() {}
    public static User userOrNull() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof User u ? u : null;
    }
    public static User user() {
        User u = userOrNull();
        if (u == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        return u;
    }
}
