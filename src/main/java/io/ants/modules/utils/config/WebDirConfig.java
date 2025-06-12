package io.ants.modules.utils.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class WebDirConfig  implements Serializable {
    private static final long serialVersionUID = 1L;

    private String adminDir;

    private String userDir;

    private String frontAddress;
}
