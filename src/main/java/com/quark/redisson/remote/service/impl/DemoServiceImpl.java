package com.quark.redisson.remote.service.impl;

import com.quark.redisson.remote.service.DemoService;

/**
 * Created by ZhenpengLu on 2018/3/23.
 */
public class DemoServiceImpl implements DemoService {
    @Override
    public Object invoke() {
        System.out.println("invoke method is executing....");
        return null;
    }
}
