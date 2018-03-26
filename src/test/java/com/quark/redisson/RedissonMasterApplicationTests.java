package com.quark.redisson;

import com.quark.redisson.collator.WordCollator;
import com.quark.redisson.durable.CustomerLoader;
import com.quark.redisson.durable.CustomerWriter;
import com.quark.redisson.entity.DistributeEntity;
import com.quark.redisson.entity.PropertyEntity;
import com.quark.redisson.mapper.WordCollectionMapper;
import com.quark.redisson.mapper.WordMapper;
import com.quark.redisson.reducer.WordReducer;
import com.quark.redisson.remote.service.DemoService;
import com.quark.redisson.remote.service.DemoServiceAsyn;
import com.quark.redisson.remote.service.impl.DemoServiceImpl;
import com.quark.redisson.task.CallableTask;
import com.quark.redisson.task.RunnableTask;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.RedissonMultiLock;
import org.redisson.RedissonNode;
import org.redisson.RedissonRedLock;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryEvent;
import org.redisson.api.map.event.EntryExpiredListener;
import org.redisson.api.map.event.EntryRemovedListener;
import org.redisson.api.map.event.EntryUpdatedListener;
import org.redisson.api.mapreduce.RCollectionMapReduce;
import org.redisson.api.mapreduce.RMapReduce;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnection;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.config.Config;
import org.redisson.config.RedissonNodeConfig;
import org.redisson.config.TransportMode;
import org.redisson.connection.ConnectionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RedissonMasterApplicationTests {

    private Config config;

    @Value(value="classpath:config.json")
    private Resource jsonResource;

    @Value(value="classpath:config.yml")
    private Resource ymlResource;

    private RedissonClient client;
    private RedissonClient client2;
    private RedissonClient client3;

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
        this.client2 = Redisson.create(config);
        this.client3 = Redisson.create(config);
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
//		从.json文件中读取配置 支持流 、path、url。。。读取方式
        Config config_json = config.fromJSON(jsonResource.getInputStream());
//		从.yml文件中读取配置
        Config config_yml = config.fromYAML(ymlResource.getInputStream());
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

    //数据分片功能  只在集群模式下有效
//            @Test
//            public void dataShardingTest(){
//				RClusteredMap<String, Object> map = client.getClusteredMap("sample_cluster_map");
//				Object prevObject = map.put("123", "test");
//				Object currentObject = map.putIfAbsent("323", "test");
//				Object obj = map.remove("123");
//				map.fastPut("321", "test");
//				map.fastRemove("321");
//            }


    //	映射持久化方式
//	read-through 加载策略
// writer-through 同步写入策略
//	writer-behind 异步写入策略
    @Test
    public void mapDurableStoreTest() {
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
    public void MapListenerTest() {
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
    public void LRULimitMapTest() {
        RMapCache<String, Object> map = client.getMapCache("map");
// 尝试将该映射的最大容量限制设定为10
        map.trySetMaxSize(10);
// 将该映射的最大容量限制设定或更改为10
        map.setMaxSize(10);
        map.put("1", "2");
        map.put("3", "3", 1, TimeUnit.SECONDS);
    }

    // 基于set的多值映射 & 淘汰机制
    @Test
    public void multiMapTest() {
        RSetMultimap<String, String> setMultiMap = client.getSetMultimap("sample_set_multiMap");
        setMultiMap.put("key", "multivalue_1");
        setMultiMap.put("key", "multivalue_2");
        setMultiMap.put("key1", "multivalue_3");
        setMultiMap.put("key1", "multivalue_4");
        RSet<String> multiSet = setMultiMap.get("key");
        List<String> newValues = Arrays.asList("7", "6", "5");
        Set<String> oldValues = setMultiMap.replaceValues("0", newValues);
        Set<String> removedValues = setMultiMap.removeAll("0");
//        set中的淘汰机制： redis本身暂不支持set中元素淘汰，因此所有的元素都是通过org.redisson.EvictionScheduler实例来
//         实现定期清理。而且当下次清理的数据量比上次少时，清理时间间隔也会随之边长。
        RSetMultimapCache<String, String> setMultimapCache = client.getSetMultimapCache("sample_set_multiMap");
        setMultimapCache.expireKey("key", 10, TimeUnit.SECONDS);

    }

    //    基于列表的多值映射
    @Test
    public void listMultiMapTest() {
        RListMultimap<String, String> listMultiMap = client.getListMultimap("sample_list_multiMap");
        listMultiMap.put("key", "multivalue_1");
        listMultiMap.put("key", "multivalue_2");
        listMultiMap.put("key1", "multivalue_3");
        listMultiMap.put("key1", "multivalue_4");
        RList<String> valueList = listMultiMap.get("key");
        Collection<String> newValues = Arrays.asList("7", "6", "5");
        List<String> oldValues = listMultiMap.replaceValues("0", newValues);
        List<String> removedValues = listMultiMap.removeAll("0");
    }

    //   有序集合sortset
    public void sortSetTest() {
        RSortedSet<Object> sortSet = client.getSortedSet("sample_sort_set");
//            sortSet.trySetComparator(new MyComparator()); // 配置自定义元素比较器
        sortSet.add(3);
        sortSet.add(1);
        sortSet.add(2);
        sortSet.removeAsync(0);
        sortSet.addAsync(5);
    }

    //    计分排序集
    public void scoreSortSetTest() {
        RScoredSortedSet<String> set = client.getScoredSortedSet("simple_score_sortSet");
        set.add(0.13, "afdaf");
        set.addAsync(0.251, "gahhga");
        set.add(0.302, "yywsaag");
        set.pollFirst();
        set.pollLast();
        int index = set.rank("afdaf"); // 获取元素在集合中的位置
        Double score = set.getScore("afdaf"); // 获取元素的评分
    }


    //    字典排序集  把所有的字符串元素按照字典顺序排列
    public void lexSortedSetTest() {
        RLexSortedSet set = client.getLexSortedSet("simple_lex_sortSet");
        set.add("d");
        set.addAsync("e");
        set.add("f");
        set.rangeTail("d", false);
        set.countHead("e", false);
        set.range("d", true, "z", false);
    }

    //    列表list
    public void listTest() {
        RList<Object> list = client.getList("sample_list");
        list.add("a");
        list.add("b");
        list.readAll();
    }

    //   队列
    public void queueTest() {
        RQueue<Object> queue = client.getQueue("sample_queue");
        queue.add("this is a queue");
        Object poll = queue.poll();
        Object peek = queue.peek();
    }


    //    双端队列
    public void dequeTest() throws InterruptedException {
        RBlockingQueue<String> queue = client.getBlockingQueue("deque");
        queue.offer("");
        String obj = queue.peek();
        String someObj = queue.poll();
        String ob = queue.poll(10, TimeUnit.SECONDS);
    }

    //    分布式无界阻塞队列
    public void blockQueueTest() throws InterruptedException {
        RBlockingDeque<Object> blockQueue = client.getBlockingDeque("sample_block_queue");
        blockQueue.offer("clockQueue");
        Object obj = blockQueue.peek();
        Object someObj = blockQueue.poll();
        Object ob = blockQueue.poll(10, TimeUnit.SECONDS);
    }

    //    有界阻塞队列
    @Test
    public void BoundedBlockQueueTest() throws InterruptedException {
        RBoundedBlockingQueue<Object> queue = client.getBoundedBlockingQueue("sample_bounded_block_queue");
// 如果初始容量（边界）设定成功则返回`真（true）`，
// 如果初始容量（边界）已近存在则返回`假（false）`。
        queue.trySetCapacity(2);
        queue.offer("1");
        queue.offer("2");
// 此时容量已满，下面代码将会被阻塞，直到有空闲为止。
        queue.put("3");
        Object obj = queue.peek();
        Object someObj = queue.poll();
        Object ob = queue.poll(10, TimeUnit.MINUTES);
    }


    //    阻塞双端队列
    @Test
    public void blockDequeTest() throws InterruptedException {
        RBlockingDeque<Object> blockDeque = client.getBlockingDeque("sample_block_deque");
        blockDeque.putFirst(1);
        blockDeque.putLast(2);
        Object firstValue = blockDeque.takeFirst();
        Object lastValue = blockDeque.takeLast();
        blockDeque.pollFirst(10, TimeUnit.SECONDS);
        blockDeque.pollLast(3, TimeUnit.SECONDS);
    }

    //  延迟队列  向队列按要求延迟添加项目
    @Test
    public void delayQueueTest() {
//        定义一个标准queue
        RQueue<String> distinationQueue = client.getQueue("sample_queue");
//        由标准queue生成一个延迟queue
        RDelayedQueue<String> delayedQueue = client.getDelayedQueue(distinationQueue);
// 10秒钟以后将消息发送到指定队列
        delayedQueue.offer("msg1", 10, TimeUnit.SECONDS);
// 一分钟以后将消息发送到指定队列
        delayedQueue.offer("msg2", 1, TimeUnit.MINUTES);
    }

    //    优先队列
    @Test
    public void priorityQueueTest() {
        RPriorityQueue<Integer> queue = client.getPriorityQueue("sample_priority_queue");
//        通过比较器（Comparator）接口来对元素排序
//        queue.trySetComparator(new MyComparator()); // 指定对象比较器
        queue.add(3);
        queue.add(1);
        queue.add(2);
        queue.remove(0);
        queue.add(5);
        queue.poll();
    }


    //    优先双端队列
    @Test
    public void priorityDequeTest() {
        RPriorityDeque<Integer> queue = client.getPriorityDeque("sample_priority_queue");
//        可以通过Comparator接口对元素排序
//        queue.trySetComparator(new MyComparator()); // 指定对象比较器
        queue.addLast(3);
        queue.addFirst(1);
        queue.add(2);
        queue.remove(0);
        queue.add(5);
        queue.pollFirst();
        queue.pollLast();
    }


    //    优先阻塞队列
    @Test
    public void priorityBlockQueueTest() throws InterruptedException {
        RPriorityBlockingQueue<Integer> queue = client.getPriorityBlockingQueue("sample_priority_block_queue");
//        queue.trySetComparator(new MyComparator()); // 指定对象比较器
        queue.add(3);
        queue.add(1);
        queue.add(2);
        queue.removeAsync(0);
        queue.addAsync(5);
        queue.take();
    }

    //    优先双端阻塞队列
    @Test
    public void priorityBlockDequeTest() throws InterruptedException {
        RPriorityBlockingDeque<Integer> queue = client.getPriorityBlockingDeque("sample_priority_block_deque");
//        queue.trySetComparator(new MyComparator()); // 指定对象比较器
        queue.add(2);
        queue.removeAsync(0);
        queue.addAsync(5);
        queue.pollFirst();
        queue.pollLast();
        queue.takeFirst();
        queue.takeLast();
    }


//  ------------------- redisson中的分布式锁和同步器---------------------
//    在lock.lock()  即加锁后  如果保存锁的redis 宕机了 就会出现锁死的情况 为了避免这种情况 redisson专门提供了一个监控锁的看门狗
//        它的作用是在Redisson实例被关闭前，不断的延长锁的有效期。默认情况下，看门狗的检查锁的超时时间是30秒钟，
//         也可以通过修改Config.lockWatchdogTimeout来另行指定。

    //    可重入锁
    @Test
    public void RlockTest() throws InterruptedException {
//        获取锁
        RLock rLock = client.getLock("R_lock");
//        加锁

        // 加锁以后10秒钟自动解锁
        // 无需调用unlock方法手动解锁
        rLock.lock(10, TimeUnit.SECONDS);
        // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
        boolean res = rLock.tryLock(100, 10, TimeUnit.SECONDS);
        rLock.unlock();
    }

    //    公平锁  当有多个redisson客户端同时请求时 优先把锁分配给先发送请求的线程
    @Test
    public void FairLockTest() throws InterruptedException {
        RLock lock = client.getLock("fair_lock");
// 最常见的使用方法
        lock.lock();
        // 加锁以后10秒钟自动解锁
// 无需调用unlock方法手动解锁
        lock.lock(10, TimeUnit.SECONDS);

// 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
        boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
    }


    //    联锁  把多个Rlock对象关联为一个联锁，每一个rlock对象可以来自不同的redisson实例
    @Test
    public void multiLockTest() {
        RLock lock1 = client.getLock("lock1");
        RLock lock2 = client2.getLock("lock2");
        RLock lock3 = client3.getLock("lock3");
        RedissonMultiLock lock = new RedissonMultiLock(lock1, lock2, lock3);
// 同时加锁：lock1 lock2 lock3
// 所有的锁都上锁成功才算成功。
        lock.lock();
        lock.unlock();
    }

    //    redlock  红锁的实现机制：
//    就是采用N（通常是5）个独立的redis节点，同时setnx，如果多数节点成功，就拿到了锁，这样就可以允许少数（2）个节点挂掉了。
//    整个取锁、释放锁的操作和单节点类似，当成功获取到锁的数量大于一半时 就认为锁获取成功了
    @Test
    public void reLockTest() throws InterruptedException {
        RLock lock1 = client.getLock("lock1");
        RLock lock2 = client2.getLock("lock2");
        RLock lock3 = client3.getLock("lock3");
        RedissonRedLock lock = new RedissonRedLock(lock1, lock2, lock3);
// 同时加锁：lock1 lock2 lock3
// 红锁在大部分节点上加锁成功就算成功。
//        lock.lock();
        lock.lock(10, TimeUnit.SECONDS);
        lock.unlock();
        // 给lock1，lock2，lock3加锁，如果没有手动解开的话，10秒钟后将会自动解开
// 为加锁等待100秒时间，并在加锁成功10秒钟后自动解开
        boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
        lock.unlock();
    }

    //    读写锁
    @Test
    public void wrLockTest() throws InterruptedException {
        RReadWriteLock rwlock = client.getReadWriteLock("sample_rw_lock");
// 最常见的使用方法
        rwlock.readLock().lock();
// 或
        rwlock.writeLock().lock();
        // 10秒钟以后自动解锁
// 无需调用unlock方法手动解锁
        rwlock.readLock().lock(10, TimeUnit.SECONDS);
// 或
        rwlock.writeLock().lock(10, TimeUnit.SECONDS);
// 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
        boolean resWLock = rwlock.readLock().tryLock(100, 10, TimeUnit.SECONDS);
// 或
        boolean resRLock = rwlock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
        rwlock.readLock().unlock();
        rwlock.writeLock().unlock();
    }

    //    分布式信号量   （用来限制某个物理、逻辑资源的访问数量）
    @Test
    public void semaphore() throws InterruptedException {
        RSemaphore semaphore = client.getSemaphore("sample_semaphore");
        semaphore.acquire();
//或
        semaphore.acquireAsync();
        semaphore.acquire(23);
        semaphore.tryAcquire();
//或
        semaphore.tryAcquireAsync();
        semaphore.tryAcquire(23, TimeUnit.SECONDS);
//或
        semaphore.tryAcquireAsync(23, TimeUnit.SECONDS);
        semaphore.release(10);
        semaphore.release();
//或
        semaphore.releaseAsync();
    }

    //    可过期信号量
    @Test
    public void permitExpirableSemaphoreTest() throws InterruptedException {
        RPermitExpirableSemaphore semaphore = client.getPermitExpirableSemaphore("sample_expire_semaphore");
//        String permitId = semaphore.acquire();
// 获取一个信号，有效期只有2秒钟。
        String permitId = semaphore.acquire(2, TimeUnit.SECONDS);
        semaphore.release(permitId);
    }

    //     分布式闭锁   允许一个或者多个线程等待一件事情的发生
    @Test
    public void countDownLatchTest() throws InterruptedException {
//        当前线程
        RCountDownLatch latch = client.getCountDownLatch("anyCountDownLatch");
        latch.trySetCount(1);
        latch.await();
// 在其他线程或其他JVM里
        RCountDownLatch latch_down = client.getCountDownLatch("anyCountDownLatch");
        latch_down.countDown();
    }


//    --------------------分布式服务  ---------------------
//需要和redisson node 一起使用  由redisson的客户端来发布服务  redisson node来接收、执行服务
    //   基于redisson的远程rpc
    //  服务端（服务提供者）
    /**
     *  当服务端工作者可用实例 > 1 时，会并行执行被调用的方法
     * 并行执行的工作者数量  =  redissson 服务端的数量 *  服务端注册服务时指定的当前服务端的工作者实例
     *  超过该数量的并发请求会在队列中等候
     */
    @Test
    public void redissonRpcServerTest() throws InterruptedException {
        RRemoteService remoteService = client.getRemoteService();
        DemoServiceImpl demoService = new DemoServiceImpl();
        // 在调用远程方法以前，应该首先注册远程服务
        // 只注册了一个服务端工作者实例，只能同时执行一个并发调用
        remoteService.register(DemoService.class, demoService);
        // 注册了12个服务端工作者实例，可以同时执行12个并发调用
        remoteService.register(DemoService.class, demoService, 12);
//        不让服务停止
        Thread.sleep(500000000l);
    }

    //    客户端  （服务消费者）
    @Test
    public void redissonRpcProviderTest() {
        RRemoteService remoteService = client.getRemoteService();
        //获取服务
        DemoService demoService = remoteService.get(DemoService.class);
        //调用
        demoService.invoke();
    }


//    发送即不管（Fire-and-Forget）模式和应答回执（Ack-Response）模式
    @Test
    public void  differentModelTest(){
        // 应答回执超时1秒钟，远程执行超时30秒钟
        RemoteInvocationOptions options = RemoteInvocationOptions.defaults();

// 无需应答回执，远程执行超时30秒钟
        RemoteInvocationOptions options1 = RemoteInvocationOptions.defaults().noAck();

// 应答回执超时1秒钟，不等待执行结果
        RemoteInvocationOptions options2 = RemoteInvocationOptions.defaults().noResult();

// 应答回执超时1分钟，不等待执行结果
        RemoteInvocationOptions options3 = RemoteInvocationOptions.defaults().expectAckWithin(1, TimeUnit.MINUTES).noResult();

// 发送即不管（Fire-and-Forget）模式，无需应答回执，不等待结果
        RemoteInvocationOptions options4 = RemoteInvocationOptions.defaults().noAck().noResult();

        RRemoteService remoteService = client.getRemoteService();
        DemoService service = remoteService.get(DemoService.class, options);
    }


//    异步远程调用
    @Test
    public  void  asynRemoteRpcTest(){
        RRemoteService remoteService = client.getRemoteService();
        DemoServiceAsyn asyncService = remoteService.get(DemoServiceAsyn.class);
        RFuture<Object> rFuture = asyncService.invoke();
//        取消异步调用
        rFuture.cancel(true);
    }


//      分布式实时对象  可以同时被多个位于不同jvm的线程引用
//    成的代理类（Proxy），将一个指定的普通Java类里的所有字段，以及针对这些字段的操作全部映射到一个Redis Hash的数据结构，
//    实现这种理念。每个字段的get和set方法最终被转译为针对同一个Redis Hash的hget和hset命令，
//    从而使所有连接到同一个Redis节点的所有可以客户端同时对一个指定的对象进行操作
    @Test
    public  void  distributeRealTimeObjectTest(){
//        放入一个RLO实例
        RLiveObjectService service = client.getLiveObjectService();
        DistributeEntity myObject1 = new DistributeEntity();
        myObject1.setId("123");
        myObject1.setName("myName");
        myObject1 = service.<DistributeEntity>persist(myObject1);
//或者取得一个已经存在的RLO实例
        DistributeEntity myObject2 = service.<DistributeEntity, String>get(DistributeEntity.class, "123");
        System.out.println(myObject2.getId());
        System.out.println(myObject2.getName());
    }

//    分布式实时对象中存在的'问题'
    @Test
    public  void  differenceBetweenRLOAndEntity(){
        RLiveObjectService service = client.getLiveObjectService();
        DistributeEntity myObject1 = new DistributeEntity();
        myObject1.setId("123");
        myObject1 = service.<DistributeEntity>persist(myObject1);

        PropertyEntity propertyEntity = new PropertyEntity();
        propertyEntity.setId("12");
        propertyEntity = service.<PropertyEntity>persist(propertyEntity);
//或者取得一个已经存在的RLO实例
        DistributeEntity myObject2 = service.<DistributeEntity, String>get(DistributeEntity.class, "123");
        PropertyEntity propertyEntity2 = service.<PropertyEntity, String>get(PropertyEntity.class, "12");
        myObject2.setPropertyEntity(propertyEntity2);
//        每次都是返回一个全新的对象引用 返回结果 false
//    产生这个现象的原因是因为Redisson没有在JVM里保存PropertyEntity对象的状态，而是在每次调用set和get的时候，
//      先将一个实例从Redis里序列化和反序列化出来，再赋值取值。
        System.out.println("get method result="+(myObject2.getPropertyEntity() == myObject2.getPropertyEntity()));
        //----------------------------------------
        //    如果想让RLO和java 中的实例保持完全一致  可以把内部的实体也定义为一个REntity
        //   内部实体同样需要持久化到redis中  并从redis中取出 使用
        //RLO对象:
        DistributeEntity myLiveObject = service.get(DistributeEntity.class, "123");
        PropertyEntity other = new PropertyEntity();
        propertyEntity2.setName("ABC");
        myLiveObject.setPropertyEntity(propertyEntity2);
        System.out.println(myLiveObject.getPropertyEntity().getName());
//输出是ABC

        propertyEntity2.setName("BCD");
        System.out.println(myLiveObject.getPropertyEntity().getName());
//还是输出ABC  当把PropertyEntity  实体也定义为一个RLO时  就会输出刚设置的字符 BCD

        myLiveObject.setPropertyEntity(propertyEntity2);
        System.out.println(myLiveObject.getPropertyEntity().getName());
//现在输出是BCD

//普通Java对象:
        DistributeEntity myLiveObject1 = new DistributeEntity();
        PropertyEntity other1 = new PropertyEntity();
        other1.setName("ABC");
        myLiveObject1.setPropertyEntity(other1);
        System.out.println(myLiveObject1.getPropertyEntity().getName());
//输出是ABC

        other1.setName("BCD");
        System.out.println(myLiveObject1.getPropertyEntity().getName());
//输出已经是BCD了

        myLiveObject1.setPropertyEntity(other1);
        System.out.println(myLiveObject1.getPropertyEntity().getName());
//输出还是BCD
    }


//    RLO类的预处理工作
    @Test
    public  void RLOPreHanderTest(){
//        提前注册RLO类
        RLiveObjectService service = client.getLiveObjectService();
        service.registerClass(DistributeEntity.class);
//        取消注册
        service.unregisterClass(DistributeEntity.class);
//        检查注册情况
        Boolean isRegistered = service.isClassRegistered(DistributeEntity.class);
    }


//    分布式执行服务  （保证服务执行的唯一性）
//    Redisson独立节点不要求任务的类在类路径里。他们会自动被Redisson独立节点（redisson node）的ClassLoader加载。
//     因此每次执行一个新任务时，不需要重启Redisson独立节点。
//    callableTask
    @Test
    public  void  callableTaskTest() throws ExecutionException, InterruptedException {
        RExecutorService executorService = client.getExecutorService("myExecutor");
        RExecutorFuture<Object> future = executorService.submit(new CallableTask());
        executorService.cancelTask(future.getTaskId());
//        取消任务
        future.cancel(true);
        Object result = future.get();
    }
//  runnable task
    @Test
    public  void  runnableTask(){
        RExecutorService executorService = client.getExecutorService("myExecutor");
        executorService.submit(new RunnableTask(123));
    }

//    分布式任务调度服务 scheduler callable task
    @Test
    public  void callableSchedulerTaskTest() throws ExecutionException, InterruptedException {
        RScheduledExecutorService executorService = client.getExecutorService("myExecutor");
        ScheduledFuture<Object> future = executorService.schedule(new CallableTask(), 10, TimeUnit.MINUTES);

        ScheduledFuture<Object> future1 = executorService.schedule(new CallableTask(), 10, TimeUnit.MINUTES);
        Object result = future.get();
    }
//     scheduler runnable task
    @Test
    public  void  runnableSchdulerTaskTest(){
        RScheduledExecutorService executorService = client.getExecutorService("myExecutor");
//        支持cron表达式
        ScheduledFuture<?> future1 = executorService.schedule(new RunnableTask(123), CronSchedule.of("10 0/5 * * * ?"));
        future1.cancel(true);
//        CronSchedule 中方法调用  == cron表达式
        ScheduledFuture<?> future4 = executorService.schedule(new RunnableTask(123), CronSchedule.dailyAtHourAndMinute(10, 5));
        ScheduledFuture<?> future2 = executorService.scheduleAtFixedRate(new RunnableTask(123), 10, 25, TimeUnit.HOURS);
        ScheduledFuture<?> future3 = executorService.scheduleWithFixedDelay(new RunnableTask(123), 5, 10, TimeUnit.HOURS);
    }



//    分布式映射归纳任务 (MapReduce)
//    处理储存在Redis环境里的大量数据的服务。所有 映射（Map） 和
//  归纳（Reduce） 阶段中的任务都是被分配到各个独立节点（Redisson Node）里并行执行的。以下所有接口均支持映射归纳
// （MapReduce）功能： RMap、 RMapCache、 RLocalCachedMap、 RSet、 RSetCache、 RList、 RSortedSet、 RScoredSortedSet、
// RQueue、 RBlockingQueue、 RDeque、 RBlockingDeque、 RPriorityQueue 和 RPriorityDeque

    /**
     * RMapper、 RCollectionMapper、 RReducer 和 RCollator接口工作原理
     * RMapper  适用于映射（Map）类，它用来把映射（Map）中的每个元素转换为另一个作为归纳（Reduce）处理用的键值对。
     * RCollectionMapper 仅适用于集合（Collection）类型的对象，它用来把集合（Collection）中的元素转换成一组作为归纳（Reduce）处理用的键值对。
     * RReducer 归纳器接口用来将上面这些，由映射器生成的键值对列表进行归纳整理。
     * RCollator 收集器接口用来把归纳整理以后的结果化简为单一一个对象。
     */
    @Test
//    处理 map 映射类型数据
    public  void  MapReduceTaskTest(){
        RMap<String, String> map = client.getMap("wordsMap");
        map.put("line1", "Alice was beginning to get very tired");
        map.put("line2", "of sitting by her sister on the bank and");
        map.put("line3", "of having nothing to do once or twice she");
        map.put("line4", "had peeped into the book her sister was reading");
        map.put("line5", "but it had no pictures or conversations in it");
        map.put("line6", "and what is the use of a book");
        map.put("line7", "thought Alice without pictures or conversation");
        RMapReduce<String, String, String, Integer> mapReduce
                = map.<String, Integer>mapReduce()
                .mapper(new WordMapper())
                .reducer(new WordReducer());
        // 统计词频
        Map<String, Integer> mapToNumber = mapReduce.execute();
        // 统计字数
        Integer totalWordsAmount = mapReduce.execute(new WordCollator());
    }
//      处理集合collection类型数据
    @Test
    public  void  MapReduceCollectionTaskTest() {
        RList<String> list = client.getList("myList");
        list.add("Alice was beginning to get very tired");
        list.add("of sitting by her sister on the bank and");
        list.add("of having nothing to do once or twice she");
        list.add("had peeped into the book her sister was reading");
        list.add("but it had no pictures or conversations in it");
        list.add("and what is the use of a book");
        list.add("thought Alice without pictures or conversation");
        RCollectionMapReduce<String, String, Integer> mapReduce
                = list.<String, Integer>mapReduce()
                .mapper(new WordCollectionMapper())
                .reducer(new WordReducer());
        // 统计词频
        Map<String, Integer> mapToNumber = mapReduce.execute();
        // 统计字数
        Integer totalWordsAmount = mapReduce.execute(new WordCollator());
    }


//   --------------------额外功能--------------------
    @Test
    public  void nodeOperationTest(){
        NodesGroup<Node> nodesGroup = client.getNodesGroup();
        nodesGroup.addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect(InetSocketAddress addr) {
                //节点连接成功
            }

            @Override
            public void onDisconnect(InetSocketAddress addr) {
                //节点断开
            }
        });
//        获取所有节点
        Collection<Node> nodes = nodesGroup.getNodes();
        for (Node node: nodes) {
//            循环ping
            node.ping();
        }
//        pingall
        nodesGroup.pingAll();
    }


//    复杂多维对象结构和对象引用支持
    @Test
    public  void complexInstructionTest(){
        RMap<RSet<RList>, RList<RMap>> complexMap = client.getMap("complex_map");
        RList<RMap> complexList = client.getList("complex_list");
        RSet<RList> complexSet = client.getSet("complex_set");
//        这里map中保存的元素发生了改变，我们不需要更新元素来使map保持最新，因为map对象所记录不是序列化后的值而是对保存
//        元素地址的引用
        complexMap.put(complexSet,complexList);
        complexSet.add(complexList);
        complexList.add(complexMap);
    }


//      命令的批量执行  (redis 管道的应用)
    @Test
    public  void batchExecutorCommandTest(){
        RBatch batch = client.createBatch();
        batch.getMap("sample_batch_map").putAsync("batch_key","batch_value1");
        batch.getMap("sample_batch_map").putAsync("batch_key","batch_value2");
        batch.getAtomicLong("counter").incrementAndGetAsync();
        batch.getAtomicLong("counter").incrementAndGetAsync();
//        原子化批量执行任务（事务）
        batch.atomic();
//        告知redis不用返回结果 (减少网络用量)
        batch.skipResult();
//        把写入操作同步到从节点
        batch.syncSlaves(2,1,TimeUnit.SECONDS);
//        处理结果超时时间
        batch.timeout(2, TimeUnit.SECONDS);
        // 命令重试等待间隔时间为2秒钟
        batch.retryInterval(2, TimeUnit.SECONDS);
// 命令重试次数，仅适用于未发送成功的命令
        batch.retryAttempts(4);
//        每个节点返回结果都会汇总到最终列表里
        BatchResult res = batch.execute();
// 或者
        Future<BatchResult<?>> asyncRes = batch.executeAsync();
    }

//    脚本执T行
    @Test
    public  void scriptExecutorTest(){
        client.getBucket("foo").set("bar");
        Object result = client.getScript().eval(RScript.Mode.READ_ONLY, "return redis.call('get', 'foo')", RScript.ReturnType.VALUE);

//        通过预存的脚本进行同样的操作  把上面的方法 分步执行
        RScript script = client.getScript();
//        first 把脚本保存到redis所有主节点
        String res = script.scriptLoad("return redis.call('get', 'foo')");
        // 返回值 res == 282297a0228f48cd3fc6a55de6316f31422f5d17
        // 再通过SHA值调用脚本
        Future<Object> r1 = client.getScript().evalShaAsync(RScript.Mode.READ_ONLY,
                "282297a0228f48cd3fc6a55de6316f31422f5d17",
                RScript.ReturnType.VALUE, Collections.emptyList());
    }

//    底层redis客户端
//    redisson 底层采用了高性能异步非阻塞java客户端  同时支持异步和同步两种通信方式
//    可以直接调用底层redis客户端来实现 redisson没有提供的命令
    @Test
    public  void  groundRedisClientTest(){
        NioEventLoopGroup group = new NioEventLoopGroup();
        RedisClientConfig config = new RedisClientConfig();
        config.setAddress("redis://localhost:6379") // 或者用rediss://使用加密连接
                .setPassword("myPassword")
                .setDatabase(0)
                .setClientName("myClient")
                .setGroup(group);
        RedisClient client = RedisClient.create(config);
        RedisConnection conn = client.connect();
// 或
        RFuture<RedisConnection> connFuture = client.connectAsync();
        conn.sync(StringCodec.INSTANCE, RedisCommands.SET, "test", 0);
// 或
        conn.async(StringCodec.INSTANCE, RedisCommands.GET, "test");
        conn.closeAsync();
// 或
        conn.closeAsync();
        client.shutdown();
// 或
        client.shutdownAsync();
    }

//-------------------------redisson node  --------------------
//     redison node 以嵌入式方法运行在其他应用中
    @Test
    public  void embeddedRunningRedissonNodeTest() throws IOException {
        Config redissonNodeConfig = new Config();
        // Redisson程序化配置代码
        Config config_json =  redissonNodeConfig.fromJSON(new File("H:\\idea workspace\\redisson-master\\src\\main\\resources\\config.json"));;
        Config config_yml =  redissonNodeConfig.fromJSON(new File("H:\\idea workspace\\redisson-master\\src\\main\\resources\\config.yml"));;
// Redisson Node 程序化配置方法
        RedissonNodeConfig nodeConfig = new RedissonNodeConfig(config_yml);
        Map<String, Integer> workers = new HashMap<String, Integer>();
        workers.put("test", 1);
        nodeConfig.setExecutorServiceWorkers(workers);
// 创建一个Redisson Node实例
        RedissonNode node = RedissonNode.create(nodeConfig);
// 或者通过指定的Redisson实例创建Redisson Node实例
        RedissonNode node_assign = RedissonNode.create(nodeConfig, client);
        node.start();
        node.shutdown();
    }







}
