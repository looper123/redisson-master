package com.quark.redisson.task;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RInject;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * callable 分布式服务
 */
@Component(value = "callableTask")
public class CallableTask implements Callable<Object> {

    @RInject
    private RedissonClient redissonClient;

    @Override
    public Object call() throws Exception {
        RMap<String, Integer> map = redissonClient.getMap("myMap");
        Long result = 0L;
        for (Integer value : map.values()) {
            //取消任务
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            result += value;
        }
        return result;
    }
}
