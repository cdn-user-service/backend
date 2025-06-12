package io.ants.modules.sys.vo;


import lombok.Data;

@Data
public class Ngx30xFollowVo {

    //重试次数上限
    private int proxy_next_upstream_tries=3;

    //跟随保留参数
    private int follow_args=0;

    //跟随保留请求头
    private int follow_headers=1;

    //缓存源数据
    private int cache_flag=1;
}
