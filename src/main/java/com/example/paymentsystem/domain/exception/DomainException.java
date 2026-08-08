package com.example.paymentsystem.domain.exception;

/** HTTP 계층과 분리된 업무 오류 코드와 메시지를 제공하는 도메인 예외의 기반 클래스다. */
public class DomainException extends RuntimeException {

    private final DomainErrorCode errorCode;

    /** 표준 오류 코드에 정의된 메시지로 도메인 예외를 생성한다. */
    public DomainException(DomainErrorCode errorCode) {
        super(requireErrorCode(errorCode).getMessage());
        this.errorCode = errorCode;
    }

    /** Presentation 계층에서 사용할 문자열 업무 오류 코드를 반환한다. */
    public String getCode() {
        return errorCode.getCode();
    }

    /** 발생한 오류의 enum 값을 반환한다. */
    public DomainErrorCode getErrorCode() {
        return errorCode;
    }

    private static DomainErrorCode requireErrorCode(DomainErrorCode errorCode) {
        if (errorCode == null) {
            throw new IllegalArgumentException("도메인 오류 코드는 필수입니다.");
        }
        return errorCode;
    }
}
