package io.ants.modules.sys.enums;

public enum CdnSiteStatusEnum {
    CLOSE(0,"close_site","1,2"),
    NORMAL(1,"normal","1,2"),
    CLOSE_AND_LOCKED(2,"locked","1"),
    ;
    private int id;
    private String name;
    private String userMode;
    CdnSiteStatusEnum(int id,String name,String userMode ){
        this.id=id;
        this.name=name;
        this.userMode=userMode;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserMode() {
        return userMode;
    }
}
