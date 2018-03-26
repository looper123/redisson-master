package com.quark.redisson.integration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ZhenpengLu on 2018/3/26.
 * redisson 实现了redis 和spring的无缝对接
 */
@Configuration
@ComponentScan
@EnableCaching
public class SpringIntegration {
    @Bean(destroyMethod="shutdown")
    RedissonClient redisson() throws IOException {
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress("192.168.194.130:7004", "192.168.194.130:7001");
        return Redisson.create(config);
    }

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<String, CacheConfig>();
        // 创建一个名称为"testMap"的缓存，过期时间ttl为24分钟，同时最长空闲时maxIdleTime为12分钟。
        config.put("testMap", new CacheConfig(24*60*1000, 12*60*1000));
        return new RedissonSpringCacheManager(redissonClient, config);
    }

}
