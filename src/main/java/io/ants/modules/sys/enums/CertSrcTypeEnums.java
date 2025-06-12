package io.ants.modules.sys.enums;

import java.util.ArrayList;
import java.util.List;

public enum CertSrcTypeEnums {
    AutoSelect("l",-1,"auto-select",true,"auto"),
    LetsencryptHttp("l",0,"Letsencrypt-http",true,"http"),
    ZeroSslHttp("l",1,"ZeroSslHttp-http",true,"http"),
    LetsencryptDns("l",2,"Letsencrypt-dns",false,"dns"),
    CertServerLetV2("r",3,"letsencrypt-http",false,"http"),
    CertServerLetDnsV2("r",4,"letsencrypt-dns",false,"dns"),
    CertServerZeroHttpV2("r",5,"zerossl-http",false,"http"),
    CertServerZeroDnsV2("r",6,"zerossl-dns",false,"dns"),
    ;
    final String group;
    final int type;
    final String name;
    final boolean needVerify;
    final String verifyType;

    CertSrcTypeEnums(String group, int type, String name,boolean needVerify,String verifyType){
        this.group=group;
        this.type=type;
        this.name=name;
        this.needVerify=needVerify;
        this.verifyType=verifyType;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public boolean isNeedVerify(){
        return needVerify;
    }

    public String getVerifyType() {
        return verifyType;
    }



    public static boolean isNeedVerifyByType(int type){
        for (CertSrcTypeEnums item:CertSrcTypeEnums.values()){
            if (item.getType()==type){
                return item.isNeedVerify();
            }
        }
        return true;
    }

    public static boolean getIsCanApply(String group,int type){
        for (CertSrcTypeEnums item:CertSrcTypeEnums.values()){
            if (item.getType()==type && item.getGroup()==group){
                return true;
            }
        }
       return false;
    }

    public static boolean getIsNeedVerifyDnsByType(int type){
        for (CertSrcTypeEnums item:CertSrcTypeEnums.values()){
            if (item.getType()==type){
                return item.getVerifyType().equalsIgnoreCase("dns");
            }
        }
        return false;
    }

    public static List<Integer> getAllDnsTypes(){
        List<Integer> list=new ArrayList<Integer>();
        for (CertSrcTypeEnums item:CertSrcTypeEnums.values()){
            if (item.getVerifyType().equals("dns")){
                list.add(item.getType());
            }
        }
        return list;

    }

    public static String getNameByType(int type){
        for (CertSrcTypeEnums item:CertSrcTypeEnums.values()){
            if (item.getType()==type){
                return item.getName();
            }
        }
        return "--";
    }
}
