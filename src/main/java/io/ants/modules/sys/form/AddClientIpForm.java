package io.ants.modules.sys.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AddClientIpForm {

    @NotNull
    private int id;

    @NotNull
    private String clientIps;


    @NotNull
    //  //1=main;2=middle;3=backup;4=回源IP
    private int clientType;
}
