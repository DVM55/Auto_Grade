package com.example.Auto_Grade.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ExceptionResponse> handleException(Exception e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .details(e.toString())
                .message(e.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(exceptionResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleBadCredentialsException(BadCredentialsException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.UNAUTHORIZED)
                .details(e.toString())
                .message("Tài khoản hoặc mật khẩu không chính xác!")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exceptionResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    public ResponseEntity<ExceptionResponse> handleAccessDeniedException(AccessDeniedException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.FORBIDDEN)
                .details(e.toString())
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exceptionResponse);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleExpiredJwtException(ExpiredJwtException e) {
        ExceptionResponse response = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.UNAUTHORIZED)
                .message("Token đã hết hạn")
                .details("Expired at: " + e.getClaims().getExpiration())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(SignatureException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleSignatureException(SignatureException e) {
        ExceptionResponse response = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.UNAUTHORIZED)
                .message("Chữ ký JWT không hợp lệ")
                .details(e.toString())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(MalformedJwtException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleMalformedJwtException(MalformedJwtException e) {
        ExceptionResponse response = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST)
                .message("Định dạng JWT không hợp lệ")
                .details(e.toString())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UnsupportedJwtException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ExceptionResponse> handleUnsupportedJwtException(UnsupportedJwtException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.UNAUTHORIZED)
                .details(e.toString())
                .message("JWT không được hỗ trợ")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exceptionResponse);
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.LOCKED) // HTTP 423
    public ResponseEntity<ExceptionResponse> handleLockedException(LockedException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.LOCKED)
                .details(e.toString())
                .message("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ.")
                .build();

        return ResponseEntity.status(HttpStatus.LOCKED).body(exceptionResponse);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        FieldError fieldError = (FieldError) ex.getBindingResult().getAllErrors().getFirst();

        String fieldName = fieldError.getField();
        String errorMessage = fieldError.getDefaultMessage();

        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST)
                .message(errorMessage)
                .details("Invalid field: " + fieldName)
                .build();

        return ResponseEntity.badRequest().body(exceptionResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ExceptionResponse response = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST)
                .message(ex.getMessage())
                .details(ex.toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public ResponseEntity<ExceptionResponse> handleConflictException(ConflictException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.CONFLICT)
                .details(e.toString())
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exceptionResponse);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ResponseEntity<ExceptionResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.NOT_FOUND)
                .details(e.toString())
                .message(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ExceptionResponse> handleEntityNotFoundException(EntityNotFoundException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.NOT_FOUND)
                .message(e.getMessage())
                .details("Không tìm thấy tài nguyên được yêu cầu")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ExceptionResponse> handleInvalidEnum(HttpMessageNotReadableException ex) {

        ExceptionResponse response = ExceptionResponse.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST)
                .message("Dữ liệu gửi lên không hợp lệ")
                .details(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(response);
    }
}
