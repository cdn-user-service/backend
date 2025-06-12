package io.ants.modules.app.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CreateSiteByAccessForm {

    @NotNull
    private String access_token;

    @NotNull
    private String mainServerName;

    @NotNull
    private String serialNumber;


    @JsonProperty("sProtocol")
    private String sProtocol;

}
