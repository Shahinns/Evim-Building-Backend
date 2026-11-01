package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static class RequestLog {
        AtomicInteger count = new AtomicInteger(0);
        long lastReset = System.currentTimeMillis();
    }

    private final Map<String, RequestLog> clientRequests = new ConcurrentHashMap<>();
    private final Map<String, RequestLog> viewRequests = new ConcurrentHashMap<>();
    private final AuthRateLimitService authRateLimitService;

    private static final int MAX_REQUESTS_PER_MINUTE = 300;
    private static final long RESET_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    public RateLimitFilter(AuthRateLimitService authRateLimitService) {
        this.authRateLimitService = authRateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 🔴 ƏSAS DÜZƏLİŞ: Şəkil sorğularını Rate Limit hesablamasından azad edirik
        if (path.startsWith("/uploads/") || path.startsWith("/api/uploads/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);

        if (path.startsWith("/api/auth")) {
            boolean isVerify = path.equals("/api/auth/verify-email");
            if (!isVerify && !authRateLimitService.allowAuthRequest(clientIp)) {
                writeTooManyRequests(response);
                return;
            }
        } else if (isViewIncrement(path, request.getMethod())) {
            String buildingId = extractBuildingIdFromView(path);
            String viewKey = clientIp + ":" + buildingId;
            if (!allowViewIncrement(viewKey)) {
                writeTooManyRequests(response);
                return;
            }
        } else {
            RequestLog log = clientRequests.computeIfAbsent(clientIp, k -> new RequestLog());
            synchronized (log) {
                long now = System.currentTimeMillis();
                if (now - log.lastReset > RESET_INTERVAL) {
                    log.count.set(0);
                    log.lastReset = now;
                }
                if (log.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                    writeTooManyRequests(response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isViewIncrement(String path, String method) {
        return "POST".equalsIgnoreCase(method) && path.matches("/api/buildings/\\d+/view");
    }

    private String extractBuildingIdFromView(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "unknown";
    }

    private boolean allowViewIncrement(String key) {
        long now = System.currentTimeMillis();
        RequestLog log = viewRequests.computeIfAbsent(key, k -> new RequestLog());
        synchronized (log) {
            if (now - log.lastReset > RESET_INTERVAL) {
                log.count.set(0);
                log.lastReset = now;
            }
            return log.count.incrementAndGet() <= 3;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"Həddindən artıq sorğu. Zəhmət olmasa bir az gözləyin.\"}");
    }
}
