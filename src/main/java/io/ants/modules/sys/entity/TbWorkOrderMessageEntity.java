package io.ants.modules.sys.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_work_order_message")
public class TbWorkOrderMessageEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer workOrderId;

    private Integer senderType;

    private Long senderId;

    private String content;

    private Date createdate;
}
