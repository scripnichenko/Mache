package com.excelian.mache.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HashMapCacheLoader<K, V> implements MacheLoader<K, V> {
    private final String cacheName;
    private final Map<K, V> store = new ConcurrentHashMap<>();

    public HashMapCacheLoader(final Class<V> valueType) {
        this.cacheName = valueType.getSimpleName();
    }


    @Override
    public void create() {
        // NOOP
    }

    @Override
    public void put(final K k, final V v) {
        store.put(k, v);
    }

    @Override
    public void remove(final K k) {
        store.remove(k);
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public V load(K key) throws Exception {
        return store.get(key);
    }
}
