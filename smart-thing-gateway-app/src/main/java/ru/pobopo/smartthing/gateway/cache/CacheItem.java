package ru.pobopo.smartthing.gateway.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CacheItem<T> {
    private final T item;
    private final LocalDateTime addedTime;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        if (((CacheItem<T>) obj).getItem() == null) {
            return getItem() == null;
        }

        return ((CacheItem<T>) obj).getItem().equals(getItem());
    }

    @Override
    public int hashCode() {
        return getItem().hashCode();
    }
}
