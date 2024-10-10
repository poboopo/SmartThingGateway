package ru.pobopo.smartthing.gateway.cache;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.NonNull;

public class ConcurrentSetCache<T> {
    private final Set<CacheItem<T>> items = ConcurrentHashMap.newKeySet();
    private final long evictionTime;
    private final ChronoUnit timeUnits;

    public ConcurrentSetCache(long evictionTime, ChronoUnit timeUnits) {
        this.evictionTime = evictionTime;
        this.timeUnits = timeUnits;
    }

    public void put(T value) {
        if (value == null) {
            return;
        }

        CacheItem<T> item = new CacheItem<>(value, LocalDateTime.now());
        items.remove(item);
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
        return item.getAddedTime().until(LocalDateTime.now(), timeUnits) >= evictionTime;
    }
}
