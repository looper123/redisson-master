package com.quark.redisson.task;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.redisson.api.annotation.RInject;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * 分布式runnable 服务
 */
public class RunnableTask implements Runnable {

    @RInject
    private RedissonClient redissonClient;

    private long param;

    public RunnableTask() {
    }

    public RunnableTask(long param) {
        this.param = param;
    }

    @Override
    public void run() {
        RAtomicLong atomic = redissonClient.getAtomicLong("myAtomic");
        atomic.addAndGet(param);
    }
}
