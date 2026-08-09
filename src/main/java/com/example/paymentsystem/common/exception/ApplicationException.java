package com.example.paymentsystem.common.exception;

import lombok.Getter;

/** HTTP 상태와 분리된 Application 업무 오류와 추적 식별자를 전달한다. */
@Getter
public class ApplicationException extends RuntimeException {

    private final ApplicationErrorCode errorCode;
    private final Long resourceId;

    /** 표준 오류 코드로 Application 예외를 생성한다. */
    public ApplicationException(ApplicationErrorCode errorCode) {
        this(errorCode, null, null);
    }

    /** 관련 업무 리소스 식별자를 포함해 Application 예외를 생성한다. */
    public ApplicationException(ApplicationErrorCode errorCode, Long resourceId) {
        this(errorCode, resourceId, null);
    }

    /** 내부 원인을 보존한 Application 예외를 생성한다. */
    public ApplicationException(ApplicationErrorCode errorCode, Long resourceId, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.resourceId = resourceId;
    }

    /** Presentation 계층에서 사용할 문자열 업무 오류 코드를 반환한다. */
    public String getCode() {
        return errorCode.getCode();
    }
}
