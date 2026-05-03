package com.circleguard.promotion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FenceException extends RuntimeException {
    public FenceException(String message) {
        super(message);
    }
}
