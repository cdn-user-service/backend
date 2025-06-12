package io.ants.modules.sys.form;

import lombok.Data;

@Data
public class SaveTbUserInfoFrom {

     private Long userId;

     private String username;

    private String mobile;

    private String mail;

     private String password;

    private String note;

    private String unvalidModel;

    private Integer status;
}
