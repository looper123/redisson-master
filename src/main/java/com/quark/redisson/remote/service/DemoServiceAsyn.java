package com.quark.redisson.remote.service;

import org.redisson.api.RFuture;
import org.redisson.api.annotation.RRemoteAsync;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * 异步远程调用接口
 */
//必须匹配远程调用接口名称
@RRemoteAsync(DemoService.class)
public interface DemoServiceAsyn {

//    返回类型必须匹配RFuture<T>  其中T为同步接口的返回类型
//    方便redisson 监听异步响应信息
    public RFuture<Object> invoke();
}
