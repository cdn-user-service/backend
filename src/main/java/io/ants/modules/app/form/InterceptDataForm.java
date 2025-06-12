package io.ants.modules.app.form;

import lombok.Data;

@Data
public class InterceptDataForm {

    //{"interceptMode":"30*","date":"*","nodeIp":"**","sourceIp":"**","serverName":"**","page":1,"limit":50}
   private String nodeIp;

   private String serverName;

   private String sourceIp;

   private String interceptMode;

   private String  date;

   private Integer page;

   private Integer limit;

   private  Integer release;
}
