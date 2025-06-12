package io.ants.modules.sys.enums;

public enum NginxCacheType {
    FILE_SUFFIX(1,"文件后缀"),
    PATH_EQUAL(2,"精准批配"),
    PATH_LIKE(3,"模糊批配"),
    PATH_INCLUDE(4,"包含路径"),
    VISITOR(5,"游客")
    ;
    private final Integer id;

    private final String name;

    NginxCacheType(Integer id,String name){
        this.id=id;
        this.name=name;
    }

    public String getName() {
        return this.name;
    }

    public Integer getId() {
        return this.id;
    }
}
