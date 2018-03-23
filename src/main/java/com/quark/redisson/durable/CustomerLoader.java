package com.quark.redisson.durable;

import org.redisson.api.map.MapLoader;

/**
 * Created by ZhenpengLu on 2018/3/21.
 * 映射持久化
 * 自定义loader  用来加载数据
 */
public class CustomerLoader<K,V> implements MapLoader<K,V> {
    @Override
    public Object load(Object o) {
        return null;
    }

    @Override
    public Iterable loadAllKeys() {
        return null;
    }
}
