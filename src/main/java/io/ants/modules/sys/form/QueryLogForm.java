package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class QueryLogForm {

    @NotNull
    private Integer page=1;

    @NotNull
    private Integer  limit=20;

    private String key;

    private String start_date;

    private String end_date;

    private Long userId;

    private String username;

    private String user;

    private Integer logType=1;

    private Integer userType=1;

    private String ip;

    private List<Long> uIds;
}
