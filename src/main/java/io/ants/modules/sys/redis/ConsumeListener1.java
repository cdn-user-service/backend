package io.ants.modules.sys.redis;

import io.ants.common.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author ：zz
 * @date ：Created in 2022/5/19 10:15
 * @description：消费监听，不自动ack
 */
@Slf4j
@Component
public class ConsumeListener1 implements StreamListener<String, MapRecord<String, String, String>> {

    @Autowired
    private RedisUtils redisUtil;

    private static ConsumeListener1 consumeListener1;

    @PostConstruct
    public void init(){
        consumeListener1 = this;
        consumeListener1.redisUtil=this.redisUtil;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        String stream = message.getStream();
        RecordId id = message.getId();
        Map<String, String> map = message.getValue();
        log.info("[不自动ack] group:[group-a] consumerName:[{}] 接收到一个消息 stream:[{}],id:[{}],value:[{}]", stream, id, map);
        consumeListener1.redisUtil.ack(stream, "group-a", id.getValue());
        consumeListener1.redisUtil.streamDel(stream, id.getValue());
    }
}
