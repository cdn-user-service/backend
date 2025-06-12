package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class OrderCdnProductVo {
    @Data
    public class OrderCdnProductItemVo{
        private int value=0;
        private int status=0;
    }

    private OrderCdnProductItemVo m=new OrderCdnProductItemVo();
    private OrderCdnProductItemVo s=new OrderCdnProductItemVo();
    private OrderCdnProductItemVo y=new OrderCdnProductItemVo();
    private OrderCdnProductItemVo v=new OrderCdnProductItemVo();
}
