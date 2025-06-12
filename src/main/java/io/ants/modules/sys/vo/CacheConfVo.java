package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class CacheConfVo {

    /*
    *
    * 	"type": 1,
	"content": "mp3|flv|swf|wma|wav|mp4|mov|mpeg|rm|avi|wmv|mkv|vob|rmvb|asf|mpg|ogg|m3u8|ts|mid|midi|3gp|css|js|txt|iso|img|exe|zip|rar|7z|gz|tar|apk|ipa|dmg|manifest|conf|xml|cab|bin|msi",
	"time": 300,
	"unit": "d",
	"mode": "gho",
	"id": 527
	*
	* */

   private Integer id=0;

   private String mode;

   private Integer type;

   private String content;

   private Integer time;

   private String unit ;
}
