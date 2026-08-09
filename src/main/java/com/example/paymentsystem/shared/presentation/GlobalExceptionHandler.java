package com.example.paymentsystem.shared.presentation;

import com.example.paymentsystem.shared.application.exception.ApplicationErrorCode;
import com.example.paymentsystem.shared.application.exception.ApplicationException;
import com.example.paymentsystem.shared.domain.exception.DomainErrorCode;
import com.example.paymentsystem.shared.domain.exception.DomainException;
import com.example.paymentsystem.audit.presentation.AuditRequestContext;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 계층별 예외를 공통 오류 응답과 일관된 HTTP 상태로 변환한다. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Application 업무 오류를 정의된 HTTP 상태로 변환한다. */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplication(ApplicationException exception) {
        AuditRequestContext.setErrorCode(exception.getCode());
        return error(status(exception.getErrorCode()), exception.getCode(), exception.getMessage());
    }

    /** Domain 불변식 오류를 입력 오류·충돌·서버 오류로 분류한다. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException exception) {
        AuditRequestContext.setErrorCode(exception.getCode());
        return error(status(exception.getErrorCode()), exception.getCode(), exception.getMessage());
    }

    /** Request Body 검증 오류를 필드명 순으로 정렬해 반환한다. */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<ErrorDetail>> handleValidation(BindException exception) {
        AuditRequestContext.setErrorCode("INVALID_REQUIRED_VALUE");
        List<ErrorDetail.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorDetail.FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorDetail.FieldError::field))
                .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "INVALID_REQUIRED_VALUE", "요청값이 올바르지 않습니다.", ErrorDetail.builder().errors(fields).build()));
    }

    /** Header 누락, 타입 변환, JSON 형식 및 제약 위반을 표준 입력 오류로 반환한다. */
    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
        AuditRequestContext.setErrorCode("INVALID_REQUIRED_VALUE");
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUIRED_VALUE", "요청값이 올바르지 않습니다.");
    }

    /** 매핑되지 않은 API 경로를 공개 가능한 표준 404 오류로 반환한다. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(NoResourceFoundException exception) {
        String code = ApplicationErrorCode.API_RESOURCE_NOT_FOUND.getCode();
        AuditRequestContext.setErrorCode(code);
        return error(HttpStatus.NOT_FOUND, code, ApplicationErrorCode.API_RESOURCE_NOT_FOUND.getMessage());
    }

    /** 예상하지 못한 예외는 내부 내용을 숨기고 서버 로그에만 원인을 남긴다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        AuditRequestContext.setErrorCode("SYSTEM_ERROR");
        log.error("예상하지 못한 API 처리 오류가 발생했습니다.", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "SYSTEM_ERROR", "시스템 처리 중 오류가 발생했습니다.");
    }

    private static HttpStatus status(ApplicationErrorCode code) {
        return switch (code) {
            case CUSTOMER_NOT_FOUND, WALLET_NOT_FOUND, PAYMENT_NOT_FOUND, CANCEL_NOT_FOUND, AUDIT_LOG_NOT_FOUND,
                    API_RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case SYSTEM_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static HttpStatus status(DomainErrorCode code) {
        return switch (code) {
            case INVALID_PAYMENT_STATE -> HttpStatus.CONFLICT;
            case OUTBOX_RETRY_OVERFLOW -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message, null));
    }
}
