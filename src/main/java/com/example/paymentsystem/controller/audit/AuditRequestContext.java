package com.example.paymentsystem.controller.audit;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** 현재 HTTP 요청에 감사용 공개 오류 코드를 안전하게 전달한다. */
public final class AuditRequestContext {
    public static final String ERROR_CODE_ATTRIBUTE = AuditRequestContext.class.getName() + ".errorCode";

    private AuditRequestContext() {
    }

    /** 현재 요청이 존재할 때만 표준 오류 코드를 request scope에 기록한다. */
    public static void setErrorCode(String errorCode) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
