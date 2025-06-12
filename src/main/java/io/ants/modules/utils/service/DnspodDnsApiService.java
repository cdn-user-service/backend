package io.ants.modules.utils.service;


import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.dnspod.v20210323.DnspodClient;
import com.tencentcloudapi.dnspod.v20210323.models.*;
import io.ants.common.utils.R;
import io.ants.modules.sys.vo.DnsLineVo;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class DnspodDnsApiService {

    private final static String endPoint="dnspod.tencentcloudapi.com";
    //private final static String endPoint="api.dnspod.com";
    //https://console.cloud.tencent.com/api/explorer?Product=dnspod&Version=2021-03-23&Action=DescribeRecordList&SignVersion=

    /**
     * 获取用户等级
     * @param appId
     * @param appKey
     * @return
     */
    private static String getUserGrade(String appId,String appKey){
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeUserDetailRequest req = new DescribeUserDetailRequest();

            // 返回的resp是一个DescribeUserDetailResponse的实例，与请求对象对应
            DescribeUserDetailResponse resp = client.DescribeUserDetail(req);
            // 输出json格式的字符串回包
            return resp.getUserInfo().getUserGrade();
            //System.out.println(DescribeUserDetailResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取域名信息的域名等级
     * @param appId
     * @param appKey
     * @param domain
     * @return
     */
    private static String getDomainGradeInfo(String appId,String appKey,String domain){
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeDomainRequest req = new DescribeDomainRequest();
            req.setDomain(domain);
            // 返回的resp是一个DescribeDomainResponse的实例，与请求对象对应
            DescribeDomainResponse resp = client.DescribeDomain(req);
            return resp.getDomainInfo().getGrade();
            // 输出json格式的字符串回包
           // System.out.println(DescribeDomainResponse.toJsonString(resp));
        }catch (Exception e){
            e.printStackTrace();
        }
        return "DP_FREE";
    }

    public static R getLine(String domain, String appId, String appKey){
        LinkedHashMap map=new LinkedHashMap();
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeRecordLineListRequest req = new DescribeRecordLineListRequest();
            req.setDomain(domain);
            String dg=DnspodDnsApiService.getDomainGradeInfo(appId,appKey,domain);
            //	Domain level.
            //+ Original plan. Valid values:
            // D_FREE (Free Plan);
            // D_PLUS (Individual Plus Plan);
            // D_EXTRA (Enterprise 1 Plan);
            // D_EXPERT (Enterprise 2 Plan);
            // D_ULTRA (Enterprise 3 Plan).+ New plan. Valid values:
            // DP_FREE (Free Version);
            // DP_PLUS (Professional);
            // DP_EXTRA (Enterprise Basic);
            // DP_EXPERT (Enterprise);
            // DP_ULTRA (Ultimate).
            final String[] dps={"D_FREE","D_PLUS","D_EXTRA","D_EXPERT","D_ULTRA","DP_FREE","DP_PLUS","DP_EXTRA","DP_EXPERT","DP_ULTRA"};
            if (StringUtils.isBlank(dg)){
                dg="DP_FREE";
            }
            if (!Arrays.asList(dps).contains(dg)){
                System.out.println("InvalidParameterValue.DomainGradeInvalid:"+dg);
            }
            req.setDomainGrade(dg);
            // 返回的resp是一个DescribeRecordLineListResponse的实例，与请求对象对应
            DescribeRecordLineListResponse resp = client.DescribeRecordLineList(req);
            // 输出json格式的字符串回包
            // res=  DescribeRecordLineListResponse.toJsonString(resp);
            LineInfo[] list= resp.getLineList();

            for (LineInfo item:list){
                map.put(item.getName(),item.getName());
            }
            return R.ok().put("data",map) ;

        } catch (TencentCloudSDKException e) {
            map.put("err",e.getMessage());
            System.out.println(e);
        } catch (Exception e){
            map.put("err",e.getMessage());
            e.printStackTrace();
        }
        return R.error().put("data",map);
    }

    public static R getLineV2(String domain, String appId, String appKey){
        List<DnsLineVo> dLines=new ArrayStack();
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeRecordLineListRequest req = new DescribeRecordLineListRequest();
            req.setDomain(domain);
            String dg=DnspodDnsApiService.getDomainGradeInfo(appId,appKey,domain);
            //	Domain level.
            //+ Original plan. Valid values:
            // D_FREE (Free Plan);
            // D_PLUS (Individual Plus Plan);
            // D_EXTRA (Enterprise 1 Plan);
            // D_EXPERT (Enterprise 2 Plan);
            // D_ULTRA (Enterprise 3 Plan).+ New plan. Valid values:
            // DP_FREE (Free Version);
            // DP_PLUS (Professional);
            // DP_EXTRA (Enterprise Basic);
            // DP_EXPERT (Enterprise);
            // DP_ULTRA (Ultimate).
            final String[] dps={"D_FREE","D_PLUS","D_EXTRA","D_EXPERT","D_ULTRA","DP_FREE","DP_PLUS","DP_EXTRA","DP_EXPERT","DP_ULTRA"};
            if (StringUtils.isBlank(dg)){
                dg="DP_FREE";
            }
            if (!Arrays.asList(dps).contains(dg)){
                System.out.println("InvalidParameterValue.DomainGradeInvalid:"+dg);
            }
            req.setDomainGrade(dg);
            // 返回的resp是一个DescribeRecordLineListResponse的实例，与请求对象对应
            DescribeRecordLineListResponse resp = client.DescribeRecordLineList(req);
            // 输出json格式的字符串回包
            // res=  DescribeRecordLineListResponse.toJsonString(resp);
            LineInfo[] list= resp.getLineList();
            for (LineInfo item:list){
                DnsLineVo vo=new DnsLineVo();
                vo.setName(item.getName());
                vo.setId(item.getLineId());
                dLines.add(vo);
            }
            return R.ok().put("data",dLines) ;

        } catch (Exception e){
            DnsLineVo vo=new DnsLineVo();
            vo.setName(e.getMessage());
            vo.setId("-1");
            dLines.add(vo);
            e.printStackTrace();
        }
        return R.error().put("data",dLines);
    }

    public static R GetRecordList(String domain,String appId,String appKey){
        String eMsg="";
        try{
            //https://console.cloud.tencent.com/api/explorer?Product=dnspod&Version=2021-03-23&Action=DescribeRecordList&SignVersion=
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeRecordListRequest req = new DescribeRecordListRequest();
            req.setDomain(domain);
            req.setOffset(1L);
            //限制数量，当前Limit最大支持3000。默认值为100
            req.setLimit(3000L);
            // 返回的resp是一个DescribeRecordListResponse的实例，与请求对象对应
            DescribeRecordListResponse resp = client.DescribeRecordList(req);
            // 输出json格式的字符串回包
            //System.out.println(DescribeRecordListResponse.toJsonString(resp));
            return R.ok().put("data",resp.getRecordList());
        } catch (TencentCloudSDKException e) {
            eMsg=e.getMessage();
            e.printStackTrace();
        } catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R GetRecordByInfo(String domain,String appId,String appKey,String top, String type, String line){
        String eMsg="";
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            //Credential cred = new Credential("SecretId", "SecretKey");
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DescribeRecordListRequest req = new DescribeRecordListRequest();
            req.setDomain(domain);
            req.setSubdomain(top);
            req.setRecordType(type);
            if(StringUtils.isNotBlank(line)){
                req.setRecordLine(line);
            }
            // 返回的resp是一个DescribeRecordListResponse的实例，与请求对象对应
            DescribeRecordListResponse resp = client.DescribeRecordList(req);
            // return  resp.getRecordList();
            return R.ok().put("data",resp.getRecordList());
            // 输出json格式的字符串回包
            //System.out.println(DescribeRecordListResponse.toJsonString(resp));
        } catch (TencentCloudSDKException e) {
            eMsg=e.getMessage();
            e.printStackTrace();
        } catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static  R addRecord(String domain,String appId,String appKey,String top, String recordType, String line, String value, String ttl){
        String eMsg="";
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            CreateRecordRequest req = new CreateRecordRequest();
            req.setDomain(domain);
            req.setSubDomain(top);
            req.setRecordType(recordType);
            if(StringUtils.isNotBlank(line)){
                req.setRecordLine(line);
            }else {
                req.setRecordLine("默认");
            }

            req.setValue(value);
            req.setTTL(Long.parseLong(ttl));
            // 返回的resp是一个CreateRecordResponse的实例，与请求对象对应
            CreateRecordResponse resp = client.CreateRecord(req);
            // 输出json格式的字符串回包
            //System.out.println(CreateRecordResponse.toJsonString(resp));
            return R.ok().put("data", resp.getRecordId());
        } catch (TencentCloudSDKException e) {
            eMsg=e.getMessage();
            e.printStackTrace();
        } catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R removeRecord(String domain,String appId,String appKey,String recordId){
        String eMsg="";
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteRecordRequest req = new DeleteRecordRequest();
            req.setDomain(domain);
            req.setRecordId(Long.parseLong(recordId));
            // 返回的resp是一个DeleteRecordResponse的实例，与请求对象对应
            DeleteRecordResponse resp = client.DeleteRecord(req);
            // 输出json格式的字符串回包
            //System.out.println(DeleteRecordResponse.toJsonString(resp));
            return R.ok().put("data",resp.getRequestId());
        } catch (TencentCloudSDKException e) {
            eMsg=e.getMessage();
            System.out.println(e.toString());
        } catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }

    public static R modifyRecord(String domain, String appId, String appKey, String recordId, String top, String recordType, String line, String value, String ttl){
        String eMsg="";
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户secretId，secretKey,此处还需注意密钥对的保密
            // 密钥可前往https://console.cloud.tencent.com/cam/capi网站进行获取
            //Credential cred = new Credential("SecretId", "SecretKey");
            Credential cred = new Credential(appId, appKey);
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(endPoint);
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            DnspodClient client = new DnspodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            ModifyRecordRequest req = new ModifyRecordRequest();
            req.setDomain(domain);
            req.setSubDomain(top);
            req.setRecordType(recordType);
            req.setRecordLine(line);
            req.setValue(value);
            req.setTTL(Long.parseLong(ttl));
            req.setRecordId(Long.parseLong(recordId));
            // 返回的resp是一个ModifyRecordResponse的实例，与请求对象对应
            ModifyRecordResponse resp = client.ModifyRecord(req);
            // 输出json格式的字符串回包
            return R.ok().put("data",resp.getRecordId()).put("res",resp);
            //System.out.println(ModifyRecordResponse.toJsonString(resp));
        }catch (Exception e){
            eMsg=e.getMessage();
            e.printStackTrace();
        }
        return R.error(eMsg);
    }


}
