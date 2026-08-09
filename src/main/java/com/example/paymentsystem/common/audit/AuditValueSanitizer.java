package com.example.paymentsystem.common.audit;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 외부 입력 식별자를 길이와 허용 문자 기준으로 정규화한다. */
@Component
public class AuditValueSanitizer {
    private static final Pattern CLIENT_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    /** 유효한 clientId만 유지하고 나머지는 anonymous로 대체한다. */
    public String clientId(String value) {
        return value != null && CLIENT_ID.matcher(value).matches() ? value : "anonymous";
    }

    /** 매핑된 URI 템플릿만 저장하고 미매핑 경로의 실제 값은 숨긴다. */
    public String requestPath(Object template) {
        return template instanceof String path && !path.isBlank() && !"/**".equals(path) && path.length() <= 255
                ? path : "/unmapped";
    }
}
