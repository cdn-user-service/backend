package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class BuildAiModellingForm {

    @NotNull
    private int mode=1;


    @Pattern(regexp="^[0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}$")
    private String startTime;

    @Pattern(regexp="^[0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}:[0-9]{2}$")
    private String endTime;

    @NotNull
    private int maxCount=100000;
}
