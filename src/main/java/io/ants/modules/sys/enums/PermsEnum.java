package io.ants.modules.sys.enums;


import java.util.ArrayList;
import java.util.List;

public enum PermsEnum {
    PERMS_ORDER_0("generator:order:delete","订单删除"),
    PERMS_ORDER_1("generator:order:list,generator:order:info","订单查询"),
    PERMS_ORDER_2("generator:order:save","创建订单"),
    PERMS_ORDER_3("generator:order:update","订单修改保存"),
    PERMS_ORDER_4("generator:order:admin_recharge","管理员后台充值"),
    PERMS_PRODUCT_0("generator:product:delete","产品删除"),
    PERMS_PRODUCT_1("generator:product:list,generator:product:info","产品查询"),
    PERMS_PRODUCT_2("generator:product:save","产品创建"),
    PERMS_PRODUCT_3("generator:product:update","产品修改"),
    PERMS_CONFIG_0("sys:config:list,sys:config:info","系统配置查询"),
    PERMS_CONFIG_1("sys:config:save","系统配置创建"),
    PERMS_CONFIG_2("sys:config:update","系统配置修改"),
    PERMS_CONFIG_3("sys:config:delete","系统配置删除"),
    PERMS_LOG_0("sys:log:list,sys:log:search","日志查看"),
    PERMS_LOG_1("sys:log:delete","日志查删除"),
    PERMS_MENU_0("sys:menu:delete","菜单删除"),
    PERMS_MENU_1("sys:menu:list,sys:menu:info","菜单查看"),
    PERMS_MENU_3("sys:menu:save,sys:menu:select","菜单创建"),
    PERMS_MENU_2("sys:menu:update,sys:menu:select","菜单修改"),
    PERMS_OSS_0("sys:oss:all",""),
    PERMS_ROLE_0("sys:role:delete","角色删除"),
    PERMS_ROLE_1("sys:role:list,sys:role:info","角色列表"),
    PERMS_ROLE_2("sys:role:save,sys:menu:list","角色创建"),
    PERMS_ROLE_3("sys:role:update,sys:menu:list","角色修改"),
    PERMS_SCHEDULE_0("sys:schedule:delete",""),
    PERMS_SCHEDULE_1("sys:schedule:list,sys:schedule:info",""),
    PERMS_SCHEDULE_2("sys:schedule:log",""),
    PERMS_SCHEDULE_3("sys:schedule:pause",""),
    PERMS_SCHEDULE_4("sys:schedule:resume",""),
    PERMS_SCHEDULE_5("sys:schedule:run",""),
    PERMS_SCHEDULE_6("sys:schedule:save",""),
    PERMS_SCHEDULE_7("sys:schedule:update",""),
    PERMS_SYS_USER_0("sys:user:delete","删除管理用户"),
    PERMS_SYS_USER_1("sys:user:list","查看管理信息"),
    PERMS_SYS_USER_2("sys:user:list,sys:user:info","查看管理信息"),
    PERMS_SYS_USER_3("sys:user:save,sys:role:select","新增管理信息"),
    PERMS_SYS_USER_4("sys:user:update,sys:role:select","修改管理信息"),
    PERMS_USER_0("app:user:delete","删除前台用户"),
    PERMS_USER_1("app:user:list","查看前台用户信息"),
    PERMS_USER_2("app:user:list,app:user:info","查看前台用户信息"),
    PERMS_USER_3("app:user:save,app:role:select","新增前台用户信息"),
    PERMS_USER_4("app:user:update,app:role:select","修改前台用户信息"),
    PERMS_VERSION_1("sys:version:update","版本更新"),
    PERMS_DNS_LIST("sys:dns_api:list","查看DNS-API "),
    PERMS_DNS_SAVE("sys:dns_api:save","保存DNS-API "),
    ANTS_CONF_SAVE("sys:ants_conf:save","管理ants配置"),
    REWRITE_LIST("tb:rewrite:list",""),
    REWRITE_INFO("tb:rewrite:info",""),
    REWRITE_SAVE("tb:rewrite:save",""),
    REWRITE_UPDATE("tb:rewrite:update",""),
    REWRITE_DELETE("tb:rewrite:delete",""),
    PRODUCT_LIST("sys:product:list","产品列表"),
    PRODUCT_SAVE("sys:product:save","产品增删改查"),
    ORDER_LIST("sys:order:list","订单列表"),
    ORDER_SAVE("sys:order:save","订单增删改查"),

    ;
    private final  String perms;
    private final  String name;
    PermsEnum(String perms, String name){
        this.perms=perms;
        this.name=name;
    }

    public String getPerms(){
        return perms;
    }

    public String getName(){
        return  name;
    }

    public static List<String> getAllPerms() {
        List<String> list =new ArrayList<>();
        for (PermsEnum item : PermsEnum.values()) {
            list.add(item.perms);
        }
        return list;
    }
}
