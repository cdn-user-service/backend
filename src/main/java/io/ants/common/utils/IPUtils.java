/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants.common.utils;


import com.alibaba.druid.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jsonwebtoken.lang.Strings;
import org.apache.commons.net.util.SubnetUtils;

import javax.naming.*;
import javax.naming.directory.*;

/**
 * IP地址
 *
 * @author Mark sunlightcs@gmail.com
 */
public class IPUtils {
	//private static Logger logger = LoggerFactory.getLogger(IPUtils.class);

	/**
	 * 获取IP地址
	 * 
	 * 使用Nginx等反向代理软件， 则不能通过request.getRemoteAddr()获取IP地址
	 * 如果使用了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP地址，X-Forwarded-For中第一个非unknown的有效IP字符串，则为真实IP地址
	 */
	public static String getIpAddr(HttpServletRequest request) {
    	String ip = "127.0.0.1";
        try {
            ip = request.getHeader("x-forwarded-for");
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {
        	//logger.error("IPUtils ERROR ", e);
        }

        //        //使用代理，则获取第一个IP地址
        //        if(StringUtils.isEmpty(ip) && ip.length() > 15) {
        //			if(ip.indexOf(",") > 0) {
        //				ip = ip.substring(0, ip.indexOf(","));
        //			}
        //		}
        //使用代理，则获取第一个IP地址
        if(!StringUtils.isEmpty(ip) && ip.indexOf(",") > 0) {
            ip = ip.substring(0, ip.indexOf(","));

        }
        
        return ip;
    }

    private static final String IPV4_REGEX =  "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";

    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

    public static boolean isValidIPV4ByCustomRegex(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        if (!IPV4_PATTERN.matcher(ip).matches()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        try {
            for (String segment : parts) {
                boolean testStatus=Integer.parseInt(segment) > 255 ||  (segment.length() > 1 && segment.startsWith("0"));
                if (testStatus) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isValidIPV6(String ip){
        if (StringUtils.isEmpty(ip)){
            return false;
        }
        String pattern = "^\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:)(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}))|:)))(%.+)?\\s*$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(ip);
        return m.matches();
    }

    public static boolean isValidIPV4(String ip){
        try{
            if (StringUtils.isEmpty(ip) ){
                return false;
            }
            String nIp=long2ip(ip2long(ip));
            if (nIp.equals(ip)){
                return true;
            }
        }catch (Exception e){
            System.out.println(ip+" :"+e.getMessage());
            //System.out.println(e.getMessage());
        }
        return false;
    }

    public static boolean isValidIPV4ByRegx(String ip){
        String pattern = "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(ip);
        boolean r1= m.matches();
        if (r1){
            return true;
        }
        return false;
    }

    public static boolean isValidDomain(String host){
        if (StringUtils.isEmpty(host)){
            return false;
        }
        //http://baidu.com:8443
        String pattern = "^http(s)?://([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}[:]?[\\d]{0,5}$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(host);
        return m.matches();
    }

    public static  boolean isValidIpv4OrIpv6(String ip){
        return isValidIPV4(ip) || isValidIPV6(ip);
    }


    public static boolean isValidPort(String port){
        if (null==port || "".equals(port) ){
            return false;
        }
        String pattern = "^([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(port);
        return m.matches();

    }

    public static boolean isValidPortEx(String port){
        if (null==port || "".equals(port) ){
            return false;
        }
        try{
            Integer p=Integer.parseInt(port);
            if (p>0 && p<65535 ){
                return true;
            }
        }catch (Exception e){
            System.out.println("isValidPortEx error:"+e.getMessage());
        }

        return false;
    }

    //核心代码，检索IP所属网段
    public static boolean isInRange(String ip, String cidr) {
        String[] ips = ip.split("\\.");
        long ipAddr = (Long.parseLong(ips[0]) << 24)
                | (Long.parseLong(ips[1]) << 16)
                | (Long.parseLong(ips[2]) << 8)
                | Long.parseLong(ips[3]);
        long type = Long.parseLong(cidr.replaceAll(".*/", ""));
        long mask = 0xFFFFFFFF << (32 - type);
        String cidrIp = cidr.replaceAll("/.*", "");
        String[] cidrIps = cidrIp.split("\\.");
        long networkIpAddr = (Long.parseLong(cidrIps[0]) << 24)
                | (Long.parseLong(cidrIps[1]) << 16)
                | (Long.parseLong(cidrIps[2]) << 8)
                | Long.parseLong(cidrIps[3]);
        return (ipAddr & mask) == (networkIpAddr & mask);
    }


    public static boolean isCidr(String ip){
        if (null==ip || "".equals(ip) ){
            return false;
        }
        final  String pattern = "^(?:(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\/([1-9]|[1-2]\\d|3[0-2])$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(ip);
        if(m.matches()){
           return true;
        }
        return false;
    }

    public static String normalizeFromCIDR(final String netspec) {
        final int bits = 32 - Integer.parseInt(netspec.substring(netspec.indexOf('/') + 1));
        final int mask = (bits == 32) ? 0 : 0xFFFFFFFF - ((1 << bits) - 1);
        return netspec.substring(0, netspec.indexOf('/') + 1) + Integer.toString(mask >> 24 & 0xFF, 10) + "." + Integer.toString(mask >> 16 & 0xFF, 10) + "." + Integer.toString(mask >> 8 & 0xFF, 10) + "." + Integer.toString(mask >> 0 & 0xFF, 10);
    }



    private static long bytesToLong(byte[] address) {
        long ipnum = 0;
        for (int i = 0; i < 4; ++i) {
            long y = address[i];
            if (y < 0) {
                y += 256;
            }
            ipnum += y << ((3 - i) * 8);
        }
        return ipnum;
    }
    public static String long2ip(long l) {
        String s = "";
        for (int i1 = 0; i1 < 4; i1++) {
            if (i1 > 0) {
                s = s + ".";
            }
            int k = (int) ((l / Math.pow(2D, (3 - i1) * 8)) % 256D);
            s = s + "" + k;
        }
        return s;
    }
    public static long ip2long(String ip) {
        try{
            long result = 0;
            if (Strings.hasLength(ip)) {
                String[] section = ip.split("\\.");
                if (section.length > 2) {
                    for (int i = 0; i < section.length; i++) {
                        result += Long.parseLong(section[i]) << ((section.length - i - 1) * 8);
                    }
                }
            }
            return result;
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return 0;

    }
    public static long[] getLowerAndUpper( String subnet){
        try{
            if (isCidr(subnet)){
                SubnetUtils utils = new SubnetUtils(subnet);
                Inet4Address a = (Inet4Address) InetAddress.getByName(utils.getInfo().getHighAddress());
                long high = bytesToLong(a.getAddress())+1;
                Inet4Address b = (Inet4Address) InetAddress.getByName(utils.getInfo().getLowAddress());
                long low = bytesToLong(b.getAddress())-1;
                //System.out.println(low);
                //System.out.println(long2ip(low));
                //System.out.println(high);
                //System.out.println(long2ip(high));
                return new long[] { low, high };
            }else if(isValidIPV4(subnet)){
                long value=ip2long(subnet);
                return new long[] { value, value };
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static String getDomainIp(String domain){
        try{
            String fDomain=domain.replaceAll("\\*","a");
            InetAddress address = InetAddress.getByName(fDomain);
            return address.getHostAddress();
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }


    public static boolean checkDomainInCname(String domain,String cname){
        try{
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            InetAddress[] cnameAddresses = InetAddress.getAllByName(cname);
            boolean match = false;
            for (InetAddress address : addresses) {
                for (InetAddress c_address:cnameAddresses){
                   if(address.getHostAddress().equals(c_address.getHostAddress())){
                       match=true;
                       break;
                   }
                }
            }
           // System.out.println("域名 " + domain + " 指向 CNAME " + cname);
            return match;
        }catch (Exception e){
            //System.out.println(e.getMessage());
            //e.printStackTrace();
        }
        return false;
    }



    public static String domain2domainStr(String domain){
        if (StringUtils.isEmpty(domain)){
            return "";
        }
        String fStr=domain.replace("*.","");
        fStr=fStr.replace(".","-");
        return  fStr;
    }


    public static void cNameLookup ( String domain ) {
        //String domain = "a1.cdntest.91hu.top";
        try{
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

            DirContext context = new InitialDirContext(env);
            Attributes attrs = context.getAttributes(domain, new String[]{"CNAME"});

            Attribute cnameAttribute = attrs.get("CNAME");
            if (cnameAttribute != null) {
                System.out.println("Canonical Name (CNAME): " + cnameAttribute.get());
            } else {
                System.out.println("No CNAME record found");
            }
            context.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static boolean ipCompare(String ip1, String ip2){
        try {
            // 原始IPv6地址
            //String ip1 = "2607:f130:0:1a0::b461:3bd3";
            //String ip2 = "2607:f130:0000:01a0:0000:0000:b461:3bd3";

            // 解析为InetAddress对象
            InetAddress addr1 = InetAddress.getByName(ip1);
            InetAddress addr2 = InetAddress.getByName(ip2);

            // 比较地址
            boolean isEqual = addr1.equals(addr2);

            return  isEqual;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static void main(String[] args) {
        //System.out.println(normalizeFromCIDR("192.168.1.1/16"));
        //System.out.println(getLowerAndUpper("192.168.1.1/16")[0]);
        //System.out.println(  "1.19".compareToIgnoreCase("1.18")>=0);
       // System.out.println(isValidIPV4(""));
       // System.out.println(checkDomainInCname("c19.cdntest.91hu.top","c19-cdntest-91hu-top.4ad5281653.165668.com"));
       // System.out.println(checkDomainInCname("c118.cd2test.91hu.top","c18cdntest-91hu-top.4ad5281653.165668.com"));

        // cNameLookup("a1.cdntest.91hu.top");
        //  if ( (fail_sum/total_sum)*100>config.getFailPercent()){
        //                    unNormalNode.add(nodeIp);
        //                }
//        int f=9;
//        int t=10;
//        double result = (double) f / t;
//        System.out.println((result));
//        System.out.println((result)*100.0);
//        System.out.println((result)*100>=99);

        //System.out.println(isValidPortEx("80,443"));
        //System.out.println(ipCompare("1.1.1.1","2.2.2.2"));
        String ip1 = "2607:f130:0:1a0::b461:3bd3";
        String ip2 = "2607:f130:0000:01a0:0000:0000:b461:3bd3";
        System.out.println(ipCompare(ip1,ip2));
        System.out.println(ipCompare("1.1.1.1","1.1.1.1"));
    }
}
