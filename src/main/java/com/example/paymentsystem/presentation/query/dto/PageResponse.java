package com.example.paymentsystem.presentation.query.dto;

import com.example.paymentsystem.application.dto.query.PageResult;
import java.util.List;
import java.util.function.Function;

/** API 목록의 1-based 페이지 메타데이터를 표현한다. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    /** 애플리케이션 페이지의 항목을 외부 응답 형식으로 변환한다. */
    public static <S, T> PageResponse<T> from(PageResult<S> source, Function<S, T> mapper) {
        return new PageResponse<>(source.getContent().stream().map(mapper).toList(), source.getPage(),
                source.getSize(), source.getTotalElements(), source.getTotalPages());
    }
}
