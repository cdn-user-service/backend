package io.ants.modules.app.form;

import lombok.Data;

@Data
public class SuitListForm {

    private Integer page;

    private Integer limit;

    private String serialNumber;

    private String userIds;

    private Integer startTime;

    private Integer endTime;

    private Integer mode;

    private String key;

    private String productTypes;
}
