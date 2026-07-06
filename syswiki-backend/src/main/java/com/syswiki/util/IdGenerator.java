package com.syswiki.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式友好的 ID 生成器（雪花算法风格）
 *
 * ID 格式：{prefix}{timestamp(14)}{worker(3)}{seq(2)}
 *   示例：SP2026053012345000101
 *
 * - timestamp: yyyyMMddHHmmss（14 位，到秒级别）
 * - worker:    3 位，支持 0-999 个实例
 * - seq:       2 位循环序列，每个 worker 每秒最多 99 个 ID
 *
 * 每实例每秒容量：99 个 ID
 * 集群总容量：999 × 99 = 98901 ID/秒
 *
 * 通过 WORKER_ID 环境变量配置实例编号（默认 0），不同实例配置不同值以避免 ID 冲突。
 */
public class IdGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 实例编号，通过环境变量配置（默认 0） */
    private static final int WORKER_ID;

    /** 秒级序列计数器 */
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    /** 上一秒的时间戳字符串，用于检测秒变更时重置序列 */
    private static volatile String lastSecond = "";

    static {
        int worker = 0;
        try {
            String env = System.getenv("WORKER_ID");
            if (env != null && !env.isEmpty()) {
                worker = Integer.parseInt(env);
                if (worker < 0 || worker > 999) {
                    throw new IllegalArgumentException("WORKER_ID 必须在 0-999 之间");
                }
            }
        } catch (NumberFormatException e) {
            // 默认保持 0
        }
        WORKER_ID = worker;
    }

    public static String nextId(String prefix) {
        String ts = LocalDateTime.now().format(FMT);

        // 秒变更时重置序列
        if (!ts.equals(lastSecond)) {
            lastSecond = ts;
            SEQ.set(0);
        }

        int seq = SEQ.getAndIncrement() % 100;
        return prefix + ts + String.format("%03d%02d", WORKER_ID, seq);
    }

    public static String nextId() { return nextId("ID"); }

    /** 获取当前实例的 WORKER_ID */
    public static int getWorkerId() { return WORKER_ID; }
}
