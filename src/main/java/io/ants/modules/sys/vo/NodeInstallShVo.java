package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NodeInstallShVo {
    /*
    	resultMap.put("master_ip",StaticVariableUtils.authMasterIp);
		resultMap.put("redis_conf",map);
		resultMap.put("install_node_command",iCmd);
		resultMap.put("full_install_node_command",String.format("%s -i %s -p %s ",iCmd,StaticVariableUtils.authMasterIp,requirepass));
		resultMap.put("full_install_node_command_with_ubuntu",ubuntuICmd);
    * */
    @Data
    public class RedisConf{
        private String requirepass;
    }
    private String master_ip;
    private RedisConf redis_conf=new RedisConf();
    private String install_node_command;
    private String full_install_node_command;
    private String full_install_node_command_with_ubuntu;

    //centos bbr 加速 （选装，安装后提升提高带宽利用率，降低延迟，但在某些网络环境中，反而降低网络性能）
    private String full_install_bbr_centos;
    //ubuntu bbr 加速 （选装，安装后提升提高带宽利用率，降低延迟，但在某些网络环境中，反而降低网络性能）
    private String full_install_bbr_ubuntu;

    //ubuntu22.04证书服务安装
    private String ubuntu_cert_server;
}
