package com.quark.redisson.durable;

import org.redisson.api.map.MapWriter;

import java.util.Collection;
import java.util.Map;

/**
 * Created by ZhenpengLu on 2018/3/21.
 * 映射持久化
 * 自定义writer map 用来写出数据
 */
public class CustomerWriter<K,V>  implements MapWriter<K,V> {
    @Override
    public void write(K k, V v) {

    }

    @Override
    public void writeAll(Map<K, V> map) {

    }

    @Override
    public void delete(K k) {

    }

    @Override
    public void deleteAll(Collection<K> collection) {

    }
}
