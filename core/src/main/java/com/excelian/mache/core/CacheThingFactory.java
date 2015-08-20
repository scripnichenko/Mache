package com.excelian.mache.core;

public class CacheThingFactory {
    public <K, V, D> ExCache<K, V> create(MacheLoader<K, V, D> cacheLoader, String... options) {
        return new CacheThing<K, V>(cacheLoader, options);
    }
}
