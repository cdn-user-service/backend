/**
 * 
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.common.utils;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 返回数据
 *
 * @author Mark sunlightcs@gmail.com
 */
public class R extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;
	
	public R() {
		put("code", 1);
		put("msg", "success");
	}


	public static R error() {
		return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "数据异常,请检查参数！");
	}
	
	public static R error(String msg) {
		return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
	}
	
	public static R error(int code, String msg) {
		R r = new R();
		r.put("code", code);
		r.put("msg", msg);
		return r;
	}

	public static R ok(String msg) {
		R r = new R();
		r.put("msg", msg);
		return r;
	}
	
	public static R ok(Map<String, Object> map) {
		R r = new R();
		r.putAll(map);
		return r;
	}

	public String getMsg(){
		if (this.containsKey("msg") && null!=this.get("msg")){
			return this.get("msg").toString();
		}
		return "";
	}

	public  Integer getCode(){
		if (this.containsKey("code")){
			return Integer.parseInt(this.get("code").toString());
		}
		return 0;
	}

	public static R ok() {
		return new R();
	}

	@Override
	public R put(String key, Object value) {
		if (null==value){
			super.put(key, "");
			return  this;
		}
		super.put(key, value);
		return this;
	}

	public String toJsonString(){
		return DataTypeConversionUtil.map2json(this).toJSONString();
	}

}
