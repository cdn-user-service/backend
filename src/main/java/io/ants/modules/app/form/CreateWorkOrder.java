package io.ants.modules.app.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "创建工单")
public class CreateWorkOrder {

    private  Integer categoryId;

    private Integer urgentLevel;

    private String title;

    private String description;

    private String images;

}
