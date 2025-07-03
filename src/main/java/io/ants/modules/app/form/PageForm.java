package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(name = "分布查询Page")
public class PageForm {
    @Schema(description = "搜索条件KEY1", example = "")
    String search_key1;

    @Schema(description = "搜索条件KEY2", example = "")
    String search_key2;

    @Schema(description = "搜索条件KEY3", example = "")
    String search_key3;

    @Schema(description = "review")
    Integer review;

    @Schema(description = "domain_id", example = "1")
    @NotBlank(message = "domain_id 不能为空")
    String domain_id;

    @Schema(description = "查询页", example = "1")
    @NotNull(message = "查询页 不能为空")
    Integer pagenum;

    @Schema(description = "每页数量", example = "10")
    @NotNull(message = "每页数量 不能为空")
    Integer pagesize;

}
