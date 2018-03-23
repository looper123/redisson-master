package com.quark.redisson.mapper;

import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RInject;
import org.redisson.api.mapreduce.RCollectionMapper;
import org.redisson.api.mapreduce.RCollector;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * 把数据处理成key value形式 便于下面reducer处理 ----集合 collection类型的数据
 */
public class WordCollectionMapper implements RCollectionMapper<String, String, Integer> {

    @RInject
    private RedissonClient redissonClient;

    @Override
    public void map(String value, RCollector<String, Integer> collector) {
        //        收集原始数据
        String[] words = value.split("[^a-zA-Z]");
        for (String word : words) {
            collector.emit(word, 1);
        }
    }
}
