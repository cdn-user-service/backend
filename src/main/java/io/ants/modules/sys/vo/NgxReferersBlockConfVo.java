package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class NgxReferersBlockConfVo {

    //            "match_mode": 0,
    //            "no_blocked_referers": false,
    //            "return_code": "404",
    //            "match_domains": "*.test.com",
    //            "match_uri_type": 1,
    //            "match_uri": "*.test.com|css|js|txt|iso|img|exe|zip|rar|7z|gz|tar|apk|ipa|dmg|manifest|conf|xml|cab|bin|msi"
    //
    private Integer match_mode;
    private boolean no_blocked_referers;
    private String  return_code;
    private String  match_domains;
    private Integer match_uri_type;
    private String  match_uri ;

}
