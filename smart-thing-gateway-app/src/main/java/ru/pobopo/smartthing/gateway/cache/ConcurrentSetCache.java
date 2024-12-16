package ru.pobopo.smartthing.gateway.cache;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ConcurrentSetCache<T> {
    private final Set<CacheItem<T>> items = ConcurrentHashMap.newKeySet();

    private final long evictionTime;
    private final ChronoUnit timeUnits;
    // Called, when added item is not already in cache
    private final Consumer<T> onPut;
    // Called, when item evicted from cache
    private final Consumer<T> onEvict;

    public void put(T value) {
        if (value == null) {
            return;
        }

        CacheItem<T> item = new CacheItem<>(value, LocalDateTime.now());
        if (!items.remove(item)) {
            onPut.accept(value);
        }
        items.add(item);
    }

    @NonNull
    public Set<T> getValues() {
        Set<CacheItem<T>> it = new HashSet<>(items);
        return it.stream().map(CacheItem::getItem).collect(Collectors.toUnmodifiableSet());
    }

    public int size() {
        return items.size();
    }

    public void evictItems() {
        items.removeIf(this::shouldEvict);
    }

    private boolean shouldEvict(CacheItem<T> item) {
        boolean result = item.getAddedTime().until(LocalDateTime.now(), timeUnits) >= evictionTime;
        if (result) {
            onEvict.accept(item.getItem());
        }
        return result;
    }
}
