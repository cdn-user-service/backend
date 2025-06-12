package io.ants.modules.app.vo;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class StreamInfoVo {
	  /*
	 {
		"listen": "88",
		"protocol": "TCP/UDP",
		"server_mode": "weight",
		"proxy_protocol": 0,
		"proxy_timeout": "30s",
		"proxy_connect_timeout": "60s",
		"server": ["1.1.1.1:89 weight=1"]
	}
	   */
	  private String listen;
	  private String  protocol="TCP/UDP";
	  private String  server_mode="weight";
	  private String  proxy_timeout="30s";
	  private String  proxy_connect_timeout="60s";
	  private Integer proxy_protocol=0;
	  private JSONArray server;

}
