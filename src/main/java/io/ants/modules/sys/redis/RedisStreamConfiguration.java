package io.ants.modules.sys.redis;

import io.ants.common.utils.RedisUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ：zz
 * @date ：Created in 2022/5/19 10:23
 * @description：消费组配置
 */

public class RedisStreamConfiguration {


    private static RedisUtils redisUtils;
    private static String streamKey= "";

    public  RedisStreamConfiguration(String ip, RedisUtils rds){
        streamKey=ip+":feedback-stream";
        redisUtils=rds;
    }


    public   StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer() {
        AtomicInteger index = new AtomicInteger(1);
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(processors, processors, 0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), r -> {
            Thread thread = new Thread(r);
            thread.setName("async-stream-consumer-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        // 一次最多获取多少条消息
                        .batchSize(3)
                        // 运行 Stream 的 poll task
                        .executor(executor)
                        // Stream 中没有消息时，阻塞多长时间，需要比 `spring.redis.timeout` 的时间小
                        .pollTimeout(Duration.ofSeconds(3))
                        // 获取消息的过程或获取到消息给具体的消息者处理的过程中，发生了异常的处理
                        .errorHandler(new StreamErrorHandler())
                        .build();

        RedisConnectionFactory redisConnectionFactory=redisUtils.getRedisConnectionFactory();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        // 独立消费
       // String streamKey = streamKey;

        // 消费组A,不自动ack
        // 从消费组中没有分配给消费者的消息开始消费
        streamMessageListenerContainer.receive(Consumer.from("group-a", "consumer-a"),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()), new ConsumeListener1());

        // 消费组B,自动ack
        streamMessageListenerContainer.receiveAutoAck(Consumer.from("group-b", "consumer-a"),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()), new ConsumeListener());

        return streamMessageListenerContainer;
    }

}