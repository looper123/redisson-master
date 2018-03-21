package com.quark.redisson;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedissonMasterApplicationTests {

    private Config config;

    private RedissonClient client;

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Test
    public void contextLoads() {
    }

    @Before
    public void propertySet() {
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
    public void synAndAsynExecutor() {
        //原子长整型
        RAtomicLong longObject = client.getAtomicLong("myLong");
        longObject.set(3);
        // 同步执行
        longObject.compareAndSet(3, 401);
        //异步执行
        RFuture<Boolean> future = longObject.compareAndSetAsync(3, 401);
//		添加监听实现非阻塞执行方式 jdk 1.8+
//		监听同步执行结果
        future.whenComplete((res, exception) -> {
        });
//		异步监听执行结果
        future.whenCompleteAsync((res, exception) -> {
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
    public void keyOperationTest() {
        RKeys keys = client.getKeys();
        Iterable<String> iterable = keys.getKeysByPattern("redisson*");
        Iterator<String> iterator1 = iterable.iterator();
        while (iterator1.hasNext()) {
            System.out.println(iterator1.next() + "-------------");
        }
        long count = keys.count();
//		Iterable<String> iterator = keys.getKeys();
    }


    //通用对象桶 用来操作各种类型的对象
    @Test
    public void bucketOperationTest() {
//		RBucket<T> 中的T 支持任意类型
        RBucket<String> bucket = client.getBucket("key_test");
        bucket.set("value_test");
        String value = bucket.get();
        System.out.println(bucket.get() + "-------------");
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


    //	redis发布订阅
//	在redis 故障迁移（主从切换/断线重连）后，会自动完成topic的重新订阅
    @Test
    public void pubSubTest() {
        RTopic<String> rTopic = client.getTopic("topic_1");
//		订阅多个topic
        client.getPatternTopic("topic*");
        rTopic.addListener(new MessageListener<String>() {
            @Override
            public void onMessage(String channel, String msg) {
                System.out.println("channel----" + channel);
                System.out.println("msg----" + msg);
            }
        });
        rTopic.publish("news_1");
    }

    //	布隆过滤器  (集合查询效率和空间利用率很高 但是存在一定的误识别率和删除困难的缺点)
    @Test
    public void bloomfilterTest() {
        RBloomFilter<Object> bloomFilter = client.getBloomFilter("sample_filter");
        bloomFilter.tryInit(50000l, 0.01);
        bloomFilter.add("123");
        bloomFilter.add("1234");
        bloomFilter.add("12345");
        boolean exist = bloomFilter.contains("1234");
        assert exist = true;
    }

    //	二进制流
    @Test
    public void binaryStreamTest() throws IOException {
        RBinaryStream stream = client.getBinaryStream("sample_stream");
        byte[] byteStream = "写入数据".getBytes();
        //写入数据
        InputStream inputStream = stream.getInputStream();
        inputStream.read(byteStream);
        //写出数据
        OutputStream outputStream = stream.getOutputStream();
        outputStream.write("写出数据".getBytes());
    }


    //	地理空间对象桶
    @Test
    public void geoBucketTest() {
        RGeo<String> geo = client.getGeo("sample_geo");
        geo.add(new GeoEntry(13.361389, 38.115556, "Palermo"),
                new GeoEntry(15.087269, 37.502669, "Catania"));
        geo.addAsync(37.618423, 55.751244, "Moscow");
        Double distance = geo.dist("Palermo", "Catania", GeoUnit.METERS);
        geo.hashAsync("Palermo", "Catania");
        Map<String, GeoPosition> positions = geo.pos("test2", "Palermo", "test3", "Catania", "test1");
        List<String> cities = geo.radius(15, 37, 200, GeoUnit.KILOMETERS);
        Map<String, GeoPosition> citiesWithPositions = geo.radiusWithPosition(15, 37, 200, GeoUnit.KILOMETERS);
    }


    //	分布式可伸缩式位向量
    @Test
    public void bitSetTest() {
        RBitSet set = client.getBitSet("sample_bitset");
        set.set(0, true);
        set.set(1812, false);
        set.clear(0);
        set.andAsync("e");
        set.xor("anotherBitset");
    }

    //	整长型累加器
    @Test
    public void longAdderTest() {
        RLongAdder longAdder = client.getLongAdder("sample_longAdder");
        longAdder.add(1000000);
        longAdder.increment();
        longAdder.increment();
        longAdder.decrement();
        longAdder.decrement();
        longAdder.decrement();
        long result = longAdder.sum();
    }

    //	分布式集合  rmap 实现了java的concurrentmap 和map接口  并且保证了元素的插入顺序
//	当把map中的所有元素remove掉后 map也会消失
    @Test
    public void rmapTest() {
        RMap<Object, Object> map = client.getMap("sample_map");
//		能够返回之前的value值
        Object oldValue = map.put("key", "value");
        Object delValue = map.remove("key");
//		无法返回之前的value值 但是速度比put更快
        map.fastPut("key_f", "value_f");
        map.fastRemove("key_f");
        RFuture<Object> rFuture = map.putAsync("s_key", "s_value");
//		监听异步的响应结果
        rFuture.whenCompleteAsync((res, exceptrion) -> {

        });
        RFuture<Object> rFuture1 = map.removeAsync("s_key");
        map.fastPutAsync("s_key_f", "s_value_f");
        map.fastRemoveAsync("s_key_f");
    }

    //	映射（map）的字段锁
//	map还带有元素淘汰、本地缓存、数据分片功能
//	元素淘汰：针对映射中的每一个元素设置有效时间 和最长闲置时间
//	本地缓存：高频繁的读取动作使得网络通讯成为瓶颈  这时redisson在和redis通讯的同时把部分数据保存在本地 以提高读取速度
//	数据分片：仅仅适用于集群环境下 利用分库原理 把单一的映射（map）结构切分成若干小映射 均匀分布在集群的各个槽里，使数据真正做到均匀分布
    @Test
    public void mapLockTest() {
        RMap<Object, Object> map_lock = client.getMap("sample_map_lock");
//		获取key锁
        RLock lock_key = map_lock.getLock("lock_key");
//		加锁
        lock_key.lock();
        //.......some logic code for  current key
//		释放锁
        lock_key.unlock();

//		读写锁
        RReadWriteLock rwLock = map_lock.getReadWriteLock("lock_key");
//		对写入操作加锁
        rwLock.writeLock().lock();
        //......some logic code for current key
//		释放锁
        rwLock.writeLock().unlock();
    }

    //	元素淘汰
    @Test
    public void elementWeedOutTest() {
        RMapCache<String, String> map = client.getMapCache("anyMap");
//        String value_1 = map.get("key1");
//        String value_2 = map.get("key2");
// 有效时间 ttl = 10分钟
        map.put("key1", "value1", 10, TimeUnit.MINUTES);
// 有效时间 ttl = 10分钟, 最长闲置时间 maxIdleTime = 10秒钟
        map.put("key1", "value1", 10, TimeUnit.MINUTES, 10, TimeUnit.SECONDS);

// 有效时间 = 3 秒钟
        map.putIfAbsent("key2", "value2", 3, TimeUnit.SECONDS);
// 有效时间 ttl = 40秒钟, 最长闲置时间 maxIdleTime = 10秒钟
        map.putIfAbsent("key2", "value2", 40, TimeUnit.SECONDS, 10, TimeUnit.SECONDS);
    }

    //    本地缓存功能
    @Test
    public void localCacheTest() {
        LocalCachedMapOptions options = LocalCachedMapOptions.defaults()
                // 用于淘汰清除本地缓存内的元素
                // 共有以下几种选择:
                // LFU - 统计元素的使用频率，淘汰用得最少（最不常用）的。
                // LRU - 按元素使用时间排序比较，淘汰最早（最久远）的。
                // SOFT - 元素用Java的WeakReference来保存，缓存元素通过GC过程清除。
                // WEAK - 元素用Java的SoftReference来保存, 缓存元素通过GC过程清除。
                // NONE - 永不淘汰清除缓存元素。
                .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
                // 如果缓存容量值为0表示不限制本地缓存容量大小
                .cacheSize(1000)
                // 以下选项适用于断线原因造成了未收到本地缓存更新消息的情况。
                // 断线重连的策略有以下几种：
                // CLEAR - 如果断线一段时间以后则在重新建立连接以后清空本地缓存
                // LOAD - 在服务端保存一份10分钟的作废日志
                //        如果10分钟内重新建立连接，则按照作废日志内的记录清空本地缓存的元素
                //        如果断线时间超过了这个时间，则将清空本地缓存中所有的内容
                // NONE - 默认值。断线重连时不做处理。
                .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR)
                // 以下选项适用于不同本地缓存之间相互保持同步的情况
                // 缓存同步策略有以下几种：
                // INVALIDATE - 默认值。当本地缓存映射的某条元素发生变动时，同时驱逐所有相同本地缓存映射内的该元素
                // UPDATE - 当本地缓存映射的某条元素发生变动时，同时更新所有相同本地缓存映射内的该元素
                // NONE - 不做任何同步处理
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
                // 每个Map本地缓存里元素的有效时间，默认毫秒为单位
                .timeToLive(10000)
                // 或者
                .timeToLive(10, TimeUnit.SECONDS)
                // 每个Map本地缓存里元素的最长闲置时间，默认毫秒为单位
                .maxIdle(10000)
                // 或者
                .maxIdle(10, TimeUnit.SECONDS);
//               本地缓存需要和RLocalCachedMap<K,T> 对象一起使用  把上面定义的option 作为参数传入方法中
                RLocalCachedMap<String, Integer> map = client.getLocalCachedMap("sample_map_cache", options);
                Integer prevObject = map.put("123", 1);
                Integer currentObject = map.putIfAbsent("323", 2);
                Integer obj = map.remove("123");
                // 在不需要旧值的情况下可以使用fast为前缀的类似方法
                map.fastPut("a", 1);
                map.fastPutIfAbsent("d", 32);
                map.fastRemove("b");
                RFuture<Integer> putAsyncFuture = map.putAsync("key", 111);
                RFuture<Boolean> fastPutAsyncFuture = map.fastPutAsync("key", 222);
                map.fastPutAsync("key", 123);
                map.fastRemoveAsync("key");
            // 当不再使用缓存本地对象时 应该把map销毁 除非client已经关闭
            // map.destroy();
            //  这里的key value的类型应该和RLocalCachedMap保持一致
                Map<String, Integer> entryMap = new HashMap<>();
                // 对RLocalCachedMap 的批量put
                entryMap.put("hhah", 25);
                map.putAll(entryMap);
//              清理缓存在本地的映射
                map.clearLocalCache();
//               清理redis中的映射
//                map.clear();
    }

            //    数据分片功能
            @Test
            public void dataShardingTest(){

            }


}
