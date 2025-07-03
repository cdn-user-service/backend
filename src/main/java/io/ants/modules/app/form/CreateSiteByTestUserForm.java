package io.ants.modules.app.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class CreateSiteByTestUserForm {

    @NotNull
    private String userLabel;

    @NotNull
    private String mainServerName;

    @JsonProperty("sProtocol")
    private String sProtocol;

}
