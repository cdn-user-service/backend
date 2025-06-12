package io.ants.modules.app.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class RequestVo {

    @NotNull
    private String url;

    @NotNull
    private String method="GET";

    private String param;

    private List<String> params;
}
