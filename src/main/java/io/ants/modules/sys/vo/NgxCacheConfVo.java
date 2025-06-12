package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxCacheConfVo {

   //{"type":1,"content":"css|txt|iso|img|exe|zip|rar|7z|gz|tar|apk|ipa|dmg|manifest|conf|xml|cab|bin|msi|jpg|jpeg|gif|ico|png|bmp|webp|psd|tif|tiff|svg|svgz|mp3|flv|swf|wma|wav|mp4|mov|mpeg|rm|avi|wmv|mkv|vob|rmvb|asf|mpg|ogg|m3u8|ts|mid|midi|3gp|js","time":30,"unit":"m","mode":"cache"}
   //{"type":2,"content":"/aaa/","time":24,"unit":"h","mode":"cache"}

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
