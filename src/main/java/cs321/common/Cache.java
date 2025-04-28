package cs321.common;

import java.util.LinkedHashMap;

/**
 * This program creates and manages a cache for the BTree.
 */
public class Cache<K, V extends KeyInterface<K>> {
    // Instance variables
    private LinkedHashMap<K, V> cachedNodes;
    private int cacheSize;
    private int cacheHits;
    private int numberOfGetCalls;

    /**
     * Constructs a Cache with the specified size.
     *
     * @param size the maximum number of elements the cache can hold
     */
    public Cache(int size) {
        cachedNodes = new LinkedHashMap<>();
        cacheSize = size;
        cacheHits = 0;
        numberOfGetCalls = 0;
    }

    /**
     * Retrieves a node from the cache by its key.
     *
     * @param targetAddress the key to search for
     * @return the cached node if found, otherwise null
     */
    public V get(K targetAddress) {
        numberOfGetCalls++;
        V retVal = cachedNodes.get(targetAddress);
        if (retVal != null) {
            cacheHits++;
        }
        return retVal;
    }

    /**
     * Adds a node to the cache. If the cache is full, removes the oldest entry.
     *
     * @param newNode the node to add
     * @return the removed node if the cache was full, otherwise null
     */
    public V add(V newNode) {
        V retVal = null;
        if (cachedNodes.size() == cacheSize) {
            retVal = cachedNodes.entrySet().iterator().next().getValue();
            cachedNodes.remove(retVal.getKey());
        }
        cachedNodes.put(newNode.getKey(), newNode);
        return retVal;
    }

    /**
     * Clears all cached nodes.
     */
    public void clear() {
        cachedNodes.clear();
    }

    /**
     * Returns the current cached nodes.
     *
     * @return a LinkedHashMap of cached nodes
     */
    public LinkedHashMap<K, V> getCachedNodes() {
        return this.cachedNodes;
    }

    /**
     * Calculates and returns the cache hit percentage.
     *
     * @return the cache hit rate as a percentage
     */
    public double getCacheHitPercent() {
        return ((double) this.cacheHits / this.numberOfGetCalls) * 100;
    }
}
