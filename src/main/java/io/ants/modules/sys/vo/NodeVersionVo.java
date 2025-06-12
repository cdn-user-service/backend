package io.ants.modules.sys.vo;


import io.ants.common.utils.DataTypeConversionUtil;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

@Data
public class NodeVersionVo {
    private String agentVersion="--";

    private String nginxVersion="1.21.4.1";

    private String nginxWafVersion="--";

    private String systemOsName;

    public static NodeVersionVo getVersionObj(String v){
        NodeVersionVo vo =new NodeVersionVo();
        try{
            if (StringUtils.isNotBlank(v)){
                //"1.27|{\"nginx_version\":\"1.19.9\",\"ants_waf\":\"2.30\"}"
                String[] agentVersionAndNgxAdnWafVersion= v.split("\\|");
                vo.setAgentVersion(agentVersionAndNgxAdnWafVersion[0]);
                if (agentVersionAndNgxAdnWafVersion.length>=2){
                    NgxVersionObjVo nvvo= DataTypeConversionUtil.string2Entity(agentVersionAndNgxAdnWafVersion[1],NgxVersionObjVo.class);
                    if (null!=nvvo){
                        vo.setNginxWafVersion(nvvo.getAnts_waf());
                        vo.setNginxVersion(nvvo.getNginx_version());
                        vo.setSystemOsName(nvvo.getOs());
                    }
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println(v);
        }
        return vo;
    }
}
