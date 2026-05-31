package com.syswiki.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {
    private static final AtomicLong SEQ = new AtomicLong(ThreadLocalRandom.current().nextInt(10000, 99999));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String nextId(String prefix) {
        String ts = LocalDateTime.now().format(FMT);
        long seq = SEQ.incrementAndGet() % 100000;
        return prefix + ts + String.format("%05d", seq);
    }

    public static String nextId() { return nextId("ID"); }
}
