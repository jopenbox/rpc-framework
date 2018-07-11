package com.example.rpc.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 序列类
 */
public class Sequence {

    private static AtomicInteger sequence = new AtomicInteger(1000);

    public static int next() {
        return sequence.addAndGet(1);
    }
}
