package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(name = "消息")
public class MessageForm {

    private Integer id;

    /**
     * 1=站内消息;2=公告消息
     */
    @NotNull
    private Integer type;

    /**
     * 0=指定userIds发送；1=指定et用户组发【暂无】；2=指定所有 用户
     */
    @NotNull
    private Integer sendType;

    private String sendObj;

    private String title;

    private String content;

    private Integer status;

}
