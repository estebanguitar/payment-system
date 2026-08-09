package com.example.paymentsystem.shared.application.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

/** Spring Page를 외부 계층과 분리한 1-based 페이지 결과다. */
@Getter
@Builder
public class PageResult<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    /** Spring Page의 메타데이터를 1-based 결과로 변환한다. */
    public static <T> PageResult<T> from(Page<T> source) {
        return PageResult.<T>builder().content(source.getContent()).page(source.getNumber() + 1)
                .size(source.getSize()).totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages()).build();
    }
}
