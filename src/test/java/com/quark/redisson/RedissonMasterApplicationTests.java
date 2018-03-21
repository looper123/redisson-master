package com.quark.redisson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.map.MapLoader;
import org.redisson.api.map.MapWriter;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryEvent;
import org.redisson.api.map.event.EntryExpiredListener;
import org.redisson.api.map.event.EntryRemovedListener;
import org.redisson.api.map.event.EntryUpdatedListener;
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
import java.util.concurrent.TimeUnit;

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

//	映射持久化方式
//	read-through 加载策略
// writer-through 同步写入策略
//	writer-behind 异步写入策略
	@Test
	public void mapDurableStoreTest(){
//		options for RMap & RMapCache
		MapOptions<String, Object> options = MapOptions.<String, Object>defaults().
//				使用自定义加载器和写入器
				loader(new CustomerLoader<>()).
				writer(new CustomerWriter<>());
//		options for RLocalCachedMap
		LocalCachedMapOptions<String, Object> localCachedMapOptions = LocalCachedMapOptions.<String, Object>defaults().
				loader(new CustomerLoader<>()).
				writer(new CustomerWriter<>());
		RMap<String, Object> map1 = client.getMap("sample_durable", options);
// 或
		RMapCache<String, Object> map2 = client.getMapCache("sample_durable", options);
// 或
		RLocalCachedMap<String, Object> map3 = client.getLocalCachedMap("sample_durable", localCachedMapOptions);
// 或
//		RLocalCachedMapCache<String, Object> map4 = client.getLocalCachedMapCache("test", options);
	}


//	映射监听器 redisson为所有实现了RMapCache | RLocalCacheMapCache的接口的对象都提供了以下事件监听
//	元素添加 org.redisson.api.map.event.EntryCreatedListener
//	元素过期 org.redisson.api.map.event.EntryExpiredListener
//	元素删除 org.redisson.api.map.event.EntryRemovedListener
//	元素更新 org.redisson.api.map.event.EntryUpdatedListener
	@Test
	public  void MapListenerTest(){
		RMapCache<String, Object> map2 = client.getMapCache("sample_durable");
//		update event listener
		int updateListener = map2.addListener(new EntryUpdatedListener<String, Object>() {
			@Override
			public void onUpdated(EntryEvent<String, Object> entryEvent) {
//				do something you like  here
				String key = entryEvent.getKey();
				Object value = entryEvent.getValue();
				Object oldValue = entryEvent.getOldValue();
//				...
			}
		});
//		add event listener
		int createListener = map2.addListener(new EntryCreatedListener<Integer, Integer>() {
			@Override
			public void onCreated(EntryEvent<Integer, Integer> event) {
				event.getKey(); // 字段名
				event.getValue(); // 值
				// ...
			}
		});
//		expire event listener
		int expireListener = map2.addListener(new EntryExpiredListener<Integer, Integer>() {
			@Override
			public void onExpired(EntryEvent<Integer, Integer> event) {
				event.getKey(); // 字段名
				event.getValue(); // 值
				// ...
			}
		});
//		delete event listener
		int removeListener = map2.addListener(new EntryRemovedListener<Integer, Integer>() {
			@Override
			public void onRemoved(EntryEvent<Integer, Integer> event) {
				event.getKey(); // 字段名
				event.getValue(); // 值
				// ...
			}
		});
//		remove listeners
		map2.removeListener(updateListener);
		map2.removeListener(createListener);
		map2.removeListener(expireListener);
		map2.removeListener(removeListener);
	}


//	  基于redis LRU回收策略的 LRU有界映射 可以主动移除超过映射容量的元素
	@Test
	public  void   LRULimitMapTest(){
		RMapCache<String, Object> map = client.getMapCache("map");
// 尝试将该映射的最大容量限制设定为10
		map.trySetMaxSize(10);
// 将该映射的最大容量限制设定或更改为10
		map.setMaxSize(10);
		map.put("1", "2");
		map.put("3", "3", 1, TimeUnit.SECONDS);
	}


}
