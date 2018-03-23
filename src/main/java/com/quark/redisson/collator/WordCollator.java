package com.quark.redisson.collator;

import org.redisson.api.mapreduce.RCollator;

import java.util.Map;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * 把经过mapper、reducer处理过的数据简化为单一对象
 */
public class WordCollator implements RCollator<String, Integer, Integer> {
    @Override
    public Integer collate(Map<String, Integer> resultMap) {
        int result = 0;
        for (Integer count : resultMap.values()) {
            result += count;
        }
        return result;
    }
}
