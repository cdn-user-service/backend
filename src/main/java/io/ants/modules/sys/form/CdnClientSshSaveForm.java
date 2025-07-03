package io.ants.modules.sys.form;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class CdnClientSshSaveForm {
    @NotNull
    private Integer id;

    private Integer sshPort;
    private String sshUser;
    private String sshPwd;
}
