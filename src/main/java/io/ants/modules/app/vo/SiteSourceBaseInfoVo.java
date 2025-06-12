package io.ants.modules.app.vo;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

@Data
public class SiteSourceBaseInfoVo {
	//{"id":602,"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"1.1.11.1","domain":"","port":"12","line":1,"weight":1}]}
 	//{"protocol":"http","port":80,"s_protocol":"http","upstream":"polling","source_set":"ip","line":[{"ip":"1.1.1.1","domain":"","port":"80","line":1,"weight":1}]}
	private Integer id=0;
	private String  protocol="http";
	private Integer port=80;
	private String  s_protocol="http";
	private String  upstream="polling";
	private String  source_set="ip";
	private JSONArray line;

}
