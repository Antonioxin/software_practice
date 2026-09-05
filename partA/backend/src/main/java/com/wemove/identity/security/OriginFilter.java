package com.wemove.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wemove.identity.config.WemoveProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class OriginFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS");
    private final Set<String> allowedOrigins;
    private final ObjectMapper mapper;

    public OriginFilter(WemoveProperties properties, ObjectMapper mapper) {
        this.allowedOrigins = new HashSet<>(Arrays.asList(properties.security().allowedOrigins().split(",")));
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        if (!SAFE.contains(request.getMethod())) {
            String origin = request.getHeader("Origin");
            if (origin == null || !allowedOrigins.contains(origin)) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                mapper.writeValue(response.getOutputStream(), Map.of(
                    "type", "about:blank", "title", "Forbidden", "status", 403,
                    "detail", "请求来源未获允许。", "code", "ORIGIN_DENIED",
                    "requestId", String.valueOf(request.getAttribute("requestId"))));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
