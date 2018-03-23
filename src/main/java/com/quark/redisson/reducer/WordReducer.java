package com.quark.redisson.reducer;

import org.redisson.api.mapreduce.RReducer;

import java.util.Iterator;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * 处理从mapper中传来的数据
 * note：泛型的类型必须和 mapper中定义的 outputkey 、outputvalue类型对应
 */
public class WordReducer implements RReducer<String, Integer> {

    @Override
    public Integer reduce(String reducedKey, Iterator<Integer> iter) {
        int sum = 0;
        while (iter.hasNext()) {
            Integer i = (Integer) iter.next();
            sum += i;
        }
        return sum;

    }
}
