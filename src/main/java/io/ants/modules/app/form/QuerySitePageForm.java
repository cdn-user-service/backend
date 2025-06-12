package io.ants.modules.app.form;

import lombok.Data;

@Data
public class QuerySitePageForm {

    private Integer page=1;
    private Integer limit=20;
    private String userIds="";
    private String user="";
    private String mainServerName="";
    private Integer group_id=0;
    private String areaId;
    //areaId 的别名
    private String groupId;
    private String id;

    private int simpleFlag=0;

    //监听端口
    private Integer listenPort;
    //回源IP
    private String sourceIp;
    //回源端口
    private Integer sourcePort;

    //筛选状态 "0,2"|"1"
    private String statuss;

    //筛选套餐是否有效 |0|1
    private Integer isAvailable;

}
