package com.faboit.cheeseutils.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AsyncExecutor implements AutoCloseable {
    private final ExecutorService ioPool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    public ExecutorService io() {
        return ioPool;
    }

    @Override
    public void close() {
        ioPool.shutdown();
        try {
            if (!ioPool.awaitTermination(3, TimeUnit.SECONDS)) {
                ioPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            ioPool.shutdownNow();
        }
    }
}
