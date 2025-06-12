package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.R;
import io.ants.modules.sys.dao.TbCdnPublicMutAttrDao;
import io.ants.modules.sys.entity.TbCdnPublicMutAttrEntity;
import io.ants.modules.sys.enums.CommandEnum;
import io.ants.modules.sys.enums.PublicEnum;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.CdnPublicAttrService;
import io.ants.modules.sys.vo.ElkServerVo;
import io.ants.modules.sys.vo.SysWafRuleVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CdnPublicAttrServiceImpl implements CdnPublicAttrService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private TbCdnPublicMutAttrDao publicMutAttrDao;

    @Autowired
    private CdnMakeFileService cdnMakeFileService;

    @Override
    public R getPubKeyDetail(Map map) {
        String key=null;
        String parentId=null;
        if (map.containsKey("key")){
            key=map.get("key").toString();
        }
        if (map.containsKey("parentId")){
            parentId=map.get("parentId").toString();
        }
        if(StringUtils.isBlank(key)){
            return R.error(" key is null ");
        }
        if(null== PublicEnum.getObjByName(key)){
            return R.error(key +" is unknown");
        }
        if("m_text".equals(PublicEnum.getObjByName(key).getType()) || "m_object".equals(PublicEnum.getObjByName(key).getType())){
            List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",key).orderByDesc("weight").orderByDesc("weight").orderByDesc("id"));
            return R.ok().put("data",list);
        }else if("c_m_text".equals(PublicEnum.getObjByName(key).getType())){
            if(StringUtils.isBlank(parentId)){
                return R.error(" parentId is null ");
            }
            List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("parent_id",parentId).eq("pkey",key).orderByDesc("weight"));
            return R.ok().put("data",list);
        }else{
            TbCdnPublicMutAttrEntity attr=publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                    .eq("pkey",key).
                    last("limit 1")
            );
            return R.ok().put("data",attr);
        }
    }

    private void deleteEtcWaf(String id){
        String path1="/home/local/nginx/conf/etc/reg_"+id+"_";
        String path2="/home/local/nginx/conf/etc/rule_"+id+"_";
        String[] paths={path1,path2};
        for (String path:paths){
            Map map=new HashMap(2);
            map.put(PushTypeEnum.COMMAND_DELETE_DIR.getName(), path);
            cdnMakeFileService.pushByInputInfo(map);
        }
    }

    private void pkeyEnumPushToNode(String key){
        if (null==PublicEnum.getObjGroupByName(key)){
            return;
        }
        Map<String,String> pushMap=new HashMap<>(8);
        String group=PublicEnum.getObjGroupByName(key);
        switch (group){
            case "nginx_conf":
                pushMap.put(PushTypeEnum.PUBLIC_NGINX_CONF.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case  "cache_conf" :
                pushMap.put(PushTypeEnum.PUBLIC_CACHE_CONF.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "cert_conf":
            case "http_conf":
            case "http_ants_conf":
                pushMap.put(PushTypeEnum.PUBLIC_HTTP_CONF.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case  "http_waf_conf" :
                pushMap.put(PushTypeEnum.PUBLIC_HTTP_DEFAULT_WAF.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "waf_verify_template":
            case  "error_page" :
                pushMap.put(PushTypeEnum.PUBLIC_PUB_ERR_PAGE_CONF.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "web_pub_waf":
            case "web_precise_waf":
                pushMap.put(PushTypeEnum.PUBLIC_PUB_WAF_SELECT.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "net_waf":
                //pushMap.put(PushTypeEnum.PUBLIC_NFT_WAF.getName(),"null");
                //cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "wb_ip_host":
                pushMap.put(PushTypeEnum.IP_TRIE.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "vhost":
                pushMap.put(PushTypeEnum.PUBLIC_PUSH_ETC_VHOST_CONF.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "common":
                pushMap.put(PushTypeEnum.NODE_SYS_WS_SPECIAL_PORTS_RESET.getName(),"null");
                cdnMakeFileService.pushByInputInfo(pushMap);
                break;
            case "":
                break;
            default:
                logger.error("ERROR:pkey Enum Push To Node fail,unknown group:"+group);
                break;
        }

    }

    @Override
    public R statusChange(Map<String, String> params) {
        if(params.containsKey("id") && params.containsKey("status")){
            UpdateWrapper<TbCdnPublicMutAttrEntity> updateWrapper=new UpdateWrapper<>();
            updateWrapper.eq("id",params.get("id")).set("status",params.get("status")).set("update_time",new Date());
            publicMutAttrDao.update(null,updateWrapper);
            TbCdnPublicMutAttrEntity pubAttr= publicMutAttrDao.selectById(params.get("id"));
            if(pubAttr.getPkey().equals(PublicEnum.WEB_RULE_PRECISE.getName())  &&  "0".equals(params.get("status"))){
                this.deleteEtcWaf(params.get("id"));
            }else{
                this.pkeyEnumPushToNode(pubAttr.getPkey());
            }
            return R.ok().put("data",pubAttr);
        }else if(params.containsKey("key") && params.containsKey("status")){
            if(null!=PublicEnum.getObjTypeByName(params.get("key"))   ){
                String type=PublicEnum.getObjByName(params.get("key")).getType();
                String [] cannot_together_change={"m_text","c_m_text"};
                if(Arrays.binarySearch(cannot_together_change,type)>0){
                    return R.error("此类型不可直接修改状态！");
                }
            }
            UpdateWrapper<TbCdnPublicMutAttrEntity> updateWrapper=new UpdateWrapper<>();
            updateWrapper.eq("pkey",params.get("key")).set("status",params.get("status")).set("update_time",new Date());
            if(params.get("key").equals(PublicEnum.WEB_RULE_PRECISE.getName())  &&  "0".equals(params.get("status"))){
                this.deleteEtcWaf(params.get("id"));
            }else {
                publicMutAttrDao.update(null,updateWrapper);
                this.pkeyEnumPushToNode(params.get("key"));
            }
            return R.ok().put("data",publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",params.get("key"))));
        }
        return R.error("修改失败！");
    }

    @Override
    public R pubAttrSave(Map<String, Object> map) {
        if(null==map ||  0==map.size()){
            return R.error("没有提交数据！");
        }
        for (String key:map.keySet()){
            PublicEnum targetPubObj=PublicEnum.getObjByName(key);
            if (null==targetPubObj){
                logger.error("["+key+"] is error type");
                continue;
            }
            String type=targetPubObj.getType();
            Object value=map.get(key);
            this.checkKeyValueRule(key,value);
            this.typeEnumSave(type,key,value);
            this.pkeyEnumPushToNode(key);
            cdnMakeFileService.deleteCacheByKey(key);
        }
        return R.ok();
    }

    @Override
    public R changeWeight(Map<String, String> params) {
        if(!params.containsKey("id")||!params.containsKey("opMode")){
            return R.error("参数不完整【id】【opMode】");
        }
        boolean op=false;
        Integer attrId=Integer.parseInt(params.get("id"));
        int opMode=Integer.parseInt(params.get("opMode"));
        TbCdnPublicMutAttrEntity pubAttr=publicMutAttrDao.selectById(attrId);
        if (null==pubAttr){
            return R.error("不存在的属性ID");
        }
        String pkey= pubAttr.getPkey();
        //Integer parentId=pub.getParentId();
        PublicEnum type_enum= PublicEnum.getObjByName(pkey);
        if (null==type_enum){
            return R.error("错误的[pkey]");
        }
        String type=type_enum.getType();
        if(!"mm_text".equals(type) && !"c_m_text".equals(type) && !"m_text".equals(type)) {
            return R.error("错误的["+pkey+"]类型");
        }
        List<TbCdnPublicMutAttrEntity> listMM = publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>()
                .eq(null!=pubAttr.getParentId(),"parent_id",pubAttr.getParentId())
                .eq("pkey", pubAttr.getPkey())
                .orderByDesc("weight")
                .select("id"));
        List<Integer> id_list = listMM.stream().map(t -> t.getId()).collect(Collectors.toList());
        List<Integer> f_id_list=new ArrayList<>();
        f_id_list.addAll(id_list);
        if(id_list.size()>0){
            for (int i = 0; i <id_list.size() ; i++) {
                if(id_list.get(i).equals(attrId)){
                    if(1==opMode && i>0 ){
                        //上移
                        op=true;
                        Integer i_p=id_list.get(i-1);
                        f_id_list.set(i,i_p);
                        f_id_list.set(i-1,attrId);
                    }else if(-1==opMode && i<id_list.size()-1){
                        //下移
                        op=true;
                        Integer i_n=id_list.get(i+1);
                        f_id_list.set(i,i_n);
                        f_id_list.set(i+1,attrId);
                    }else if(0==opMode && 0!=i){
                        //致顶
                        op=true;
                        List<Integer> buf_id_list=new ArrayList<>();
                        buf_id_list.addAll(f_id_list);
                        buf_id_list.remove(attrId);
                        f_id_list.clear();
                        f_id_list.add(attrId);
                        f_id_list.addAll(buf_id_list);
                    }
                }
            }
            if (op){
                for (int i = 0; i < f_id_list.size(); i++) {
                    UpdateWrapper<TbCdnPublicMutAttrEntity> updateWrapper=new UpdateWrapper<>();
                    updateWrapper.eq("id",f_id_list.get(i)).set("weight",f_id_list.size()-i);
                    publicMutAttrDao.update(null,updateWrapper);
                }
            }
        }
        this.pkeyEnumPushToNode(pkey);
        return R.ok().put("status",op);
    }

    @Override
    public R deletePubAttr(Map<String, String> map) {
        if(!map.containsKey("ids")){
            return R.error("ids is null");
        }
        String ids=map.get("ids").toString();
        for (String id:ids.split(",")){
            TbCdnPublicMutAttrEntity pubAttr=publicMutAttrDao.selectById(id);
            if (null!=pubAttr){
                this.pkeyEnumPushToNode(pubAttr.getPkey());
            }
            publicMutAttrDao.deleteById(id);
        }
        return R.ok();
    }

    @Override
    public R pubAttrList(Map<String, String> map) {
        if(!map.containsKey("groups")){
            return R.error("groups 不能为空！");
        }
        /*
        * type: 'text',
         value: '64k',
        tips: '代理请求缓存大小（默认64k）'
        * */
        String groups=map.get("groups");
        Map result=new HashMap();
        for (String group:groups.split(",")){
            Map attr_map=PublicEnum.getAllByGroupName(group);
            for(Object key:attr_map.keySet()){
                String pkey=key.toString();
                PublicEnum pub_attr_enum_obj=PublicEnum.getObjByName(pkey);
                if(null==pub_attr_enum_obj){
                    continue;
                }
                if("m_text".equals(pub_attr_enum_obj.getType())){
                    List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",pkey).orderByDesc("weight"));
                    result.put(pkey,list);
                }else if("c_m_text".equals(pub_attr_enum_obj.getType())){
                    if(!map.containsKey("parentId")){
                        continue;
                    }
                    String parentId=map.get("parentId").toString();
                    List<TbCdnPublicMutAttrEntity> list=publicMutAttrDao.selectList(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("parent_id",parentId).eq("pkey",pkey));
                    result.put(pkey,list);
                }else{
                    TbCdnPublicMutAttrEntity attr=publicMutAttrDao.selectOne(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",pkey).last("limit 1").orderByDesc("weight"));
                    if(null!=attr){
                        JSONObject obj= DataTypeConversionUtil.entity2jsonObj(attr);
                        obj.put("type",pub_attr_enum_obj.getType());
                        obj.put("default",pub_attr_enum_obj.getDefaultValue());
                        obj.put("tips",pub_attr_enum_obj.getNotes());
                        result.put(pkey,obj);
                    }else {
                        JSONObject obj=new JSONObject();
                        obj.put("id",0);
                        obj.put("type",pub_attr_enum_obj.getType());
                        obj.put("default",pub_attr_enum_obj.getDefaultValue());
                        obj.put("tips",pub_attr_enum_obj.getNotes());
                        obj.put("pvalue",pub_attr_enum_obj.getDefaultValue());
                        result.put(pkey,obj);
                    }
                }

            }
        }
        return R.ok().put("data",result);
    }

    private void checkKeyValueRule(String key, Object value){
        cdnMakeFileService.cdnPubCheckKeyValueRule(key,value);
    }


    private void typeEnumSave(String type,String key,Object value){
        switch (type){
            case "bool":
            case "int":
            case "text":
                if(0==publicMutAttrDao.selectCount(new QueryWrapper<TbCdnPublicMutAttrEntity>().eq("pkey",key))){
                    TbCdnPublicMutAttrEntity publicMutAttrEntity=new TbCdnPublicMutAttrEntity();
                    publicMutAttrEntity.setPkey(key);
                    publicMutAttrEntity.setPvalue(value.toString());
                    publicMutAttrDao.insert(publicMutAttrEntity);
                }else {
                    UpdateWrapper<TbCdnPublicMutAttrEntity> updateWrapper=new UpdateWrapper<>();
                    updateWrapper.eq("pkey",key).set("pvalue",value).set("update_time",new Date());
                    publicMutAttrDao.update(null,updateWrapper);
                }
                break;
            case "m_text":
                List<LinkedHashMap> m_list=(List<LinkedHashMap>) value;
                if(m_list.size()>0){
                    for (int i = 0; i <m_list.size() ; i++) {
                        LinkedHashMap linkedHashMap=m_list.get(i);
                        if(!linkedHashMap.containsKey("value")){
                            logger.warn(linkedHashMap.toString());
                            continue;
                        }
                        String pvalue=linkedHashMap.get("value").toString();
                        if(linkedHashMap.containsKey("id") && !"0".equals(linkedHashMap.get("id").toString())){
                            //更新
                            UpdateWrapper<TbCdnPublicMutAttrEntity> updateWrapper=new UpdateWrapper<>();
                            updateWrapper.eq("id",linkedHashMap.get("id").toString()).eq("pkey",key).set("pvalue",pvalue).set("update_time",new Date());
                            publicMutAttrDao.update(null,updateWrapper);
                        }else{
                            //insert
                            TbCdnPublicMutAttrEntity publicMutAttrEntity=new TbCdnPublicMutAttrEntity();
                            publicMutAttrEntity.setPkey(key);
                            publicMutAttrEntity.setPvalue(pvalue);
                            publicMutAttrDao.insert(publicMutAttrEntity);
                        }
                    }
                }
                break;
            case "m_object":
                List<LinkedHashMap> o_list=(List<LinkedHashMap>) value;
                if(null!=o_list &&  o_list.size()>0){
                    for (int i = 0; i <o_list.size() ; i++) {
                        LinkedHashMap linkedHashMap=o_list.get(i);
                        JSONObject object= DataTypeConversionUtil.map2json(linkedHashMap);
                        if(object.containsKey("id") && !"0".equals(object.get("id").toString())){
                            //更新
                            String id=object.get("id").toString();
                            object.remove("id");
                            UpdateWrapper<TbCdnPublicMutAttrEntity> updateWrapper=new UpdateWrapper<>();
                            updateWrapper.eq("id",id).eq("pkey",key).set("pvalue",object.toJSONString()).set("update_time",new Date());
                            publicMutAttrDao.update(null,updateWrapper);
                        }else{
                            //insert
                            object.remove("id");
                            TbCdnPublicMutAttrEntity publicMutAttrEntity=new TbCdnPublicMutAttrEntity();
                            publicMutAttrEntity.setPkey(key);
                            publicMutAttrEntity.setPvalue(object.toJSONString());
                            publicMutAttrDao.insert(publicMutAttrEntity);
                        }
                    }
                }
                break;
            case "c_m_text":
                List<LinkedHashMap> c_m_list=(List<LinkedHashMap>) value;
                if(c_m_list.size()>0){
                    for (int i = 0; i <c_m_list.size() ; i++) {
                        LinkedHashMap linkedHashMap=c_m_list.get(i);
                        if(!linkedHashMap.containsKey("value")){
                            continue;
                        }
                        if(!linkedHashMap.containsKey("parentId")){
                            continue;
                        }
                        String pvalue=linkedHashMap.get("value").toString();
                        if (key.equals(PublicEnum.WEB_RULE_PRECISE_DETAIL.getName())){
                            SysWafRuleVo sysWafRuleVo=DataTypeConversionUtil.string2Entity(pvalue,SysWafRuleVo.class);
                            if (null==sysWafRuleVo){
                                continue;
                            }
                            if (StringUtils.isNotBlank(sysWafRuleVo.getBotCheckHttpStatusCode())){
                                String pid=linkedHashMap.get("parentId").toString();
                                TbCdnPublicMutAttrEntity pEnt=publicMutAttrDao.selectById(pid);
                                if (null!=pEnt && StringUtils.isNotBlank(pEnt.getPvalue())){
                                    JSONObject jsonObject=DataTypeConversionUtil.string2Json(pEnt.getPvalue());
                                    jsonObject.put("botCheckHttpStatusCode",Integer.valueOf(sysWafRuleVo.getBotCheckHttpStatusCode()));
                                    publicMutAttrDao.update(null,new UpdateWrapper<TbCdnPublicMutAttrEntity>()
                                            .eq("pkey",PublicEnum.WEB_RULE_PRECISE.getName())
                                            .eq("id",pid)
                                            .set("pvalue",jsonObject.toJSONString())
                                    );
                                }
                            }
                        }
                        if(linkedHashMap.containsKey("id") && !"0".equals(linkedHashMap.get("id").toString())){
                            //更新
                            UpdateWrapper<TbCdnPublicMutAttrEntity> updateWrapper=new UpdateWrapper<>();
                            updateWrapper.eq("id",linkedHashMap.get("id").toString()).eq("pkey",key).set("pvalue",pvalue).set("update_time",new Date());
                            publicMutAttrDao.update(null,updateWrapper);
                        }else {
                            TbCdnPublicMutAttrEntity publicMutAttrEntity=new TbCdnPublicMutAttrEntity();
                            publicMutAttrEntity.setParentId(Integer.parseInt(linkedHashMap.get("parentId").toString()) );
                            publicMutAttrEntity.setPkey(key);
                            publicMutAttrEntity.setPvalue(pvalue);
                            publicMutAttrDao.insert(publicMutAttrEntity);
                        }
                    }
                }
                break;
            case "c_int":
                break;
            default:
                logger.error(key+" is unknown type !");
                break;
        }
        if (key.equals(PublicEnum.ELK_CONFIG.getName())){
            ElkServerVo elkServerVo=DataTypeConversionUtil.string2Entity(value.toString(),ElkServerVo.class);
            cdnMakeFileService.setElkConfig2Redis(elkServerVo);
            Map<String,String> pushMap=new HashMap<>(2);
            pushMap.put(PushTypeEnum.COMMAND.getName(), CommandEnum.INIT_ELK.getId().toString());
            cdnMakeFileService.pushByInputInfo(pushMap);
        }
    }

}
