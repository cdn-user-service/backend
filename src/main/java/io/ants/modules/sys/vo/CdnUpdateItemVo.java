package io.ants.modules.sys.vo;

import lombok.Data;

@Data
public class CdnUpdateItemVo {
    //  "addtime": "1693561667",
    //        "goods": "nginx",
    //        "version": "-",
    //        "title": null,
    //        "info": null,
    //        "url": "http://download.antsxdp.com/antscdn20/nginx-3.0.3-1693561667.tar.gz",
    //        "mkurl": "/home/local/",
    //        "hash": "3.0.3",
    //        "hash2": "fc3c1815a19d17c1fca2afc8683d2826",
    //        "version_date": "1693561667"

    private String addtime="";
    private String goods="";
    private String version="";
    private String title="";
    private String info="";
    private String url="";
    private String mkurl="";
    private String hash="";
    private String hash2="";
    private String version_date="";

    /**
     * 更新前置动作
     */
    private String precmd="";

    /**
     * 更新后置动作
     */
    private String latecmd="";
}
