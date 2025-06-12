package io.ants.common.utils;


import com.gliwka.hyperscan.wrapper.Scanner;
import com.gliwka.hyperscan.wrapper.*;

import io.ants.modules.app.form.SysCreateWafRuleForm;
import io.ants.modules.app.vo.SysWafRuleConfVo;
import io.ants.modules.sys.enums.WafOpEnum;
import io.ants.modules.sys.vo.SysWafRuleVo;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * function 正则工具类
 * auth zhanghu 2022 07 24
 * @author Administrator
 */
public class RegExUtils {

    public enum RegexOpTypeEnum {
          E(0,0,1,"e","字符串等于"),
         UE(0,1,2,"ue","字符串不等于"),
          I(0,0,3,"i","字符串包含"),
         UI(0,1,4,"ui","字符串不包含"),
        LGE(0,0,5,"lge","字符串长度长于"),
         LE(0,0,6,"le","字符串长度等于"),
        LLE(0,0,7,"lle","字符串长度小于"),
       NULL(0,0,8,"null","为空"),
        IGE(0,0,9,"ige","数字（字符串形式）大于等于"),
         IE(0,0,10,"ie","数字（字符串形式）等于"),
        ILE(0,0,11,"ile","数字（字符串形式）小于等于"),
        BLE(0,0,12,"ble","种类数小于（2-32）"),
        BEQ(0,0,13,"beq","种类数等于（0-32"),
        BGE(0,0,14,"bge","种类数大于（0-32"),
        ;
        private Integer groupId;
        private Integer mode;
        private Integer id;
        private String key;
        private String cnName;
        RegexOpTypeEnum(Integer groupId, Integer mode,Integer id, String key, String cnName){
            this.groupId=groupId;
            this.mode=mode;
            this.id=id;
            this.key=key;
            this.cnName=cnName;
        }


        public Integer getGroupId() {
            return groupId;
        }

        public Integer getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public String getCnName() {
            return cnName;
        }

        public Integer getMode() {
            return mode;
        }

        public static String getCnNameByKey(String key){
            for (RegexOpTypeEnum item: RegexOpTypeEnum.values()){
                if (item.getKey().equals(key)){
                    return item.getCnName();
                }
            }
            return "";
        }

        public static String getModeByHandleKey(String key){
            for (RegexOpTypeEnum item: RegexOpTypeEnum.values()){
                if (item.getKey().equals(key)){
                    return item.getMode().toString();
                }
            }
            return "";
        }

        public static List<String> getAllKey(){
            List<String> ls=new ArrayList<>(RegexOpTypeEnum.values().length);
            for (RegexOpTypeEnum item: RegexOpTypeEnum.values()){
                ls.add(item.getKey());
            }
            return ls;
        }
    }

    // ANSI转义码 - 文本样式
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    //private static Logger logger = LoggerFactory.getLogger(RegExUtils.class);
    /**
     * 00:02:00-03:03:03==>时间范围正则表示
     * @return (00|01|02):(00|01):(00|01|03))
     */
    private static String getTimeRangeRex(String timeRangeStr){
        String rt="";
        if (timeRangeStr.contains("-")){
            String[] r=timeRangeStr.split("-");
            if(r.length>=2){
                String[] s0=r[0].split(":");
                String[] s1=r[1].split(":");
                if(3==s0.length && 3==s1.length){
                    // 小时相同
                    if(Integer.parseInt(s0[0])==Integer.parseInt(s1[0])){
                        String f0="%s:[%s-%s]\\d:\\d{2}";
                        String m00=s0[1].substring(0,1);
                        String m10=s1[1].substring(0,1);
                        rt=String.format(f0,s0[0],m00,m10);
                    }else {
                        // 小时不同 ：分三段 开始小时 中间小时  结束小时
                        String t0="",t1="",t2="";
                        if(true){
                            //开始相同时 正则
                            String f0="%s:[%s-6]\\d:\\d{2}";
                            String m00=s0[1].substring(0,1);
                            t0=String.format(f0,s0[0],m00);
                        }

                        if(Integer.parseInt(s1[0])-Integer.parseInt(s0[0])>1 ){
                            //中间相差的小时
                            String f1="(%s):\\d{2}:\\d{2}";
                            String h0=s0[0];
                            String h1=s1[0];
                            List<String> list=new ArrayList<>();
                            for (int i = Integer.parseInt(h0)+1; i <Integer.parseInt(h1) ; i++) {
                                String th=String.format("%02d",i);
                                if(!list.contains(th)){
                                    list.add(th);
                                }
                            }
                            String h_list=String.join("|",list);
                            t1=String.format(f1,h_list);
                        }

                        if (true){
                            //结束小时
                            String f2="%s:[0-%s]\\d:\\d{2}";
                            String m10=s1[1].substring(0,1);
                            t2=String.format(f2,s1[0],m10);
                        }
                        rt=String.format("(%s)|(%s)|(%s)",t0,t1,t2);
                    }

                }
            }
        }
        return  rt;
    }

    private static  String getAZaz09Reg(List<String> bi){
        int j=0;
        String buf="";
        char start_char=0;
        do{
            if(j+1<bi.size()-1){
                char j0=bi.get(j).charAt(0);
                char j1=bi.get(j+1).charAt(0);
                if(0==start_char){
                    buf+= j0 +"-";
                }
                //System.out.println(j1-j0);
                if(1==j1-j0){
                    start_char=j0;
                }else {
                    //System.out.println("xx");
                    buf+=j0;
                    start_char=0;
                }
            }else {
                break;
            }
            j++;
        }while (true);
        buf+=bi.get(bi.size()-1);
        //System.out.println(buf);
        if ("A-Za-z0-9".equals(buf)){
            return "^&";
        }
        return buf;
    }

    public static String p_char(Integer i){
        StringBuilder s= new StringBuilder("_");
        for (int j = 0; j <i ; j++) {
            s.append("_");
        }
        return s.toString();
    }

    private static String getNZero(Integer n){
        StringBuilder sb=new StringBuilder();
        while (n>0){
            sb.append("0");
            n--;
        }
        return sb.toString();
    }

    private static String getUnEqualRegx(String content){
        final String[] m={"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z","a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","0","1","2","3","4","5","6","7","8","9"};
        String[] contentListArray=content.split("\\|");
        int maxLen=0;
        List<String> reg=new ArrayList<>();
        for (String content_i:contentListArray){
            if (content_i.length()>maxLen){
                maxLen=content_i.length();
            }
        }
        for (int i = 0; i <maxLen+1 ; i++) {
            if(0==i){
                //第一个不为首，则第2--n 可以任意
                Map<String,Object> firstMap=new HashMap();
                List<String> base_char_ls=new ArrayList<>()  ;
                base_char_ls.addAll(Arrays.asList(m));
                for (String content_i:contentListArray){
                    String is=content_i.substring(0,1);
                    base_char_ls.remove(is);
                    firstMap.put(is,null);

                }
                String calChar=getAZaz09Reg(base_char_ls);
                if (StringUtils.isNotBlank(calChar) && !calChar.equals("^&")){
                    reg.add("(["+calChar+"][^&]*)");
                }
                //包含自身直接结束且不在content组中
                for (String mKey:firstMap.keySet()){
                    if(!Arrays.asList(contentListArray).contains(mKey)){
                        reg.add("|("+mKey+")");
                    }
                }
            }else{
                //前 I 个字符 +非第（I+1）个字符
                List<String >content_0_i_list =new ArrayList<>();//取前I   个字符
                List<String >content_0_j_list =new ArrayList<>();//取前I+1 个字符
                Map<String,Object> midIMap=new HashMap(); //自身
                for (String content_i:contentListArray){
                    if(content_i.length()>=(i+1)){
                        String content_0_i=content_i.substring(0,i);
                        if(!content_0_i_list.contains(content_0_i)){
                            content_0_i_list.add(content_0_i);
                        }
                        String content_0_j=content_i.substring(0,i+1);
                        if(!content_0_j_list.contains(content_0_j)){
                            content_0_j_list.add(content_0_j);
                        }
                        midIMap.put(content_0_i,null);
                    }else{
                        //System.out.println(mi+" ->"+i);
                        if(content_i.length()<=i){
                            if(!content_0_i_list.contains(content_i)){
                                content_0_i_list.add(content_i);
                            }
                            String is_ik=content_i+p_char(i-content_i.length());
                            if(!content_0_j_list.contains(is_ik)){
                                content_0_j_list.add(is_ik);
                            }
                        }
                    }
                }
                //System.out.println(ijs);
                //System.out.println(iks);
                //固定前I个字符串的ij
                for (String content_0_i:content_0_i_list){
                    List<String> bi=new ArrayList<>()  ;
                    bi.addAll(Arrays.asList(m));
                    boolean in_next=false;
                    for (String mk:content_0_j_list){
                        if(mk.contains(content_0_i)){
                            String is=mk.substring(i,i+1);
                            bi.remove(is);
                            in_next=true;
                        }
                    }
                    String curl_reg="";
                    if(content_0_i.length()<i){
                        if(!in_next){
                            String mid_="[^&]*";
                            if (StringUtils.isNotBlank(getAZaz09Reg(bi))){
                                curl_reg="|("+content_0_i+mid_+"["+getAZaz09Reg(bi)+"][^&]*)";
                            }else {
                                curl_reg="|("+content_0_i+mid_+"[^&]*)";
                            }

                        }
                    }else {
                        if (StringUtils.isNotBlank(getAZaz09Reg(bi))){
                            curl_reg="|("+content_0_i+"["+getAZaz09Reg(bi)+"][^&]*)";
                        }else{
                            curl_reg="|("+content_0_i+"[^&]*)";
                        }
                        //包含自身直接结束且不在content组中
                        for (String  mKey:midIMap.keySet()){
                            if(!Arrays.asList(contentListArray).contains(mKey)){
                                reg.add("|("+mKey+")");
                            }
                        }
                    }
                    reg.add(curl_reg);
                }
            }

        }
        reg.add("|[^&]{"+(maxLen+1)+",256}");
        //对正则去重
        List newRegList = reg.stream().distinct().collect(Collectors.toList());
        return String.join("",newRegList);
    }

    public static  String createJavaPatternRegStr(String searchKey,String handle,String content){
        String line_rule="";
        final String[] time_range_key={"b1"};
        try{
            switch (handle){
                case "e":
                    // str 等于
                    line_rule+=searchKey+"=("+content+")&.*";
                    break;
                case "ue":
                    // str 不等于
                    //line_rule+=sk+"=(?!"+content+")&.*";
                    if (true){
                        line_rule+=searchKey+"=("+ getUnEqualRegx(content)+")&.*";
                    }
                    break;
                case "i":
                    // str 包含
                    if(Arrays.asList(time_range_key).contains(searchKey)){
                        //00:00:00-12:00:00
                        String r_regx= getTimeRangeRex(content);
                        if(StringUtils.isNotBlank(r_regx)){
                            line_rule+=searchKey+"=[^&]*"+r_regx+"[^&]*&.*";
                        }
                    }else {
                        line_rule+=searchKey+"=[^&]*("+content+")[^&]*&.*";
                    }
                    break;
                case "ui":
                    // str 不包含
                    if (true){
                        line_rule+=searchKey+"=("+ getUnEqualRegx(content)+")&.*";
                    }
                    break;
                case "lge":
                    // 长度长于
                    if (content.matches("-?\\d+"))
                    {
                        System.out.println("lge--"+content);
                        line_rule+=searchKey+"=[^&]{"+content+",128}&"+".*";
                    }
                    break;
                case "le":
                    // 长度等于
                    if (content.matches("-?\\d+"))
                    {
                        line_rule+=searchKey+"=[^&]{"+content+"}&"+".*";
                    }
                    break;
                case "lle":
                    // 长度小于
                    if (content.matches("-?\\d+"))
                    {
                        line_rule+=searchKey+"=[^&]{1,"+content+"}&"+".*";
                    }
                    break;
                case "null":
                    //为空
                    line_rule+=searchKey+"=&"+".*";
                    break;
                case "ie":
                    //数值等于
                    if (content.matches("-?\\d+"))
                    {
                        line_rule+=searchKey+"="+content+"&.*";
                    }
                    break;
                case "ile":
                    //数值小于
                    if(content.matches("-?\\d+")){
                        if(1==content.length()){
                            String reg="[0-"+content+"]";
                            line_rule+=searchKey+"="+reg+"&.*";
                        }else{
                            //多位数值生成
                            //22
                            //1小于N-1位 \d{1,1}
                            //2.1 等于N位  [1] \d{1,1}
                            //2.2         [2] [2][0-2]
                            List<String> regList=new ArrayList<>();
                            String reg0=String.format("([0-9]\\d{0,%d})",content.length()-2);// n-2 位是任意数
                            regList.add(reg0);
                            for (int i = 0; i <content.length() ; i++) {
                                String content_0_i=content.substring(0,i+1);
                                //char[] content_0_i_char_array=content_0_i.toCharArray();
                                int v_i=Integer.parseInt(content.substring(i,i+1)) ;//第i位的数值
                                // String c_regx=String.valueOf(content_0_i_char_array);
                                //System.out.println(c_regx);
                                if (1==content_0_i.length()){
                                    if (v_i>1){
                                        regList.add(String.format("[1-%d]\\d{1,%d}",v_i-1,content.length()-1));
                                    }
                                }else if (content_0_i.length()>1){
                                    Integer last_x_sum=content.length()-i-1;
                                    String last_0_i= content_0_i.substring(0,content_0_i.length()-1);
                                    String last_v="";
                                    if (v_i-1>0){
                                        last_v="[0-"+(v_i-1)+"]";
                                    }else if(0==v_i-1){
                                        last_v="0";
                                    }else {
                                       //conti
                                    }
                                    String reg=last_0_i+last_v;
                                    if (last_x_sum>0){
                                        reg+="\\d{"+last_x_sum+"}";
                                    }
                                    regList.add(reg);
                                }
                            }
                            List newRegList = regList.stream().distinct().collect(Collectors.toList());
                            StringBuilder fReg= new StringBuilder();
                            for (int i = 0; i <newRegList.size() ; i++) {
                                fReg.append(newRegList.get(i));
                                if(i!=newRegList.size() -1){
                                    fReg.append("|");
                                }
                            }
                            line_rule+=searchKey+"=("+fReg+")&.*";
                        }
                    }
                    break;
                case "ige":
                    //数值大于
                    if(content.matches("-?\\d+")){
                        List<String> regList=new ArrayList<>();
                        String reg0=String.format("(\\d{%d,32})",content.length()+1);
                        regList.add(reg0);
                        for (int i = 0; i <content.length() ; i++) {
                            String content_0_i=content.substring(0,i+1);
                            //char[] content_0_i_char_array=content_0_i.toCharArray();
                            Integer v_i=Integer.parseInt(content.substring(i,i+1)) ;
                            // String c_regx=String.valueOf(content_0_i_char_array);
                            //System.out.println(c_regx);
                            if (content_0_i.length()>0){
                                int lastXSum=content.length()-i-1;
                                String last_0_i= content_0_i.substring(0,content_0_i.length()-1);
                                String last_v="";
                                if ((v_i+1)<9){
                                    last_v="["+(v_i+1)+"-9]";
                                }else if(9==(v_i+1)){
                                    last_v="9";
                                }else{
                                    last_v="9";
                                }
                                String reg=last_0_i+last_v;
                                if (lastXSum>0){
                                    reg+="\\d{"+lastXSum+"}";
                                }
                                regList.add(reg);
                            }
                        }

                        List newRegList = regList.stream().distinct().collect(Collectors.toList());
                        StringBuilder fReg= new StringBuilder();
                        for (int i = 0; i <newRegList.size() ; i++) {
                            fReg.append(newRegList.get(i));
                            if(i!=newRegList.size() -1){
                                fReg.append("|");
                            }
                        }
                        line_rule+=searchKey+"=("+fReg+")&.*";
                    }
                    break;
                case "ble":
                    if(content.matches("-?\\d+")){
                        Integer v=Integer.parseInt(content);
                        if (v>1 && v<33){
                            line_rule+=searchKey+"="+"0*(0*10*){0,"+(v-1)+"}0*"+"&.*";
                        }
                    }
                    break;
                case "beq":
                    if(content.matches("-?\\d+")){
                        if (0==Integer.parseInt(content)){
                            line_rule+=searchKey+"="+"0{32}"+"&.*";
                        }else{
                            line_rule+=searchKey+"="+"(0*10*){"+content+"}"+"&.*";
                        }
                    }
                    break;
                case "bge":
                    if(content.matches("-?\\d+")){
                        Integer v=Integer.parseInt(content);
                        if (v>=0 && v<33){
                            line_rule+=searchKey+"="+"(0*10*){"+(v+1)+",32}"+"&.*";
                        }
                    }
                    break;
                default:
                    //logger.error("handle["+handle+"]未知的操作类型！");
                    System.out.println("handle["+handle+"]未知的操作类型！");
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  line_rule;
    }


    /**
     * waf 3.0 生成正则
     * @param searchKey
     * @param handle
     * @param content
     * @return
     */
    public static String createJavaPatternRegStrV300(String searchKey,String handle,String content){
        String line_rule="";
        final String[] time_range_key={"b1"};
        try{
            switch (handle){
                case "ue":
                case "e":
                    // str 等于
                    line_rule+=searchKey+"=("+content+")&.*";
                    break;
                case "ui":
                case "i":
                    // str 包含
                    if(Arrays.asList(time_range_key).contains(searchKey)){
                        //00:00:00-12:00:00
                        String r_regx= getTimeRangeRex(content);
                        if(StringUtils.isNotBlank(r_regx)){
                            line_rule+=searchKey+"=[^&]*"+r_regx+"[^&]*&.*";
                        }
                    }else {
                        line_rule+=searchKey+"=[^&]*("+content+")[^&]*&.*";
                    }
                    break;
                case "lge":
                    // 长度长于
                    if (content.matches("-?\\d+"))
                    {
                        System.out.println("lge--"+content);
                        line_rule+=searchKey+"=[^&]{"+content+",128}&"+".*";
                    }
                    break;
                case "le":
                    // 长度等于
                    if (content.matches("-?\\d+"))
                    {
                        line_rule+=searchKey+"=[^&]{"+content+"}&"+".*";
                    }
                    break;
                case "lle":
                    // 长度小于
                    if (content.matches("-?\\d+"))
                    {
                        line_rule+=searchKey+"=[^&]{1,"+content+"}&"+".*";
                    }
                    break;
                case "null":
                    //为空
                    line_rule+=searchKey+"=&"+".*";
                    break;
                case "ie":
                    //数值等于
                    if (content.matches("-?\\d+"))
                    {
                        line_rule+=searchKey+"="+content+"&.*";
                    }
                    break;
                case "ile":
                    //数值小于
                    if(content.matches("-?\\d+")){
                        if(1==content.length()){
                            String reg="[0-"+content+"]";
                            line_rule+=searchKey+"="+reg+"&.*";
                        }else{
                            //多位数值生成
                            //22
                            //1小于N-1位 \d{1,1}
                            //2.1 等于N位  [1] \d{1,1}
                            //2.2         [2] [2][0-2]
                            List<String> regList=new ArrayList<>();
                            String reg0=String.format("([0-9]\\d{0,%d})",content.length()-2);// n-2 位是任意数
                            regList.add(reg0);
                            for (int i = 0; i <content.length() ; i++) {
                                String content_0_i=content.substring(0,i+1);
                                //char[] content_0_i_char_array=content_0_i.toCharArray();
                                int v_i=Integer.parseInt(content.substring(i,i+1)) ;//第i位的数值
                                // String c_regx=String.valueOf(content_0_i_char_array);
                                //System.out.println(c_regx);
                                if (1==content_0_i.length()){
                                    if (v_i>1){
                                        regList.add(String.format("[1-%d]\\d{1,%d}",v_i-1,content.length()-1));
                                    }
                                }else if (content_0_i.length()>1){
                                    Integer last_x_sum=content.length()-i-1;
                                    String last_0_i= content_0_i.substring(0,content_0_i.length()-1);
                                    String last_v="";
                                    if (v_i-1>0){
                                        last_v="[0-"+(v_i-1)+"]";
                                    }else if(0==v_i-1){
                                        last_v="0";
                                    }else {
                                        //conti
                                    }
                                    String reg=last_0_i+last_v;
                                    if (last_x_sum>0){
                                        reg+="\\d{"+last_x_sum+"}";
                                    }
                                    regList.add(reg);
                                }
                            }
                            List newRegList = regList.stream().distinct().collect(Collectors.toList());
                            StringBuilder fReg= new StringBuilder();
                            for (int i = 0; i <newRegList.size() ; i++) {
                                String tReg=newRegList.get(i).toString();
                                tReg=tReg.replace("\\d{0,0}","");
                                fReg.append(tReg);
                                if(i!=newRegList.size() -1){
                                    fReg.append("|");
                                }
                            }
                            line_rule+=searchKey+"=("+fReg+")&.*";
                        }
                    }
                    break;
                case "ige":
                    //数值大于
                    if(content.matches("-?\\d+")){
                        List<String> regList=new ArrayList<>();
                        String reg0=String.format("(\\d{%d,32})",content.length()+1);
                        regList.add(reg0);
                        for (int i = 0; i <content.length() ; i++) {
                            String content_0_i=content.substring(0,i+1);
                            //char[] content_0_i_char_array=content_0_i.toCharArray();
                            Integer v_i=Integer.parseInt(content.substring(i,i+1)) ;
                            // String c_regx=String.valueOf(content_0_i_char_array);
                            //System.out.println(c_regx);
                            if (content_0_i.length()>0){
                                int lastXSum=content.length()-i-1;
                                String last_0_i= content_0_i.substring(0,content_0_i.length()-1);
                                String last_v="";
                                if ((v_i+1)<9){
                                    last_v="["+(v_i+1)+"-9]";
                                }else if(9==(v_i+1)){
                                    last_v="9";
                                }else{
                                    last_v="9";
                                }
                                String reg=last_0_i+last_v;
                                if (lastXSum>0){
                                    reg+="\\d{"+lastXSum+"}";
                                }
                                regList.add(reg);
                            }
                        }

                        List newRegList = regList.stream().distinct().collect(Collectors.toList());
                        StringBuilder fReg= new StringBuilder();
                        for (int i = 0; i <newRegList.size() ; i++) {
                            String tReg=newRegList.get(i).toString();
                            tReg=tReg.replace("\\d{0,0}","");
                            fReg.append(tReg);
                            if(i!=newRegList.size() -1){
                                fReg.append("|");
                            }
                        }
                        line_rule+=searchKey+"=("+fReg+")&.*";
                    }
                    break;
                case "ble":
                    if(content.matches("-?\\d+")){
                        Integer v=Integer.parseInt(content);
                        if (v>0 && v<33){
                            line_rule+=searchKey+"="+"0*(0*10*){0,"+(v-1)+"}0*"+"&.*";
                        }
                    }
                    break;
                case "beq":
                    if(content.matches("-?\\d+")){
                        if (0==Integer.parseInt(content)){
                            line_rule+=searchKey+"="+"0{32}"+"&.*";
                        }else{
                            line_rule+=searchKey+"="+"(0*10*){"+content+"}"+"&.*";
                        }
                    }
                    break;
                case "bge":
                    if(content.matches("-?\\d+")){
                        Integer v=Integer.parseInt(content);
                        if (v>=0 && v<33){
                            line_rule+=searchKey+"="+"(0*10*){"+(v+1)+",32}"+"&.*";
                        }
                    }
                    break;
                default:
                    //logger.error("handle["+handle+"]未知的操作类型！");
                    System.out.println("handle["+handle+"]未知的操作类型！");
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return  line_rule;
    }

    private void uitest(){
          /*
                    //&cookies=[^&]*(v|v|v)(?!t_4b1|t_4b|t_4b3)[^&]*
//                    if(false){
//                        //?! 支持下的正则
//                        if(Arrays.asList(time_range_key).contains(sk)){
//                            ///b1=2022-07-04T16:36:41+08:00&
//                            String r_regx=get_time_range_rex(content);
//                            if(StringUtils.isNotBlank(r_regx)){
//                                line_rule+=sk+"=([^&]*T(?!"+r_regx+")[^&]*)&.*";
//                            }
//                        }else{
//                            if(content.length()>1){
//                                String[] ps=content.split("\\|");
//                                if(1==ps.length  ){
//                                    line_rule+=sk+"=[^&]*"+content.substring(0,1)+"(?!"+content.substring(1)+")[^&]*&.*";
//                                }else if(ps.length>=2){
//                                    List<String> h_list=new ArrayList<>();
//                                    List<String> t_list=new ArrayList<>();
//                                    for (String p:ps){
//                                        String h=p.substring(0,1);
//                                        String t=p.substring(1);
//                                        if(!h_list.contains(h)){
//                                            h_list.add(h);
//                                        }
//                                        if(!t_list.contains(t)){
//                                            t_list.add(t);
//                                        }
//                                    }
//                                    line_rule+=sk+"=([^&]*("+String.join("|",h_list)+")(?!"+String.join("|",t_list)+")[^&]*)&.*";
//                                }
//                            }
//                        }
//                    }

                     */
    }


    /**
     * 构造Hyperscan Db(弃用)
     *
     */
    public static  void  buildHyperscanDb(){
        try{
            Long startT=System.currentTimeMillis();
            LinkedList<Expression> expressions = new LinkedList<Expression>();
            //the first argument in the constructor is the regular pattern, the latter one is a expression flag
            //make sure you read the original hyperscan documentation to learn more about flags
            //or browse the ExpressionFlag.java in this repo.
            expressions.add(new Expression("k36=((\\d{2,32})|[6-9])&.*", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("k28=((\\d{2,32})|[4-9])&.*k38=10000&.*", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{5}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("[a-z1A-Z]{2-8}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{6}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{16}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{6123}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{64}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{1246}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{1236}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{613}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{61}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{116}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{63}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            expressions.add(new Expression("\\d{622}", EnumSet.of(ExpressionFlag.SOM_LEFTMOST)));
            System.out.println(String.format("reg_count=%d",expressions.size()));
            Database db = Database.compile(expressions);
            OutputStream out = new FileOutputStream("db");
            db.save(out);
            //使用缓冲区的方法将数据读入到缓冲区中
            File file = new File("db");
            Integer file_size=Integer.parseInt(String.valueOf(file.length())) ;
            Integer len=Integer.valueOf(String.valueOf(db.getSize()));
            byte[] result2=new byte[len];
            FileInputStream fis=new FileInputStream("db");
            BufferedInputStream bis=new BufferedInputStream(fis);
            bis.skip(file_size-len);
            bis.read(result2, 0, len);
            OutputStream fos = new FileOutputStream("db_");
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(result2);
            bos.flush();
            bos.close();
            fis.close();
            System.out.println(String.format("用时：%d 毫秒",System.currentTimeMillis()-startT) );
            System.out.println(String.format("DB size=%s",db.getSize()));
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 加载 Hyperscan Db
     */
    public static void hyperscanLoadDb(){
        try{
           InputStream in = new FileInputStream("db");
           Database loadedDb = Database.load(in);
           // Use the loadedDb as before.
           Scanner scanner = new Scanner();
           scanner.allocScratch(loadedDb);
           List<Match> matches = scanner.scan(loadedDb, "12345");
           for (Match m:matches){
               System.out.println(m.getMatchedString());
               System.out.println(m.getStartPosition());
           }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static int isPowerOfTwo(int n) {
        if (n < 1) {
            return 0;
        }
        else if ((n & (n-1)) == 0) {
            return 1;
        }
        else {
            return 0;
        }
    }

    private static ByteBuffer bfTest(){
        ByteBuffer bb=ByteBuffer.allocate(32);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(403);
        bb.putInt(301);
        bb.putInt(100);
        bb.putInt(200);
        bb.putInt(302);
        bb.putInt(504);
        return  bb;
    }



    private static void test0(String handle,String content ){
        //e ue i ui  lge le lle null ie ile ige
        String key="na";
        //String handle="ile";
        //String content="22";
        System.out.println(content);
        System.out.println("handle="+handle);
        String reg=createJavaPatternRegStr(key,handle,content);
        System.out.println("f->"+reg);
        Pattern r = Pattern.compile(reg);
        long cv=Long.parseLong(content);
        switch (handle){
            case "ile":
                if(true){
                    long i=1l;
                    for ( i = 1L; i <cv ; i++) {
                        long new_cv=cv-i;
                        String str=key+"="+new_cv+"&";
                        Matcher m = r.matcher(str);
                        //System.out.println(str);
                        if (!m.matches()){
                            System.out.println("error:-->"+str);
                        }

                    }
                }
                break;
            case "ige":
                if (true){
                    long i=1l;
                    for ( i = 1L; i <cv ; i++) {
                        long new_cv=i+cv;
                        String str=key+"="+new_cv+"&";
                        Matcher m = r.matcher(str);
                        if (!m.matches()){
                            System.out.println("error->"+str);
                        }

                    }
                }
                break;
            default:
                break;
        }


    }


    private static void test1(){
        String data= "wafdata:119.97.137.47:1.1.1.1:1:113.57.182.184:311:20220902141903:1";
        String pattern = "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(data);
        String attackIp="";
        while (m.find()){
            attackIp=m.group();
        }
        System.out.println(attackIp);
    }

    private static void test2(){
        System.out.println("====test2====");
        String str="k00=127.0.0.1&k01=127.0.0.1&k02=/&k03=&k04=&k05=curl/7.29.0&k06=0000000073&k07=&k08=&k09=&k10=GET&k11=0000000000&k12=0000000000&k13=0000000000&k14=0000000001&k15=0000000000&k16=0000000000&k17=0000000000&k18=0000000000&k19=00000000000000000000000000000000&k20=00000000000000000000000000000010&k21=0000000001&k22=0000000001&";
        List<String> keys= RegexOpTypeEnum.getAllKey();
        for (String handle:keys){
            String opCnName=RegexOpTypeEnum.getCnNameByKey(handle);
            System.out.println("操作类型-->"+opCnName);
            //Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKi
            String reg=createJavaPatternRegStr("k11",handle,"1");
            if (StringUtils.isBlank(reg)){
                System.out.println( opCnName+" reg is null");
                continue;
            }
            System.out.println("regex:"+reg);
            //String reg1="b3=(|(/)|(/[A-Za-bd-z0-9][^&]*)|(/c)|(/c[A-Za-ce-z0-9][^&]*)|(/cd)|(/cd[A-Za-mo-z0-9][^&]*)|(/cdn[^&][^&]*)|[^&]{5,64})&.*";
            //String reg2="b3=((/)|(/[A-Za-bd-z0-9][^&]*)|(/c)|(/c[A-Za-ce-z0-9][^&]*)|(/cd)|(/cd[A-Za-mo-z0-9][^&]*)|(/cdn[^&]*)|[^&]{5,64})&.*";
            Pattern r = Pattern.compile(reg);
            Matcher m = r.matcher(str);
            while (m.find()){
                System.out.println(ANSI_RED + "true" + ANSI_RESET);
                System.out.println(m.group());;
            }
        }

    }



    private static void test4(String c){
        //k15>500  ==303 3600                   k15=(0*([1-9]\d{3,31})|0*[6-9]\d{2}|0*5[1-9]\d{1}|0*50[1-9])&.*
        //k14=10 && k17>8  ==303 3600           k14=0000000010&.*k17=(0*([1-9]\d{1,31})|0*[8-9])&.*
        //k14=10 && k19<2  ==303 3600           k14=0000000010&.*k19=0*[0-2]&.*
        //k11=10&k14=0  ==303 3600              k11=0000000000&.*k14=0000000010&.*
        //k15>10 & k05='' ==303 3600            k05=&.*k15=(0*([1-9]\d{2,31})|0*[2-9]\d{1}|0*1[1-9])&.*

        //pass
        //k18>1 ==100                           k18=(0*([1-9]\d{1,31})|0*[2-9])&.*
        Integer maxC=Integer.parseInt(c)*10;
        String reg=createJavaPatternRegStrV300("k15","ile",c);
        System.out.println("value:"+c);
        System.out.println("reg:"+reg);
        Pattern r = Pattern.compile(reg);
        int mac=0;
        for (int i = 0; i < maxC; i++) {
            String str="k15="+i+"&.*";
            Matcher m = r.matcher(str);
            if (m.matches()){
                mac++;
                if (i>Integer.parseInt(c)){
                    System.out.println(ANSI_RED+c+"----"+str);
                }
            }else{

            }
        }
        System.out.println(c);
        System.out.println(mac);
    }

    private static void test5(){
        String key="k17";
        String handle="bge";
        String content="16";
        String reg=createJavaPatternRegStr(key,handle,content);
        // String str="k17=00000000000000000000000000000000&";
        String str="k17=10010000000000000000010000001101&";
        Pattern r = Pattern.compile(reg);
        Matcher m=r.matcher(str);
        if (m.matches()){
            System.out.println(ANSI_RED + "true" + ANSI_RESET);
        }
        System.out.println("reg=="+reg);
    }


    public static List<SysWafRuleVo> buildSysWafByKey(SysWafRuleConfVo vo){
        List<SysWafRuleVo> list=new ArrayList<>();
        int show_s=1;
        if ( 1==vo.getBadRequest()){
            list.addAll(getSysWafRuleByType("badRequest",show_s));
        }
        if (1==vo.getHighLimit()){
            list.addAll(getSysWafRuleByType("highLimit",show_s));
        }
        if (1==vo.getForbidSeal()){
            list.addAll(getSysWafRuleByType("forbidSeal",show_s));
        }
        if(1==vo.getPostCheck()){
            list.addAll(getSysWafRuleByType("postCheck",show_s));
        }
        if (1==vo.getHotUrlCheck()){
            //hotHigh
           list.addAll(getSysWafRuleByType("hotUrlCheck",show_s));
        }
        if (1==vo.getHotUrlCheckLow()){
            list.addAll(getSysWafRuleByType("hotUrlCheckLow",show_s));
        }
        if (1==vo.getRandomCheck()){
            list.addAll(getSysWafRuleByType("randomCheck",show_s));
        }
        if (1== vo.getBotCheck()){
            list.addAll(getSysWafRuleByType("botCheck",show_s));
        }
        if (1==vo.getRefererUrl()){
            list.addAll(getSysWafRuleByType("refererUrl",show_s));
        }
        if (1==vo.getRandomReq()){
            list.addAll(getSysWafRuleByType("randomReq",show_s));
        }
        if (1==vo.getLowLimit()){
            list.addAll(getSysWafRuleByType("lowLimit",show_s));
        }
        return list;
    }

    private static List<SysWafRuleVo> getSysWafRuleByType(String key,int showDetailStatus){
        List<SysWafRuleVo> list=new ArrayList<>();
        int rataI=1;
        int perMinuteReq=1000;
        switch (key){
            case "highLimit":
                if (true){
                    //并发超出处理验证
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("高频限制");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_1_sum");
                        ruleObj.setHandle("ige");
                        Integer value=1200;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);
                    }
                    //拉黑
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                    Integer value=1440;
                    vo.getWaf_op().setParam(String.valueOf(value));
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "forbidSeal":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("封禁海外");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("geoip");
                        ruleObj.setHandle("ue");
                        ruleObj.setContent("CN|HK|MO|TWN");
                        vo.getRule().add(ruleObj);


                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                    Integer value=1440;
                    vo.getWaf_op().setParam(String.valueOf(value));
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "badRequest":
                if (true){
                    if (true){
                        SysWafRuleVo vo=new SysWafRuleVo();
                        vo.setRemark("异常请求");
                        if (1==showDetailStatus){
                            SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                            ruleObj.setType("method");
                            ruleObj.setHandle("ue");
                            ruleObj.setContent("GET|POST|HEAD|PUT|DELETE|CONNECT|OPTION|TRACE|PATCH");
                            vo.getRule().add(ruleObj);

                        }
                        vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                        Integer value=1440;
                        vo.getWaf_op().setParam(String.valueOf(value));
                        vo.getWaf_op().setHandle("forbid");
                        vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                        vo.setReq_sum_5s(1000);
                        list.add(vo);
                    }

                    if (true){
                        SysWafRuleVo vo=new SysWafRuleVo();
                        vo.setRemark("异常请求");
                        if (1==showDetailStatus){
                            SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                            ruleObj.setType("ip_1_sum");
                            ruleObj.setHandle("ile");
                            Integer value=6+4*rataI;
                            ruleObj.setContent(String.valueOf(value));
                            vo.getRule().add(ruleObj);

                            ruleObj=vo.new ruleObj();
                            ruleObj.setType("ip_400_sum");
                            ruleObj.setHandle("ige");
                            value=4+2*rataI;
                            ruleObj.setContent(String.valueOf(value));
                            vo.getRule().add(ruleObj);

                        }
                        vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                        Integer value=1440;
                        vo.getWaf_op().setParam(String.valueOf(value));
                        vo.getWaf_op().setHandle("forbid");
                        vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                        vo.setReq_sum_5s(1000);
                        list.add(vo);
                    }

                    if (true){
                        SysWafRuleVo vo=new SysWafRuleVo();
                        vo.setRemark("异常请求");
                        if (1==showDetailStatus){
                            SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                            ruleObj.setType("ip_1_sum");
                            ruleObj.setHandle("ile");
                            Integer value=6+4*rataI;
                            ruleObj.setContent(String.valueOf(value));
                            vo.getRule().add(ruleObj);

                            ruleObj=vo.new ruleObj();
                            ruleObj.setType("ip_404_sum");
                            ruleObj.setHandle("ige");
                            value=4+2*rataI;
                            ruleObj.setContent(String.valueOf(value));
                            vo.getRule().add(ruleObj);

                        }
                        vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                        Integer value=1440;
                        vo.getWaf_op().setParam(String.valueOf(value));
                        vo.getWaf_op().setHandle("forbid");
                        vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                        vo.setReq_sum_5s(1000);
                        list.add(vo);
                    }
                }
                break;
            case "postCheck":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("POST防护");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("method");
                        ruleObj.setHandle("e");
                        ruleObj.setContent("POST");
                        vo.getRule().add(ruleObj);

                        ruleObj=vo.new ruleObj();
                        ruleObj.setType("referer");
                        ruleObj.setHandle("null");
                        ruleObj.setContent(" ");
                        vo.getRule().add(ruleObj);

                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                    Integer value=1440;
                    vo.getWaf_op().setParam(String.valueOf(value));
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "randomCheck":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("随机验证");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_1_sum");
                        ruleObj.setHandle("ige");
                        Integer value=20;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);

                        ruleObj=vo.new ruleObj();
                        ruleObj.setType("head_cookies_success_sum");
                        ruleObj.setHandle("ile");
                        ruleObj.setContent("2");
                        vo.getRule().add(ruleObj);


                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                    Integer value=1440;
                    vo.getWaf_op().setParam(String.valueOf(value));
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "limitUrlRate":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("接口防护");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_uri_n_sum");
                        ruleObj.setHandle("ige");
                        Integer value=10;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);
                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_SUSPICIOUS_VERIFY_DIRECTIONAL.getKey());
                    vo.getWaf_op().setParam("0");
                    vo.getWaf_op().setHandle("suspicious");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "isReset":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("重试验证");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("server_1_sum");
                        ruleObj.setHandle("ige");
                        Integer value=1000;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);
                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_SUSPICIOUS_VERIFY_DIRECTIONAL.getKey());
                    vo.getWaf_op().setParam("0");
                    vo.getWaf_op().setHandle("suspicious");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "hotUrlCheckLow":
                if (true){
                    if (false){
                        //热U
                        SysWafRuleVo vo=new SysWafRuleVo();
                        vo.setRemark("热U拦截");
                        if (1==showDetailStatus){
                            SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                            ruleObj.setType("ip_5_sum");
                            ruleObj.setHandle("ige");
                            Integer value=20+4*rataI;
                            ruleObj.setContent(String.valueOf(value));
                            vo.getRule().add(ruleObj);

                            ruleObj=vo.new ruleObj();
                            ruleObj.setType("server_1_sum");
                            ruleObj.setHandle("ige");
                            value=perMinuteReq;
                            ruleObj.setContent(String.valueOf(value));
                            vo.getRule().add(ruleObj);
                        }
                        vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                        Integer value=1440;
                        vo.getWaf_op().setParam(String.valueOf(value));
                        vo.getWaf_op().setHandle("forbid");
                        vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                        vo.setReq_sum_5s(1000);
                        list.add(vo);
                    }

                    if (true){
                        //低频--回源热U
                        if (false){
                            SysWafRuleVo vo=new SysWafRuleVo();
                            vo.setRemark("回源热U");
                            if (1==showDetailStatus){
                                SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                                ruleObj.setType("ip_5_sum");
                                ruleObj.setHandle("ige");
                                Integer value=5;
                                ruleObj.setContent(String.valueOf(value));
                                vo.getRule().add(ruleObj);


                                ruleObj=vo.new ruleObj();
                                ruleObj.setType("ip_1_sum");
                                ruleObj.setHandle("ile");
                                value=10;
                                ruleObj.setContent(String.valueOf(value));
                                vo.getRule().add(ruleObj);

                                ruleObj=vo.new ruleObj();
                                ruleObj.setType("server_1_sum");
                                ruleObj.setHandle("ige");
                                value=500;
                                ruleObj.setContent(String.valueOf(value));
                                vo.getRule().add(ruleObj);
                            }
                            vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                            Integer value=1440;
                            vo.getWaf_op().setParam(String.valueOf(value));
                            vo.getWaf_op().setHandle("forbid");
                            vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                            vo.setReq_sum_5s(1000);
                            list.add(vo);
                        }

                        if (false){
                            SysWafRuleVo vo=new SysWafRuleVo();
                            vo.setRemark("回源热U");
                            if (1==showDetailStatus){
                                SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                                ruleObj.setType("ip_total");
                                ruleObj.setHandle("ie");
                                Integer value=1;
                                ruleObj.setContent(String.valueOf(value));
                                vo.getRule().add(ruleObj);


                                ruleObj=vo.new ruleObj();
                                ruleObj.setType("ip_5_sum");
                                ruleObj.setHandle("ie");
                                value=1;
                                ruleObj.setContent(String.valueOf(value));
                                vo.getRule().add(ruleObj);

                                ruleObj=vo.new ruleObj();
                                ruleObj.setType("server_1_sum");
                                ruleObj.setHandle("ige");
                                value=1000;
                                ruleObj.setContent(String.valueOf(value));
                                vo.getRule().add(ruleObj);
                            }
                            vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                            Integer value=1440;
                            vo.getWaf_op().setParam(String.valueOf(value));
                            vo.getWaf_op().setHandle("forbid");
                            vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                            vo.setReq_sum_5s(1000);
                            list.add(vo);
                        }

                        if (true){
                            SysWafRuleVo vo=new SysWafRuleVo();
                            vo.setRemark("回源热U");
                            if (1==showDetailStatus){
                                SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                                ruleObj=vo.new ruleObj();
                                ruleObj.setType("ip_5_sum");
                                ruleObj.setHandle("ige");
                                ruleObj.setContent(String.valueOf(20));
                                vo.getRule().add(ruleObj);
                            }
                            vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                            Integer value=1440;
                            vo.getWaf_op().setParam(String.valueOf(value));
                            vo.getWaf_op().setHandle("forbid");
                            vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                            vo.setReq_sum_5s(1000);
                            list.add(vo);
                        }

                    }
                }
                break;
            case "hotUrlCheck":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("高频热U");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("server_5_sum");
                        ruleObj.setHandle("ige");
                        Integer value=30;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);

                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                    Integer value=1440;
                    vo.getWaf_op().setParam(String.valueOf(value));
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "botCheck":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("人机验证");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("method");
                        ruleObj.setHandle("e");
                        ruleObj.setContent("GET|POST|HEAD|PUT|DELETE|CONNECT|OPTION|TRACE|PATCH");
                        vo.getRule().add(ruleObj);

                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_SUSPICIOUS_VERIFY_AUTO_RELOAD_V2.getKey());
                    vo.getWaf_op().setParam("0");
                    vo.getWaf_op().setHandle("suspicious");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "refererUrl":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("来源验证");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_referer_verify");
                        ruleObj.setHandle("ie");
                        Integer value=0;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);
                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_RETURN_CODE.getKey());
                    vo.getWaf_op().setParam("444");
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "randomReq":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("随机请求");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_total");
                        ruleObj.setHandle("ile");
                        Integer value=30;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);


                        ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_url_hash_info");
                        ruleObj.setHandle("bge");
                        value=20;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);


                        ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_content_type_info");
                        ruleObj.setHandle("ble");
                        value=2;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);
                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                    Integer value=1440;
                    vo.getWaf_op().setParam(String.valueOf(value));
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            case "lowLimit":
                if (true){
                    SysWafRuleVo vo=new SysWafRuleVo();
                    vo.setRemark("低频请求");
                    if (1==showDetailStatus){
                        SysWafRuleVo.ruleObj ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_total");
                        ruleObj.setHandle("ige");
                        Integer value=20;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);

                        ruleObj=vo.new ruleObj();
                        ruleObj.setType("ip_url_hash_info");
                        ruleObj.setHandle("beq");
                        value=1;
                        ruleObj.setContent(String.valueOf(value));
                        vo.getRule().add(ruleObj);

                    }
                    vo.getWaf_op().setKey(WafOpEnum.RESULT_FORBID_NFX_BLOCK_ONE.getKey());
                    Integer value=1440;
                    vo.getWaf_op().setParam(String.valueOf(value));
                    vo.getWaf_op().setHandle("forbid");
                    vo.setSys_index(SysWafRuleConfVo.getWafRuleTypeId(key));
                    vo.setReq_sum_5s(1000);
                    list.add(vo);
                }
                break;
            default:
                break;

        }

        return list;
    }

    public static List<SysWafRuleVo> getSysWafList(SysCreateWafRuleForm form, int showDetailStatus){
        List<SysWafRuleVo> list=new ArrayList<>();
        if (0==form.getCoefficient()){
            return list;
        }
        //int rataI=3-form.getCoefficient();

        if (1==form.getHighLimit()){
            list.addAll(getSysWafRuleByType("highLimit",showDetailStatus));
        }

        if (1==form.getForbidSeal()){
            // 封海外
            list.addAll(getSysWafRuleByType("forbidSeal",showDetailStatus));
        }

        if (1==form.getBadRequest()){
            //异常请求
            list.addAll(getSysWafRuleByType("badRequest",showDetailStatus));

        }

        if (1==form.getHotUrlCheck()){
            //热U拦截
            list.addAll(getSysWafRuleByType("hotLow",showDetailStatus));
        }


        if (1==form.getPostCheck()){
            // post检测
            list.addAll(getSysWafRuleByType("postCheck",showDetailStatus));
        }


        if (1==form.getIsReset()){
            // 随机验证
            list.addAll(getSysWafRuleByType("randomCheck",showDetailStatus));

            //重试验证
            list.addAll(getSysWafRuleByType("isReset",showDetailStatus));
        }


        if (form.getLimitUrlRate()>0){
            //限制URL (API 频率)
            list.addAll(getSysWafRuleByType("limitUrlRate",showDetailStatus));
        }


        if (form.getBotCheck()>0){
            //人机验证 botCheck
            list.addAll(getSysWafRuleByType("botCheck",showDetailStatus));

        }

        if (form.getRandomReq()>0){
            //随机请求
            list.addAll(getSysWafRuleByType("randomReq",showDetailStatus));
        }

        if (form.getLowLimit()>0){
            //低频请求
            list.addAll(getSysWafRuleByType("lowLimit",showDetailStatus));
        }

        return list;
    }


    private static void test_aa(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    String r=HttpRequest.sendGet("http://d03.cdntest.baidu.top","");
                    System.out.println(System.currentTimeMillis());

                }
            }
        }).start();
    }

    private static void test3(){
        // ile ige
        String str="k00=127.0.0.1&k01=127.0.0.1&k02=/&k03=&k04=&k05=curl/7.29.0&k06=0000000073&k07=&k08=&k09=&k10=GET&k11=12&k12=0000000000&k13=0000000000&k14=0000000001&k15=0000000000&k16=0000000000&k17=0000000000&k18=0000000000&k19=00000000000000000000000000000000&k20=00000000000000000000000000000010&k21=0000000001&k22=0000000001&";
        String[] handle={"ile","ige"};
        for (String h:handle){
            String reg=createJavaPatternRegStrV300("k11",h,"2");
            System.out.println("========="+h+":"+reg);
            Pattern r = Pattern.compile(reg);
            Matcher m = r.matcher(str);
            while (m.find()){
                System.out.println(ANSI_RED + "true" + ANSI_RESET+":"+h);
                System.out.println(m.group());;
            }
        }
    }
    public static void main(String[] args)   {
        // System.out.println(PreciseWafParamEnum.getTransLinkMap());
        //        for (int i = 0; i <101 ; i++) {
        //            test4(String.valueOf(i));
        //        }
        //PushSetEnum item=   PushSetEnum.getItemByPath("/home/local/nginx/conf/conf/waf/white_ip_4333_ww.51.com_");
        //System.out.println(item.getGroup());
//        test_aa();
//        test_aa();
        test3();
    }
}
