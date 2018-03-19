package com.quark.redisson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;

import javax.accessibility.AccessibleStateSet;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedissonMasterApplicationTests {

	private Config config;

	private RedissonClient client;

	@Rule
	public  OutputCapture outputCapture = new OutputCapture();

	@Test
	public void contextLoads() {
	}

	@Before
	public void propertySet(){
		Config config = new Config();
		config.setTransportMode(TransportMode.NIO);
		/**
		 * 三种传输模式
		 * 	nio 默认
		 *  eqoll linux
		 *  kqueue macOS
		 */
		config.useSingleServer()
				.setAddress("redis://192.168.194.130:6379");
		this.config = config;
		this.client = Redisson.create(config);
	}


//	同步执行 、异步执行、异步流执行
	@Test
	public void synAndAsynExecutor(){
		//原子长整型
		RAtomicLong longObject = client.getAtomicLong("myLong");
		longObject.set(3);
		// 同步执行
		longObject.compareAndSet(3, 401);
		//异步执行
		RFuture<Boolean> future = longObject.compareAndSetAsync(3, 401);
//		添加监听实现非阻塞执行方式 jdk 1.8+
//		监听同步执行结果
		future.whenComplete((res,exception) -> {
		});
//		异步监听执行结果
		future.whenCompleteAsync((res,exception) -> {
		});
// 		异步流执行方式 require jdk1.9
//		RedissonReactiveClient client = Redisson.createReactive(config);
//		RAtomicLongReactive longObject = client.getAtomicLong('myLong');
//		longObject.compareAndSet(3, 401);
	}

	//redisson 读取配置的方式
	@Test
	public void redissonConfigReadTest() throws IOException {
//		从.json文件中读取配置
		Config config_json = config.fromJSON(new File("H:\\idea workspace\\redisson-master\\src\\main\\resources\\redisson.json"));
//		从.yml文件中读取配置
		Config config_yml = config.fromYAML(new File("H:\\idea workspace\\redisson-master\\src\\main\\resources\\redisson.yml"));
	}


	//所有与Redis key相关的操作都归纳在RKeys接口中
	@Test
	public void keyOperationTest(){
		RKeys keys = client.getKeys();
		Iterable<String> iterable = keys.getKeysByPattern("redisson*");
		Iterator<String> iterator1 = iterable.iterator();
		while(iterator1.hasNext()){
			System.out.println(iterator1.next()+"-------------");
		}
		long count = keys.count();
//		Iterable<String> iterator = keys.getKeys();
	}


	//通用对象桶 用来操作各种类型的对象
	@Test
	public void bucketOperationTest(){
//		RBucket<T> 中的T 支持任意类型
		RBucket<String> bucket = client.getBucket("key_test");
		bucket.set("value_test");
		String value = bucket.get();
		System.out.println(bucket.get()+"-------------");
	}

}
