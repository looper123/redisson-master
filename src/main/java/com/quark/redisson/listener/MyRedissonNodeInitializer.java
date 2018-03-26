package com.quark.redisson.listener;

import com.quark.redisson.remote.service.DemoService;
import com.quark.redisson.remote.service.impl.DemoServiceImpl;
import org.redisson.RedissonNode;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonNodeInitializer;
import org.redisson.api.annotation.RInject;

/**
 * Created by ZhenpengLu on 2018/3/26.
 * redisson node 监听器  用来监听 redisson client 发布的分布式执行服务 、分布式调度执行服务、分布式远程服务
 */
public class MyRedissonNodeInitializer implements RedissonNodeInitializer {

    @RInject
    private RedissonClient redissonClient;

    @Override
    public void onStartup(RedissonNode redissonNode) {
        // ...
        // 或
        redissonClient.getRemoteService("myRemoteService").register(DemoService.class, new DemoServiceImpl());
        // 或
        redissonClient.getTopic("myNotificationTopic").publish("New node has joined. id:" + redissonNode.getId() + " remote-server:" + redissonNode.getRemoteAddress());
    }
}
