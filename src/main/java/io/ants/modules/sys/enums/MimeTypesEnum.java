package io.ants.modules.sys.enums;

import java.util.HashMap;
import java.util.Map;

public enum MimeTypesEnum {
    HTML("text/html","html htm shtml"),
    CSS("text/css","css"),
    XML("text/xml","xml"),
    GIF("image/gif","gif"),
    JPG("image/jpeg","jpeg jpg"),
    JS("application/javascript","js"),
    ATOM("application/atom+xml","atom"),
    RSS("application/rss+xml","rss"),

    MML("text/mathml","mml"),
    TXT("text/plain","txt"),
    JAD("text/vnd.sun.j2me.app-descriptor","jad"),
    WML("text/vnd.wap.wml","wml"),
    HTC("text/x-component","htc"),
    PNG("image/png","png"),
    SVG("image/svg+xml","svg svgz"),
    TIF("image/tiff","tif tiff"),
    WBMP("image/vnd.wap.wbmp","wbmp"),
    WBEP("image/webp","webp"),
    ICO("image/x-icon","ico"),
    JNP("image/x-jng","jng"),
    BMP("image/x-ms-bmp","bmp"),

    WOFF("font/woff","woff"),
    WOFF2("font/woff2","woff2"),

    JAR("application/java-archive","jar war ear"),
    JSON("application/json","json"),
    HQX("application/mac-binhex40","hqx"),
    DOC("application/msword","doc"),
    PDF("application/pdf","pdf"),
    PS("application/postscript","ps eps ai"),
    RTF("application/rtf","rtf"),
    M3U8("application/vnd.apple.mpegurl","m3u8"),
    KML("application/vnd.google-earth.kml+xml","kml"),
    KMZ("application/vnd.google-earth.kmz","kmz"),
    XLS("application/vnd.ms-excel","xls"),
    EOT("application/vnd.ms-fontobject","eot"),
    PPT("application/vnd.ms-powerpoint","ppt"),
    ODG("application/vnd.oasis.opendocument.graphics","odg"),
    ODP("application/vnd.oasis.opendocument.presentation","odp"),
    ODS("application/vnd.oasis.opendocument.spreadsheet","ods"),
    ODT("application/vnd.oasis.opendocument.text","odt"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation","pptx"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","xlsx"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document","docx"),
    WMLC("application/vnd.wap.wmlc","wmlc"),
    X7Z("application/x-7z-compressed","7z"),
    CCO("application/x-cocoa","cco"),
    JARDIFF("application/x-java-archive-diff","jardiff"),
    JNLP("application/x-java-jnlp-file","jnlp"),
    RUN("application/x-makeself","run"),
    PL("application/x-perl","pl pm"),
    PRC("application/x-pilot","prc pdb"),
    RAR("application/x-rar-compressed","rar"),
    RPM("application/x-redhat-package-manager","rpm"),
    SEA("application/x-sea","sea"),
    SWF("application/x-shockwave-flash","swf"),
    SIT("application/x-stuffit","sit"),
    TCL("application/x-tcl","tcl tk"),
    CRT("application/x-x509-ca-cert","der pem crt"),
    XPI("application/x-xpinstall","xpi"),
    XHTML("application/xhtml+xml","xhtml"),
    XSPF("application/xspf+xml","xspf"),
    ZIP("application/zip","zip"),

    BIN("application/octet-stream","bin exe dll"),
    DEB("application/octet-stream","deb"),
    DMG("application/octet-stream","dmg"),
    ISO("application/octet-stream","iso img"),
    MSI("application/octet-stream","msi msp msm"),

    MID("audio/midi","mid midi kar"),
    MP3("audio/mpeg","mp3"),
    OGG("audio/ogg","ogg"),
    M4A("audio/x-m4a","m4a"),
    RA("audio/x-realaudio","ra"),

    M3GP("video/3gpp","3gpp 3gp"),
    TS("video/mp2t","ts"),
    MP4("video/mp4","mp4"),
    MOV("video/mpeg","mpeg mpg"),
    WEBM("video/quicktime","mov"),
    FLV("video/webm","webm"),
    M4V("video/x-flv","flv"),
    MNG("video/x-m4v","m4v"),
    ASX("video/x-mng","mng"),
    ASF("video/x-ms-asf","asx asf"),
    WMV("video/x-ms-wmv","wmv"),
    AVI("video/x-msvideo","avi"),
    ;
    private final String mime;
    private final String type;
    MimeTypesEnum(String mime,String type){
        this.mime=mime;
        this.type=type;
    }

    public String getType() {
        return type;
    }

    public String getMime() {
        return mime;
    }

    public static Map GetAll(){
        Map map=new HashMap();
        for (MimeTypesEnum item : MimeTypesEnum.values()) {
            map.put(item.getType(),item.getMime());
        }
        return map;
    }
}
