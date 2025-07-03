package io.ants.modules.app.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateSingleCreateSiteForm {
    // {"userId":"",
    // "serialNumber":"17127275870510039",
    // "domains":["d11.cdntest.91hu.top","d12.cdntest.91hu.top"],
    // "sProtocol":"http",
    // "resourceType":"ip",
    // "serverSource":["1.1.1.1|80|1","1.14.1.1|80|1"],
    // "serverSourceBackup":["2.2.2.2|80|1","2.2.2.2|80|1"]}

    // batch
    // {"userId":"","serialNumber":"17127275870510039","sProtocol":"http","resourceType":"ip","serverSource":["aaa.ccc.com|1.1.1.1|80"]}

    private Long userId;

    @NotNull
    private List<String> domains;

    @NotNull
    private String serialNumber;

    // http http $scheme

    @NotNull
    private String sProtocol = "http";

    // ip domian
    @NotNull
    private String resourceType = "ip";

    @NotNull
    private List<String> serverSource;

    private List<String> serverSourceBackup;

    private int groupId;
}
