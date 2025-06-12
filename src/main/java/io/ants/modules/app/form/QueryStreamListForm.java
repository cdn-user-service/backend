package io.ants.modules.app.form;

import lombok.Data;

@Data
public class QueryStreamListForm {

    private Integer page=1;

    private Integer limit=20;

    private String user;
    private String userIds;

    //监听端口
    private Integer listenPort;
    //回源IP
    private String sourceIp;
    //回源端口
    private Integer sourcePort;
}
