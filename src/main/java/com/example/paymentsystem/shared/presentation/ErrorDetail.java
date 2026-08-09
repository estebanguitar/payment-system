package com.example.paymentsystem.shared.presentation;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 입력 검증 실패 시 외부에 공개 가능한 필드와 사유만 전달한다. */
@Getter
@Builder
public class ErrorDetail {
    private final List<FieldError> errors;

    /** 단일 필드의 검증 실패 정보를 표현한다. */
    public record FieldError(String field, String reason) {
    }
}
