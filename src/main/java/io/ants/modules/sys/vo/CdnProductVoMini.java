package io.ants.modules.sys.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class CdnProductVoMini {

    private Integer id;

    private Integer productType;

    private String name;

    //private String productJson;

    //private String attrJson;

    //选用的节点组ID 组
    private String serverGroupIds;



    //private Integer status;

    //private  Integer weight;

    //private Date createtime;


    private  Object client_group_list;


    //private  Object attr;


   // private  Object sortAttr;

}
