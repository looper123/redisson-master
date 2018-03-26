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
 * redisson  按照spring cache 实现了redis 和spring 的无缝对接
 * 支持直接从json、yml文件中读取配置
 */
@Configuration
@ComponentScan
@EnableCaching
public class SpringCacheIntegrationFromConfig {

//    @Value(value = "classpath:redisson-config.json")
//    private Resource jsonResource;
//
//    @Value(value = "classpath:redisson-config.yml")
//    private Resource ymlResource;

    @Bean(destroyMethod="shutdown")
//        直接从json、yml文件中读取配置
        RedissonClient redisson(@Value("classpath:/redisson-config.json") Resource jsonConfig) throws IOException {
        Config config = Config.fromJSON(jsonConfig.getInputStream());
        return Redisson.create(config);
    }

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient) {
//        直接从json、yml文件中读取配置
        return new RedissonSpringCacheManager(redissonClient, "classpath:/redisson-config.json");
    }

}
