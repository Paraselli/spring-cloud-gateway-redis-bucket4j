package com.example.gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketState;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    public static final String CACHE_NAME = "gateway-rate-limits";

    @Bean
    public Cache<String, RemoteBucketState> rateLimitCache() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();

        MutableConfiguration<String, RemoteBucketState> configuration =
                new MutableConfiguration<String, RemoteBucketState>()
                        .setTypes(String.class, RemoteBucketState.class)
                        .setStoreByValue(false);

        Cache<String, RemoteBucketState> cache =
                cacheManager.getCache(CACHE_NAME);

        if (cache == null) {
            cache = cacheManager.createCache(CACHE_NAME, configuration);
        }

        return cache;
    }

    @Bean
    public ProxyManager<String> proxyManager(
            Cache<String, RemoteBucketState> rateLimitCache) {

        return new JCacheProxyManager<>(
                rateLimitCache,
                Mapper.STRING
        );
    }

    @Bean
    public BucketConfiguration bucketConfiguration() {

        Refill refill = Refill.greedy(
                5,
                Duration.ofMinutes(1)
        );

        Bandwidth limit = Bandwidth.classic(5, refill);

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}
