package com.bitcomputer.portal.exception;

import com.bitcomputer.portal.dto.ApiError;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiError> app(AppException e) {
        return ResponseEntity.status(e.status())
                .body(new ApiError(e.code(), e.getMessage(), e.retryAfter(), Instant.now()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ApiError> validation(Exception e) {
        var result = e instanceof MethodArgumentNotValidException m ? m.getBindingResult()
                : ((BindException) e).getBindingResult();
        var message = result.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("DUPLICATE_EMPLOYEE", "이미 사용 중인 사번 또는 이메일입니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformedRequest() {
        return ResponseEntity.badRequest()
                .body(ApiError.of("INVALID_REQUEST", "요청 형식 또는 입력값이 올바르지 않습니다."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ApiError.of("INTERNAL_ERROR", "요청 처리 중 오류가 발생했습니다."));
    }
}
