package io.ants.modules.sys.enums;

public enum CdnConsumeAttrEnum {
    ATTR_FLOW("flow","流量使用量"),
    ;
    final String name;
    final String describe;
    CdnConsumeAttrEnum(String name, String describe){
        this.name=name;
        this.describe=describe;
    }

    public String getName() {
        return name;
    }

    public String getDescribe() {
        return describe;
    }
}
