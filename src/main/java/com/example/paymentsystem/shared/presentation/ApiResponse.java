package com.example.paymentsystem.shared.presentation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 모든 HTTP 성공·오류 응답의 공통 최상위 구조를 제공한다. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private final String code;
    private final String message;
    private final T returnObject;

    /** 기본 성공 메시지와 응답 데이터를 포함한 표준 응답을 생성한다. */
    public static <T> ApiResponse<T> success(T data) {
        return success("요청이 정상 처리되었습니다.", data);
    }

    /** 성공 코드와 응답 데이터를 포함한 표준 응답을 생성한다. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("OK", message, data);
    }

    /** 업무 오류 코드와 안전한 상세 정보를 포함한 표준 응답을 생성한다. */
    public static <T> ApiResponse<T> error(String code, String message, T detail) {
        return new ApiResponse<>(code, message, detail);
    }
}
