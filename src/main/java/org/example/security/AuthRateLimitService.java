package org.example.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthRateLimitService {

    private static class Window {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart = System.currentTimeMillis();
    }

    private static class Lockout {
        int failedAttempts;
        long lockedUntil;
    }

    private final Map<String, Window> ipWindows = new ConcurrentHashMap<>();
    private final Map<String, Window> resendWindows = new ConcurrentHashMap<>();
    private final Map<String, Lockout> verifyLockouts = new ConcurrentHashMap<>();

    private static final long MINUTE_MS = 60_000L;
    private static final long RESEND_COOLDOWN_MS = 60_000L;
    private static final int MAX_AUTH_PER_MINUTE = 100;
    private static final int MAX_VERIFY_ATTEMPTS = 15;
    private static final long LOCKOUT_MS = 3 * MINUTE_MS;

    public boolean allowAuthRequest(String clientIp) {
        return incrementWindow(ipWindows, clientIp, MAX_AUTH_PER_MINUTE, MINUTE_MS);
    }

    public boolean allowResend(String email) {
        String key = email.toLowerCase().trim();
        long now = System.currentTimeMillis();
        Window w = resendWindows.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (now - w.windowStart >= RESEND_COOLDOWN_MS) {
                w.windowStart = now;
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= 3;
        }
    }

    public boolean isVerifyLocked(String email) {
        Lockout lockout = verifyLockouts.get(email.toLowerCase().trim());
        if (lockout == null) return false;
        if (System.currentTimeMillis() >= lockout.lockedUntil) {
            verifyLockouts.remove(email.toLowerCase().trim());
            return false;
        }
        return true;
    }

    public void recordFailedVerify(String email) {
        String key = email.toLowerCase().trim();
        Lockout lockout = verifyLockouts.computeIfAbsent(key, k -> new Lockout());
        lockout.failedAttempts++;
        if (lockout.failedAttempts >= MAX_VERIFY_ATTEMPTS) {
            lockout.lockedUntil = System.currentTimeMillis() + LOCKOUT_MS;
        }
    }

    public void clearVerifyAttempts(String email) {
        verifyLockouts.remove(email.toLowerCase().trim());
    }

    private boolean incrementWindow(Map<String, Window> store, String key, int max, long windowMs) {
        long now = System.currentTimeMillis();
        Window w = store.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (now - w.windowStart >= windowMs) {
                w.windowStart = now;
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= max;
        }
    }
}
