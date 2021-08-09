package jcache.web;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@CacheDefaults(cacheName="zombies")
public class ZombiePod {
    @CacheResult
    public String getZombie(@CacheKey Integer z, String defaultName) {
        return defaultName; // only returned if not found in cache
    }

    @CachePut
    public void addOrReplaceZombie(@CacheKey Integer z, @CacheValue String name) {
    }

    @CacheRemove
    public void removeZombie(@CacheKey Integer z) {
    }

    @CacheRemoveAll
    public void emptyPod() {        
    }
}
