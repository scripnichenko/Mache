package com.excelian.mache.observable;

import com.excelian.mache.core.ExCache;
import com.excelian.mache.core.MacheLoader;

// This is something like an "ObservableCacheFactory"
public interface CacheFactory {
	<K, V, D> ExCache<K, V> createCache(MacheLoader<K, V, D> cacheLoader);
	<K, V> ExCache<K, V> createCache(ExCache<K, V> underlyingCache);
}
