package com.ares.shop.web;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {
    @org.springframework.beans.factory.annotation.Autowired com.ares.shop.service.I18nService i18n;
    /** Thông báo lỗi dịch theo Accept-Language (vi mặc định, en nếu có bản dịch). */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> api(ApiException e, jakarta.servlet.http.HttpServletRequest req) {
        String lang = com.ares.shop.service.I18nService.langOf(req.getHeader("Accept-Language"));
        return ResponseEntity.status(e.status).body(Map.of("message", i18n.error(e.getMessage(), lang)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        var f = e.getBindingResult().getFieldErrors();
        String msg = f.isEmpty() ? "Dữ liệu không hợp lệ" : f.get(0).getField() + ": " + f.get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }
}
