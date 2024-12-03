package com.moemoe.api.config.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

@Slf4j
public class RequestLoggingFilter extends AbstractRequestLoggingFilter {

    public RequestLoggingFilter() {
        // 로깅 설정
        setIncludePayload(true);
        setIncludeQueryString(true);
        setMaxPayloadLength(1000);
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        log.info(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // swagger 관련 URL 패턴 제외
        return uri.startsWith("/swagger-ui") || uri.startsWith("/api-docs") || uri.startsWith("/v3/api-docs");
    }
}

