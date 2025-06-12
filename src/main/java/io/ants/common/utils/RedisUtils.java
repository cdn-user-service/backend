/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.common.utils;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 *
 * @author Mark sunlightcs@gmail.com
 */
@Component
public class RedisUtils {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ValueOperations<String, String> valueOperations;
    //@Autowired
    //private HashOperations<String, String, Object> hashOperations;
    //@Autowired
    //private ListOperations<String, Object> listOperations;
     //@Autowired
    //private SetOperations<String, Object> setOperations;
    //@Autowired
    //private ZSetOperations<String, Object> zSetOperations;

    /**  默认过期时长，单位：秒 */
    public final static long DEFAULT_EXPIRE = 60 * 60 * 24;
    /**  不设置过期时长 */
    public final static long NOT_EXPIRE = -1;
    private final static Gson GSON = new Gson();


    public RedisConnectionFactory getRedisConnectionFactory(){
        return this.redisTemplate.getConnectionFactory();
    }

    public void longSet(String key, Object value){
        valueOperations.set(key, toJson(value));
    }

    public void set(String key, Object value, long expire){
        valueOperations.set(key, toJson(value));
        if(expire != NOT_EXPIRE){
            redisTemplate.expire(key, expire, TimeUnit.SECONDS);
        }
    }

    public boolean keyExpire(String key, long expire){
       return   redisTemplate.expire(key, expire, TimeUnit.SECONDS);
    }


    public void byteSet(String key ,String filePath){
        // Create an instance of ByteArrayRedisSerializer
        RedisSerializer<byte[]> byteArrayRedisSerializer = RedisSerializer.byteArray();

        // 将字节数组存储到 Redis
        RedisTemplate<String, byte[]> redisTemplateBuf = new RedisTemplate<>();
        redisTemplateBuf.setConnectionFactory(this.getRedisConnectionFactory());
        redisTemplateBuf.setKeySerializer(new StringRedisSerializer());
        //redisTemplateBuf.setValueSerializer(new GenericToStringSerializer<>(String.class));
        redisTemplateBuf.setValueSerializer(byteArrayRedisSerializer);
        //redisTemplateBuf.setValueSerializer( new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer()); // 设置值序列化器
        redisTemplateBuf.afterPropertiesSet();
        byte[] fileBytes=FileUtils.readFileToByteArray(filePath);
        if (fileBytes.length>1){
            redisTemplateBuf.opsForValue().set(key, fileBytes);
        }
        redisTemplateBuf.getConnectionFactory().getConnection().close();

    }

    public void set(String key, Object value){
        set(key, value, DEFAULT_EXPIRE);
    }


    /**
     * 集体添加
     * @param key
     * @param value
     * @return
     */
    public Long setAdd(String key, Object value){
       return  redisTemplate.opsForSet().add(key,value);
    }

    /**
     * 集合删除
     * @param key
     * @param value
     * @return
     */
    public Long setDel(String key, Object value){
        return  redisTemplate.opsForSet().remove(key,value);
    }

    public  Set<Object> setSMembers(String key){
      return redisTemplate.opsForSet().members(key);
    }

    public int getSMembersSize(String key){
        return redisTemplate.opsForSet().members(key).size();
    }

    public boolean setDeleteMember(String key){
        boolean ret=false;
        try{
            for (Object object: redisTemplate.opsForSet().members(key)){
                 redisTemplate.delete(object.toString());
            }
            redisTemplate.delete(key);
            ret=true;
        }catch (Exception e){
            e.printStackTrace();
            ret=false;
        }
        return  ret;

    }

    public long setUnionStore(String sourceKey,String targetKey){
         Set<Object> unObj=  redisTemplate.opsForSet().union(sourceKey,targetKey);
         return redisTemplate.opsForSet().add(targetKey,unObj);
    }

    /**
     * set 集合合并
     * ram sourceKey
     * @param targetKey
     * @return
     */
    public long setUnionAndStore(String sourceKey, String targetKey){
        return redisTemplate.opsForSet().unionAndStore(targetKey,sourceKey,targetKey);
    }

    public String streamXAdd(String StreamKey, String key, String value){
        // 创建消息记录, 以及指定stream
        try{
            StringRecord stringRecord = StreamRecords.string(Collections.singletonMap(key, value)).withStreamKey(StreamKey);
            RecordId recordId = this.redisTemplate.opsForStream().add(stringRecord);

            // 限制Stream中的最大条件
            Long maxLen = 5000L; // 假设限制为5000条记录
            this.redisTemplate.opsForStream().trim(StreamKey, maxLen);

            // 是否是自动生成的
            //boolean autoGenerated =
            recordId.shouldBeAutoGenerated();
            // id值
            String r_value = recordId.getValue();
            // 序列号部分
            //long sequence = recordId.getSequence();
            // 时间戳部分
            //long timestamp = recordId.getTimestamp();
            return r_value;
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";


    }

    public Long  streamSize(String StreamKey){
        return this.redisTemplate.opsForStream().size(StreamKey);
    }

    public Integer StreamSizeV2(String StreamKey){
       List<MapRecord<String, Object, Object>>  list= redisTemplate.opsForStream().read(StreamOffset.fromStart(StreamKey));
       return list.size();
    }




    /**
     *  //从开始读
     * @param key
     * @return
     */
    public List<MapRecord<String, Object, Object>> streamRead(String key){
        List<MapRecord<String, Object, Object>> result= redisTemplate.opsForStream().read(StreamOffset.fromStart(key));
        Collections.reverse(result);
        return  result;
    }



    public List<MapRecord<String, Object, Object>> streamReadLimitSize(String key,Integer maxSize){
        List<MapRecord<String, Object, Object>> result= redisTemplate.opsForStream().read(StreamOffset.fromStart(key));
        Collections.reverse(result);
        if (result.size()>maxSize){
            List<MapRecord<String, Object, Object>> delList=result.subList(maxSize,result.size());
            List<String> delIds=new ArrayList<>();
            for (MapRecord<String, Object, Object> mapRecord:delList){
                delIds.add( mapRecord.getId().toString());
            }
            this.streamDel(key, delIds.toArray(new String[delIds.size()]));
        }
        return  result;
    }

    public Long streamDel(String key, String... recordIds){
       return redisTemplate.opsForStream().delete(key, recordIds);

    }

    public <T> T get(String key, Class<T> clazz, long expire) {
        String value = valueOperations.get(key);
        if(expire != NOT_EXPIRE){
            redisTemplate.expire(key, expire, TimeUnit.SECONDS);
        }
        return value == null ? null : fromJson(value, clazz);
    }

    public <T> T get(String key, Class<T> clazz) {
        return get(key, clazz, NOT_EXPIRE);
    }

    public String get(String key, long expire) {
        String value = valueOperations.get(key);
        if(expire != NOT_EXPIRE){
            redisTemplate.expire(key, expire, TimeUnit.SECONDS);
        }
        return value;
    }

    public void hashSetPushMap(String key,Map<String,String>map){
        redisTemplate.opsForHash().putAll(key,map);
    }

    public String hashGet(String key,String field){
        if(null==redisTemplate.opsForHash().get(key,field)){
            return null;
        }
        return  redisTemplate.opsForHash().get(key,field).toString();
    }

    public void hashDel(String key,String field){
        redisTemplate.opsForHash().delete(key,field);
    }

    public Set<String> getKeys(String keys){
       return redisTemplate.keys(keys);
    }

    public String get(String key) {
        if (StringUtils.isBlank(key)){
            return null;
        }
        return get(key, NOT_EXPIRE);
    }

    public boolean delete(String key) {
       return  redisTemplate.delete(key);
    }



    /**
     * Object转成JSON数据
     */
    private String toJson(Object object){
        if(object instanceof Integer || object instanceof Long || object instanceof Float ||
                object instanceof Double || object instanceof Boolean || object instanceof String){
            return String.valueOf(object);
        }
        return GSON.toJson(object);
    }

    /**
     * JSON数据，转成Object
     */
    private <T> T fromJson(String json, Class<T> clazz){
        return GSON.fromJson(json, clazz);
    }


    /**
     * scan 实现
     *
     * @param pattern       表达式，如：abc*，找出所有以abc开始的键
     */
    public  Set<String> scanAll(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keysTmp = new HashSet<>();
            try ( Cursor<byte[]> cursor = connection.scan(new ScanOptions.ScanOptionsBuilder() .match(pattern) .count(100).build())) {
                while (cursor.hasNext()) {
                    keysTmp.add(new String(cursor.next(), "Utf-8"));
                }
                return  keysTmp;
            } catch (Exception e) {
                System.out.println("ERROR--scanAll-ERROR");
                e.printStackTrace();
            }
            return null;
        });
    }


    public void scanAllDelete(String pattern){
        Set<String> sets=scanAll(pattern);
        if (null==sets){
            return;
        }
        for(String key:sets){
            delete(key);
        }
    }


    public Map findKeysForPage(String patternKey, int pageNum, int pageSize) {
        Map resultMap=new HashMap();
        ScanOptions options = ScanOptions.scanOptions().match(patternKey).count(pageSize*100).build();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection rc = factory.getConnection();
        Cursor<byte[]> cursor = rc.scan(options);
        List<String> result = new ArrayList<String>(pageSize);
        int tmpIndex = 0;
        int startIndex = (pageNum - 1) * pageSize;  // 开始节点
        int end = pageNum * pageSize;   // 去redis中找的次数,结束节点
        while (cursor.hasNext()) {
            if (tmpIndex >= startIndex && tmpIndex < end) {
                byte[] key = cursor.next();
                if (key == null || key.length == 0) {
                    break;
                }
                String keyStr = new String(key);
                result.add(keyStr);
                tmpIndex++;
                continue;
            }
            // 要获取总条数, 就注释掉
            //            if (tmpIndex >= end) {
            //                //break;
            //            }
            tmpIndex++;
            cursor.next();
        }
        try {
            cursor.close();
        } catch (Exception e) {
            System.out.println("ERROR----findKeysForPage1----ERROR");
            e.printStackTrace();
        }

        try {
            RedisConnectionUtils.releaseConnection(rc, factory,false);
        } catch (Exception e) {
            System.out.println("ERROR----findKeysForPage2----ERROR");
            e.printStackTrace();

        }
        resultMap.put("total",tmpIndex);
        resultMap.put("data",result);
        return resultMap;
    }


    /**
     * create by: zz
     * description: 创建消费组
     * create time: 2022/5/11 16:45
     * @param:
     * @return java.lang.String
     */
    public String createGroup(String key, String group){
        return redisTemplate.opsForStream().createGroup(key, group);
    }

    /**
     * create by: zz
     * description: 添加Map消息
     * create time: 2022/5/11 16:28
     * @param: key
     * @param: value
     * @return
     */
    public String addMap(String key, Map<String, String> value){
        return redisTemplate.opsForStream().add(key, value).getValue();
    }

    /**
     * create by: zz
     * description: 添加Record消息
     * create time: 2022/5/11 16:30
     * @param: record
     * @return
     */
    public String addRecord(Record<String, Object> record){
        return redisTemplate.opsForStream().add(record).getValue();
    }

    /**
     * create by: zz
     * description: 确认消费
     * create time: 2022/5/19 11:21
     * @param: key
     * @param: group
     * @param: recordIds
     * @return java.lang.Long
     */
    public Long ack(String key, String group, String... recordIds){
        return redisTemplate.opsForStream().acknowledge(key, group, recordIds);
    }




}
