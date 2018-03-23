package com.quark.redisson.entity;

import org.redisson.api.annotation.REntity;
import org.redisson.api.annotation.RId;

/**
 * Created by ZhenpengLu on 2018/3/23.
 * 分布式实时对象
 */
@REntity
public class DistributeEntity {

    @RId
    private String  id;

    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
