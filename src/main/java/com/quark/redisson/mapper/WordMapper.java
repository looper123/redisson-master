package com.quark.redisson.mapper;

import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RInject;
import org.redisson.api.mapreduce.RCollector;
import org.redisson.api.mapreduce.RMapper;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * 把数据处理成key value形式 便于下面reducer处理  ----map 映射类型数据
 */
public class WordMapper implements RMapper<String, String, String, Integer> {

    @RInject
    private RedissonClient redissonClient;

    public void map(String key, String value, RCollector<String, Integer> collector) {
//        收集原始数据
        String[] words = value.split("[^a-zA-Z]");
        for (String word : words) {
            collector.emit(word, 1);
        }
    }
}
