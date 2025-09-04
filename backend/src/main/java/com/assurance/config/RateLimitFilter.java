package com.assurance.config;

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

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final int LOGIN_LIMIT = 10;      // 10 req/min par IP
    private static final int GENERIC_LIMIT = 60;    // 60 req/min par IP

    private static class CounterWindow {
        volatile long windowStartMs;
        volatile int count;
        final int limit;
        final long windowMs;

        CounterWindow(int limit, long windowMs) {
            this.limit = limit;
            this.windowMs = windowMs;
            this.windowStartMs = System.currentTimeMillis();
            this.count = 0;
        }

        synchronized boolean allow() {
            long now = System.currentTimeMillis();
            if (now - windowStartMs >= windowMs) {
                windowStartMs = now;
                count = 0;
            }
            if (count < limit) {
                count++;
                return true;
            }
            return false;
        }
    }

    private final Map<String, CounterWindow> windows = new ConcurrentHashMap<>();

    private boolean allowRequest(String key, int limit) {
        CounterWindow win = windows.computeIfAbsent(key, k -> new CounterWindow(limit, ONE_MINUTE_MS));
        return win.allow();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();

        if (path.startsWith("/api/auth/login")) {
            String key = "login:" + ip;
            if (!allowRequest(key, LOGIN_LIMIT)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too many login attempts. Please try again later.");
                return;
            }
        } else if (path.startsWith("/api/report-requests") || path.startsWith("/api/download") || path.startsWith("/api/subscriptions")) {
            String key = "gen:" + ip;
            if (!allowRequest(key, GENERIC_LIMIT)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Rate limit exceeded. Please slow down.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}


