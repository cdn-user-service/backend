package io.ants.modules.sys.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class EditGroupClientDnsVo {
    //	XHRclient
    //4 次请求
    //已传输4.5 kB
    //3.7 kB 条资源
    //{"id":128,"line":"默认","ttl":600,"status":1}

    @NotNull
    private Integer id;

    private String line="";

    private Long ttl=600L;

    @NotNull
    private Integer status=1;
}
