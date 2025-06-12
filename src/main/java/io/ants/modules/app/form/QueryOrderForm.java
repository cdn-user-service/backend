package io.ants.modules.app.form;

import lombok.Data;

@Data
public class QueryOrderForm {

    private int page=1;

    private int limit=20;

    //private int status=10;

    private String status;

    private String orderType;

    private String serialNumber;

    private String userIds;
}
