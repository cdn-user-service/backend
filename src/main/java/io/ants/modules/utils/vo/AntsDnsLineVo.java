package io.ants.modules.utils.vo;

import lombok.Data;

@Data
public class AntsDnsLineVo {
    //11{
    //  "id": 1,
    //  "parent": 0,
    //  "name": "默认",
    //  "startEnd": "100000000-999999999",
    //  "level": 1,
    //  "viewOrder": 1,
    //  "status": 1,
    //  "child": 0,
    //  "linetype": 1
    //}

    private int id;
    private int parent;
    private String name;
    private String startEnd;
    private int level;
    private int viewOrder;
    private int status;
    private int child;
    private int linetype;
}
