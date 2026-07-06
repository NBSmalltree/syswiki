package com.syswiki.util;

import com.syswiki.exception.BizException;
import com.syswiki.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录速率限制器（内存版）
 * 同一 IP 连续失败 N 次后，锁定 M 分钟
 */
@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private final int maxAttempts;
    private final long lockMinutes;

    /** key → [失败次数, 锁定时间戳(ms)] */
    private final ConcurrentHashMap<String, long[]> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter(
            @Value("${syswiki.auth.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${syswiki.auth.rate-limit.lock-minutes:15}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    /**
     * 检查是否被锁定
     * @param key IP 或用户名
     * @throws BizException 如果已被锁定
     */
    public void check(String key) {
        long[] record = attempts.get(key);
        if (record == null) return;

        long lockUntil = record[1];
        if (lockUntil > 0 && System.currentTimeMillis() < lockUntil) {
            long remaining = (lockUntil - System.currentTimeMillis()) / 1000;
            throw new BizException(ErrorCode.AUTH_FAILED,
                "登录尝试过多，请 " + remaining + " 秒后再试");
        }
        // 锁定已过期，重置
        if (lockUntil > 0 && System.currentTimeMillis() >= lockUntil) {
            attempts.remove(key);
        }
    }

    /**
     * 记录一次失败，达到阈值时锁定
     */
    public void recordFailure(String key) {
        long[] record = attempts.computeIfAbsent(key, k -> new long[]{0, 0});
        record[0]++;
        if (record[0] >= maxAttempts) {
            record[1] = System.currentTimeMillis() + lockMinutes * 60 * 1000;
            log.warn("登录锁定: key={}, 锁定{}分钟", key, lockMinutes);
        }
    }

    /** 登录成功后清除记录 */
    public void clear(String key) { attempts.remove(key); }
}