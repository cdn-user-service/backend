package io.ants.modules.sys.enums;


import io.ants.modules.sys.vo.InjPenVo;


import java.util.ArrayList;
import java.util.List;

public enum NgxInjectioPenetrationEnum {

    SQL_SFL(0,"SQL注入检测","select","检测构造select查询语句，防止非法获取数据","select.+(from|limit)"),
    SQL_UN(1,"SQL注入检测","union","检测构造union查询语句，防止非法获取数据","union.+select"),
    SQL_SLEEP(2,"SQL注入检测","sleep","检测sleep调用,防止非法测试指令返回值","sleep\\((\\s*)(\\d*)(\\s*)\\)"),
    SQL_WAIT(3, "SQL注入检测", "delay", "检测waitfor delay,防止非法测试指令返回值","(sleep\\(\\))|(waitfor delay)"),
    SQL_INFO(4,"SQL注入检测","information_schema","检测查询information_schema，防止非法获取表结构","from.+information_schema.+"),
    SQL_DB(5,"SQL注入检测","database","检测查询database，防止非法获取表结构","current_.+(use|database|schema|connection_id)"),
    SQL_DUMP(6,"SQL注入检测","dump","检测使用dump,out等函数，防止非法导出表数据","into(\\s+)+(dump|out)file\\s*"),
    SQL_SET(7,"SQL注入检测","update","检测构造update更新表语句，防止非法篡改数据","update.+set.+"),
    SQL_DELETE(8,"SQL注入检测","delete","检测构造delete删除表语句，防止非法删除数据","delete.+from.+"),

    PEN_CHMOD(9,"渗透检测","chmod","检测构造修改文件权限指令，防止非法执行","chmod\\s+(\\S+)\\s+(.+)"),
    PEN_PWD(10,"渗透检测","passwd","检测构造查询系统密码指令，防止非法修侵入系统","/etc/passwd"),
    PEN_EXE(11,"渗透检测","execute","检测eval,execute等执行指令，防止非法执行","(eval|system|exec|execute|passthru|shell_exec|phpinfo)\\s+\\S+"),
    PEN_EXE2(12,"渗透检测","phpinfo","检测phpinfo，showmodaldialog等指令，防止非法获取系统信息","\\s+(phpinfo|showmodaldialog)\\s+"),
    PEN_INJ(13,"渗透检测","whoami","检测命令注入","(pwd|ls|ll|whoami|id|net\\s+user)\\s*"),

    XSS_JS(14,"跨站脚本攻击检测","onmouseover","检测Javascript事件","(onmouseover|onmousemove|onmousedown|onmouseup|onerror|onload|onclick|ondblclick|onkeydown|onkeyup|onkeypress)\\s*="),
    XSS_FUNC(15,"跨站脚本攻击检测","alert","检测Javascript函数","(alert|eval|prompt|confirm)\\s*"),
    XSS_LABEL(16,"跨站脚本攻击检测","iframe","检测HTML标签","<(script|iframe|link)"),
    ;
     private Integer index;
     private String groupName;
     private String name;
     private String remark;
     private String regContent;

    NgxInjectioPenetrationEnum(Integer index,String groupName,String name,String remark,String regContent){
        this.index=index;
        this.groupName=groupName;
        this.name=name;
        this.remark=remark;
        this.regContent=regContent;
    }

    public Integer getIndex() {
        return index;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getName() {
        return name;
    }

    public String getRegContent() {
        return regContent;
    }

    public String getRemark() {
        return remark;
    }

    public static String getAllRegx(){
        StringBuilder sb=new StringBuilder();
        for (NgxInjectioPenetrationEnum item:NgxInjectioPenetrationEnum.values()){
            sb.append(item.getRegContent());
            sb.append("\n");
        }
        return sb.toString();
    }

    public static List<InjPenVo> getAllInfo(){
        List<InjPenVo>resultList=new ArrayList<>();
        for (NgxInjectioPenetrationEnum item:NgxInjectioPenetrationEnum.values()){
            InjPenVo vo=new InjPenVo();
            vo.setId(item.getIndex());
            vo.setGroup(item.getGroupName());
            vo.setName(item.getName());
            vo.setRemark(item.getRemark());
            resultList.add(vo);
        }
        return resultList;
    }

}
