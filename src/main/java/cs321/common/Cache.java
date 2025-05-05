package cs321.common;

import java.util.LinkedHashMap;
import java.util.LinkedList;



// Project 1 implementation of a webcache refactored for b-tree usage.

public class Cache<K, V extends KeyInterface<K>> {
    private final LinkedList<V> cache;
    private final int maxSize;
    private int calls;
    private int hits;

    public Cache(int maxSize) {
        this.cache = new LinkedList<>();
        this.maxSize = maxSize;
        this.calls = 0;
        this.hits = 0;
    }

    /**
     * Returns the object associated with the key, if present. Moves it to the front.
     */
    public V get(K key) {
        calls += 1;
        for (int i = 0; i < cache.size(); i++) {
            V value = cache.get(i);
            if (value.getKey().equals(key)) {
                cache.remove(i);
                cache.addFirst(value);
                hits += 1;
                return value;
            }
        }
        return null;
    }

    /**
     * Adds a value to the cache. If it's a duplicate, replaces the old one.
     * If the cache is full, removes the least recently used.
     */
    public V add(V value) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getKey().equals(value.getKey())) {
                cache.remove(i);
                break;
            }
        }
        cache.addFirst(value);
        if (cache.size() > maxSize) {
            return cache.removeLast(); // evict LRU
        }
        return null;
    }

    /**
     * Removes a cached item by its key.
     */
    public V remove(K key) {
        for (int i = 0; i < cache.size(); i++) {
            V value = cache.get(i);
            if (value.getKey().equals(key)) {
                cache.remove(i);
                return value;
            }
        }
        return null;
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        cache.clear();
        calls = 0;
        hits = 0;
    }

    /**
     * Returns a map copy of the cache for compatibility.
     */
    public LinkedHashMap<K, V> getCachedNodes() {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (V item : cache) {
            map.put(item.getKey(), item);
        }
        return map;
    }

    /**
     * Returns the cache hit rate as a percentage.
     */
    public double getCacheHitPercent() {
        return calls == 0 ? 0 : ((double) hits / calls) * 100.0;
    }

    @Override
    public String toString() {
        double hitRatio = (calls == 0) ? 0 : ((double) hits / calls) * 100;
        return String.format(
            "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "Cache with %d entries has been created\n" +
            "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "Total number of references:        %d\n" +
            "Total number of cache hits:        %d\n" +
            "Cache hit percent:                 %.2f%%\n",
            maxSize, calls, hits, hitRatio
        );
    }
    
}
