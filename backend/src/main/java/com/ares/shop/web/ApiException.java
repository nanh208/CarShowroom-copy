package com.ares.shop.web;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public ApiException(HttpStatus status, String msg) { super(msg); this.status = status; }
    public static ApiException bad(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    public static ApiException notFound(String m) { return new ApiException(HttpStatus.NOT_FOUND, m); }
}
