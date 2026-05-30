package com.syswiki.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器
 * 格式: 前缀 + 日期 + 序号
 * 示例: SP20260530000001
 */
public class IdGenerator {

    private static final AtomicLong SEQ = new AtomicLong(0);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String nextId(String prefix) {
        String date = LocalDateTime.now().format(FMT);
        long seq = SEQ.incrementAndGet() % 1000000;
        return prefix + date + String.format("%06d", seq);
    }

    public static String nextId() {
        return nextId("ID");
    }
}
