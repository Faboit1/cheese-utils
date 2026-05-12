package com.faboit.cheeseutils.bootstrap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceRegistry {
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    public <T> void register(Class<T> type, T instance) {
        services.put(type, instance);
    }

    public <T> T get(Class<T> type) {
        Object value = services.get(type);
        if (value == null) {
            throw new IllegalStateException("Service not registered: " + type.getName());
        }
        return type.cast(value);
    }
}
