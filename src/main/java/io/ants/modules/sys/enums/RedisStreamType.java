package io.ants.modules.sys.enums;

import java.util.ArrayList;
import java.util.List;

public enum RedisStreamType {
    STREAM_HEAD("xadd","public","public"),
    STREAM_NORMAL_KEY("xadd","normal","normal"),

    CONFIG("xadd",":config-stream","配置流"),
    COMMAND("xadd",":command-stream","命令流"),
    MULTI_PURGE_COMMAND("xadd",":purge-stream","purge清理"),
    CERTIFICATE("cert","certificate-stream","ssl证书申请"),
    UPDATE_COMMAND("version",":update-stream","节点版本更新"),
    FEEDBACK_STREAM("feedback",":feedback-stream","节点反馈流"),

    UP_CONFIG("","stream-config",""),
    ;
    private final String name;
    private final String remark;
    private final String group;
    RedisStreamType(String group,String name,String remark){
        this.group=group;
        this.name=name;
        this.remark=remark;
    }

    public String getName() {
        return name;
    }

    public String getRemark() {
        return remark;
    }

    public String getGroup() {
        return group;
    }

    public static List<String> getAll(){
        List<String> list=new ArrayList<>();
        for (RedisStreamType item : RedisStreamType.values()) {
            list.add(item.getName());
        }
        return list;
    }

    public static List<String> getAllXadd(){
        List<String> list=new ArrayList<>();
        for (RedisStreamType item : RedisStreamType.values()) {
            if (item.getGroup().equals("xadd")){
                list.add(item.getName());
            }
        }
        return list;
    }

}
