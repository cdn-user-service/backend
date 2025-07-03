package io.ants.modules.app.form;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class SysCreateWafRuleForm {

    @NotNull
    private Integer siteId;

    /**
     * 每分钟请求
     */
    @Min(0)
    private Integer perMinuteReq = 1;

    /**
     * 系数0 1 2 3
     */
    @Min(0)
    @Max(10)
    private Integer coefficient = 1;

    /**
     * 封海外0|1
     */
    private Integer forbidSeal = 0;

    /**
     * 限制URL频率
     */
    @Min(0)
    private Integer limitUrlRate = 0;

    /**
     * 异常请求
     */
    @Min(0)
    private Integer badRequest = 1;

    /**
     * POST检测
     */
    @Min(0)
    private Integer postCheck = 0;

    /**
     * 人机验证
     */
    @Min(0)
    private Integer botCheck = 0;

    /**
     * 重试
     */
    @Min(0)
    private Integer isReset = 1;

    /**
     * 高频限制
     */
    @Min(0)
    private Integer highLimit = 1;

    /**
     * 热U命中
     */
    @Min(0)
    private Integer hotUrlCheck = 0;

    // 随机请求
    @Min(0)
    private Integer randomReq = 0;

    // 低频请求
    @Min(0)
    private Integer lowLimit = 0;
}
