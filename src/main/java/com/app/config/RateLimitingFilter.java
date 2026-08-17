package com.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int maxRequestsPerMinute;
    private final Map<String, RequestBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(@org.springframework.beans.factory.annotation.Value("${app.auth.rate-limit-max-attempts:120}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        RequestBucket bucket = buckets.computeIfAbsent(clientIp, k -> new RequestBucket());

        if (!bucket.allowRequest(maxRequestsPerMinute)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please slow down.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class RequestBucket {
        private long windowStart = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger(0);

        public synchronized boolean allowRequest(int maxRequests) {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60000) {
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
