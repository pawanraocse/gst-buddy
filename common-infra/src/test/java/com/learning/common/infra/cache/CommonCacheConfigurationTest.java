package com.learning.common.infra.cache;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CommonCacheConfiguration.
 * Verifies that all expected caches are properly configured.
 */
class CommonCacheConfigurationTest {

    private final CommonCacheConfiguration config = new CommonCacheConfiguration();

    @Test
    void cacheManagerCreatesPermissionsCache_WithoutRedisson() {
        ObjectProvider<RedissonClient> nullProvider = new ObjectProvider<>() {
            @Override
            public RedissonClient getObject() { throw new UnsupportedOperationException(); }
            @Override
            public RedissonClient getObject(Object... args) { throw new UnsupportedOperationException(); }
            @Override
            public RedissonClient getIfAvailable() { return null; }
            @Override
            public RedissonClient getIfUnique() { return null; }
        };

        CacheManager cacheManager = config.cacheManager(nullProvider);

        assertThat(cacheManager.getCache(CacheNames.PERMISSIONS))
                .as("Permissions cache should exist")
                .isNotNull();
    }

    @Test
    void cacheNamesAreCorrectConstants() {
        // Verify the constants match expected values (prevents typos)
        assertThat(CacheNames.PERMISSIONS).isEqualTo("permissions");
    }

    @Test
    void distributedCachesSetContainsExpectedCaches() {
        assertThat(CacheNames.DISTRIBUTED_CACHES)
                .contains(CacheNames.PERMISSIONS)
                .contains(CacheNames.USER_PERMISSIONS)
                .contains(CacheNames.USER_ALL_PERMISSIONS);
    }

    @Test
    void localCachesSetIsEmpty() {
        assertThat(CacheNames.LOCAL_CACHES).isEmpty();
    }

    @Test
    void isDistributedReturnsTrueForDistributedCaches() {
        assertThat(CacheNames.isDistributed(CacheNames.PERMISSIONS)).isTrue();
        assertThat(CacheNames.isDistributed(CacheNames.USER_PERMISSIONS)).isTrue();
    }
}
