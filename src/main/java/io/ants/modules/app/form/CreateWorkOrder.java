package io.ants.modules.app.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel(value = "创建工单")
public class CreateWorkOrder {


    private  Integer categoryId;

    private Integer urgentLevel;

    private String title;

    private String description;

    private String images;



}
