package io.ants.modules.sys.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.annotation.SysLog;
import io.ants.common.other.QuerySysAuth;
import io.ants.common.utils.*;
import io.ants.modules.sys.dao.CdnClientDao;
import io.ants.modules.sys.dao.SysConfigDao;
import io.ants.modules.sys.dao.TableDao;
import io.ants.modules.sys.entity.CdnClientEntity;
import io.ants.modules.sys.entity.SysConfigEntity;
import io.ants.modules.sys.enums.CdnVersionEnum;
import io.ants.modules.sys.enums.RedisStreamType;
import io.ants.modules.sys.vo.CdnUpdateContrastVo;
import io.ants.modules.sys.vo.CdnUpdateItemVo;
import io.ants.modules.sys.vo.CdnUpdateQueryVo;
import io.ants.modules.sys.vo.NodeVersionVo;
import io.ants.modules.utils.ConfigConstantEnum;
import io.ants.modules.utils.config.WebDirConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@RestController
@RequestMapping("/sys/version/")
public class CdnVersionController extends AbstractController {




    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private SysConfigDao sysConfigDao;
    @Resource
    TableDao tableDao;






    /**
     * java JAR hash
     * @param contrastVo
     * @return
     */
    private  void controllerVersion( CdnUpdateContrastVo contrastVo){
        try{
            Properties props =System.getProperties();
            String jar_path= props.getProperty("java.class.path");
            if(null==jar_path){return ;}
            File jar_file = new File(jar_path);
            String hash= HashUtils.md5OfFile(jar_file);
            contrastVo.setLocal(hash);
            Map<String,Object> r_map= QuerySysAuth.queryCdnModelNewestVersionData(contrastVo.getKey(),hash);
            CdnUpdateQueryVo queryVo= DataTypeConversionUtil.map2entity(r_map, CdnUpdateQueryVo.class);
            contrastVo.setChecktime(queryVo.getChecktime());
            contrastVo.setLocal_version_date(queryVo.getLocal_version_date());
            contrastVo.setRemote(queryVo.getRemote());
            contrastVo.setRemote_version_date(queryVo.getRemote_version_date());
            //System.out.println(data1);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private WebDirConfig getSysDir(){
        SysConfigEntity config=sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().eq("param_key", ConfigConstantEnum.WEB_DIR_CONF.getConfKey()).last("limit 1"));
        if (null==config){
            return null;
        }
        if(StringUtils.isBlank(config.getParamValue())){
            return null;
        }
        String v=config.getParamValue();
        return DataTypeConversionUtil.string2Entity(v, WebDirConfig.class);
    }


    /**
     *前台 HASH
     * @param request
     * @param contrastVo
     */
    private void  webUserVersion(HttpServletRequest request, CdnUpdateContrastVo contrastVo){
        try{
            WebDirConfig webConf=getSysDir();
            String hash;
            if(null==webConf){
                String user_index_url=request.getServerName()+"/users/index.html";
                String user_index= HttpRequest.curlHttpGet(user_index_url);
                hash=HashUtils.md5ofString(user_index);
            }else {
                // /www/wwwroot/www.cdn.com/manager/
                // /www/wwwroot/www.cdn.com/users/
                String path=String.format("%s/index.html",webConf.getUserDir());
                File file=new File(path);
                hash=HashUtils.md5OfFile(file);
            }
            contrastVo.setLocal(hash);
            Map<String,Object> r_map= QuerySysAuth.queryCdnModelNewestVersionData(contrastVo.getKey(),hash);
            CdnUpdateQueryVo queryVo=DataTypeConversionUtil.map2entity(r_map, CdnUpdateQueryVo.class);
            contrastVo.setChecktime(queryVo.getChecktime());
            contrastVo.setLocal_version_date(queryVo.getLocal_version_date());
            contrastVo.setRemote(queryVo.getRemote());
            contrastVo.setRemote_version_date(queryVo.getRemote_version_date());
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 后台HASH
     * @param request
     * @param contrastVo
     * @return
     */
    private  void webManagerVersion(HttpServletRequest request, CdnUpdateContrastVo contrastVo){
        try{

            WebDirConfig webConf=getSysDir();
            String hash;
            if(null==webConf){
                String manager_index_url=request.getServerName()+"/manager/index.html";
                //System.out.println(manager_index_url);
                String manager_index=HttpRequest.curlHttpGet(manager_index_url);
                hash=HashUtils.md5ofString(manager_index);
            }else {
                // /www/wwwroot/www.cdn.com/manager/
                // /www/wwwroot/www.cdn.com/users/
                String path=String.format("%s/index.html",webConf.getAdminDir());
                File file=new File(path);
                hash=HashUtils.md5OfFile(file);
            }
            contrastVo.setLocal(hash);
            Map<String,Object> r_map= QuerySysAuth.queryCdnModelNewestVersionData(contrastVo.getKey(),hash);
            CdnUpdateQueryVo queryVo=DataTypeConversionUtil.map2entity(r_map, CdnUpdateQueryVo.class);
            contrastVo.setChecktime(queryVo.getChecktime());
            contrastVo.setLocal_version_date(queryVo.getLocal_version_date());
            contrastVo.setRemote(queryVo.getRemote());
            contrastVo.setRemote_version_date(queryVo.getRemote_version_date());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 数据库hash
     * @param request
     * @param contrastVo
     * @return
     */
    private void  dbVersion(HttpServletRequest request,  CdnUpdateContrastVo contrastVo){
        //String[] k_list={"TABLE_CATALOG","IS_NULLABLE","EXTRA","COLUMN_NAME","COLUMN_KEY","NUMERIC_PRECISION","NUMERIC_SCALE","COLUMN_TYPE","ORDINAL_POSITION","DATA_TYPE"};
        String[] k_list={"COLUMN_NAME","COLUMN_TYPE","EXTRA"};
        try{
            JSONObject final_table_json = new JSONObject(new LinkedHashMap());
            List<Map> list_table_map=  tableDao.listTable();
            for (int i = 0; i <list_table_map.size() ; i++) {
                Map m=list_table_map.get(i);
                if(m.containsKey("TABLE_NAME") ){
                    String tableName=m.get("TABLE_NAME").toString();//dns_ip_correct
                    List<Map> listt_column_map_ls= tableDao.listTableColumn(tableName);
                    JSONArray tab_attr_array=new JSONArray();
                    for (Map l_column_map:listt_column_map_ls){
                        if (l_column_map.containsKey("COLUMN_NAME")){
                            JSONObject attrJson=new JSONObject(new LinkedHashMap());
                            for (String k_:k_list){
                                if(l_column_map.containsKey(k_)){
                                    attrJson.put(k_,l_column_map.get(k_).toString());
                                }
                            }
                            tab_attr_array.add(attrJson);
                        }
                    }
                    final_table_json.put(tableName,tab_attr_array);
                }
            }

            //System.out.println();
            //logger.debug(StaticVariableUtils.db_json_str);
            //logger.warn(final_table_json.toJSONString());
            String current_db_hash=HashUtils.md5ofString(final_table_json.toJSONString());
            contrastVo.setLocal(current_db_hash);

            Map<String,Object> r_map= QuerySysAuth.queryCdnModelNewestVersionData(contrastVo.getKey(),current_db_hash);
            CdnUpdateQueryVo queryVo=DataTypeConversionUtil.map2entity(r_map, CdnUpdateQueryVo.class);
            contrastVo.setChecktime(queryVo.getChecktime());
            contrastVo.setLocal_version_date(queryVo.getLocal_version_date());
            contrastVo.setRemote(queryVo.getRemote());
            contrastVo.setRemote_version_date(queryVo.getRemote_version_date());

            if (StringUtils.isNotBlank(contrastVo.getLocal())  && StringUtils.isNotBlank(contrastVo.getRemote()) && !contrastVo.getLocal().equals(contrastVo.getRemote())){
                StaticVariableUtils.db_sync_status=false;
            }
            else {
                StaticVariableUtils.db_sync_status=true;
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 模板HASH
     * @param request
     * @param goods
     * @return
     */
    private Map dbModelVariableVersion(HttpServletRequest request, String goods){
        Map<String,Object>result=new HashMap<>();
        result.put("name","配置模板");
        try{
            List<LinkedHashMap<String,Object>> l1= tableDao.select_sql("SELECT object_mode,absolute_path_model,file_model,version,weight,nginx_check FROM cdn_file_model WHERE status=1");
            List<LinkedHashMap<String,Object>> l2=tableDao.select_sql("SELECT variable_mode,variable_name,variable_value,version FROM cdn_file_variable WHERE status = 1");
            String[]  c1list={ "object_mode","absolute_path_model","file_model","version","weight","nginx_check"};
            String[]  c2list={ "variable_mode","variable_name","variable_value","version"};
            StringBuffer l1Str=new StringBuffer();
            StringBuffer l2Str=new StringBuffer();
            for (Map m:l1){
              for (String c:c1list) {
                  if(m.containsKey(c)){
                      if (null==m.get(c)){
                          continue;
                      }
                      String value=m.get(c).toString();
                      if("true".equals(value) || "false".equals(value)){
                          value= "true".equals(value)?"1":"0";
                      }
                      //logger.debug(value);
                      l1Str.append(HashUtils.md5ofString(value));
                      l1Str.append(",");
                  }
               }
            }
            for (Map m:l2){
                for (String c:c2list) {
                    if(m.containsKey(c)){
                        if (null==m.get(c)){
                            continue;
                        }
                        String value=m.get(c).toString();
                        if("true".equals(value) || "false".equals(value)){
                            value= "true".equals(value)?"1":"0";
                        }
                        //logger.debug(value);
                        l2Str.append(HashUtils.md5ofString(value));
                        l2Str.append(",");
                    }
                }
            }
           //logger.debug("f-model->:"+ l1Str);
           //logger.debug("v-model->:"+ l2Str);
            String f_h1=HashUtils.md5ofString(l1Str.toString()) ;
            String f_h2=HashUtils.md5ofString(l2Str.toString()) ;
            String f_local_hash=HashUtils.md5ofString(f_h1+f_h2);
            result.put("checktime",System.currentTimeMillis());
            result.put("local",f_local_hash);

            Map<String,Object> r_map= QuerySysAuth.queryCdnModelNewestVersionData(goods,f_local_hash);
            result.putAll(r_map);
            return result;
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }




    /**
     * 节点守护程序版本号
     * @param request
     * @param contrastVo
     * @return
     */
    private  void antsAgentVersion(HttpServletRequest request, CdnUpdateContrastVo contrastVo){
        List<String> verList=new ArrayList<>();
        JSONObject nodeMap=new JSONObject();
        List<CdnClientEntity>list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>().eq("client_type",1).select("client_ip"));
        for (CdnClientEntity client:list){
           String v= redisUtils.get(String.format("version_%s",client.getClientIp()));
           if (StringUtils.isNotBlank(v)){
               NodeVersionVo vo=NodeVersionVo.getVersionObj(v);
               if(!verList.contains(vo.getAgentVersion())){
                   verList.add(vo.getAgentVersion());
               }
               nodeMap.put(client.getClientIp(),vo.getAgentVersion());
           }
        }
        String vLs=String.join(",",verList);
        contrastVo.setLocal(vLs);
        contrastVo.setDetail(nodeMap);

        Map<String,Object> r_map= QuerySysAuth.queryCdnModelNewestVersionData(contrastVo.getKey(),vLs);
        CdnUpdateQueryVo queryVo=DataTypeConversionUtil.map2entity(r_map, CdnUpdateQueryVo.class);
        contrastVo.setChecktime(queryVo.getChecktime());
        contrastVo.setLocal_version_date(queryVo.getLocal_version_date());
        contrastVo.setRemote(queryVo.getRemote());
        contrastVo.setRemote_version_date(queryVo.getRemote_version_date());
    }


    /**
     * 节点NGINX-waf 版本号
     * @param request
     * @param infoVo
     * @return
     */
    private void nginxVersion(HttpServletRequest request, CdnUpdateContrastVo infoVo){
        List<String> verList=new ArrayList<>();
        JSONObject nodeMap=new JSONObject();
        List<CdnClientEntity> client_list=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",1)
        );
        for (CdnClientEntity client:client_list){
            if(null==client){
                continue;
            }
            //获取本地最新version
            this.getNodeAgentVersionByRedis(client);
            if(null!=client.getVersion()){
                if(!verList.contains(client.getVersion())){
                    verList.add(client.getVersion());
                }
            }
            nodeMap.put(client.getClientIp(),client.getVersion());
        }
        infoVo.setDetail(nodeMap);

        String vLs=String.join(",",verList);
        infoVo.setLocal(vLs);

        Map<String,Object> r_map= QuerySysAuth.queryCdnModelNewestVersionData(infoVo.getKey(),vLs);
        CdnUpdateQueryVo queryVo=DataTypeConversionUtil.map2entity(r_map, CdnUpdateQueryVo.class);
        infoVo.setChecktime(queryVo.getChecktime());
        infoVo.setLocal_version_date(queryVo.getLocal_version_date());
        infoVo.setRemote(queryVo.getRemote());
        infoVo.setRemote_version_date(queryVo.getRemote_version_date());
    }

    private void test(){
        String tableName="tb_rewrite";
        String create_sql="CREATE TABLE IF NOT EXISTS `"+tableName+"` ( `id` INT UNSIGNED AUTO_INCREMENT,PRIMARY KEY ( `id` ) ) ENGINE=InnoDB DEFAULT CHARSET=utf8 ";
       //logger.debug(create_sql);
        tableDao.update_sql(create_sql);
    }

    @GetMapping("/GetUpdateStatus")
    public R GetUpdateStatus(){
        this.recordInfo(redisUtils);
        Long nowTm=System.currentTimeMillis();
        if(StaticVariableUtils.is_in_update && 0L!=StaticVariableUtils.up_time && Math.abs(nowTm-StaticVariableUtils.up_time)>600*1000){
            return R.ok().put("Updating",0).put("type",StaticVariableUtils.up_name).put("msg","版本更新任务后台执行超时");
        }
        else if(StaticVariableUtils.is_in_update && null!=StaticVariableUtils.up_time  && Math.abs(nowTm-StaticVariableUtils.up_time)<=600*1000  ){
            return R.ok().put("Updating",1).put("type",StaticVariableUtils.up_name).put("msg","版本更新任务后台执行中...");
        }
        return R.ok().put("Updating",0).put("msg","版本更新任务后台执行完毕！");
    }

    private CdnUpdateContrastVo getVersionKeyValue(String goods){
        HttpServletRequest request=  HttpContextUtils.getHttpServletRequest();
        CdnUpdateContrastVo contrastVo=new CdnUpdateContrastVo();
        contrastVo.setKey(goods);
        contrastVo.updateName();
        switch (CdnVersionEnum.getEnumItemByKey(goods)){
            case M_JAVA_JAR:
                this.controllerVersion(contrastVo);
                break;
            case M_WEB_USER:
                this.webUserVersion(request,contrastVo);
                break;
            case M_WEB_MANAGER:
                this.webManagerVersion(request,contrastVo);
                break;
            case M_MYSQL_DB:
                this.dbVersion(request,contrastVo);
                break;
            case M_NODE_AGENT:
                this.antsAgentVersion(request,contrastVo);
                break;
            case M_NODE_NGINX:
                this.nginxVersion(request,contrastVo);
                break;
            default:
                break;
        }
        return  contrastVo;
    }

    @GetMapping("/GetVersionData")
    public R getVersion(@RequestParam(required = false,defaultValue = "") String type ){
        this.recordInfo(redisUtils);
        Map<String,String> cdnUnitMap=CdnVersionEnum.getAllKN();
        Map<String,Object>map=new HashMap<>(cdnUnitMap.size());
        List<String> goodsArray=new ArrayList<>(cdnUnitMap.size());
        for (String key : cdnUnitMap.keySet()) {
            CdnUpdateContrastVo contrastVo=this.getVersionKeyValue(key);
            map.put(key,contrastVo);
            goodsArray.add(key);
        }
        return R.ok().put("data",map).put("infos",cdnUnitMap).put("goods",goodsArray);
    }

    /**更新JAVA JAR
     * @param download_url
     * @param mk_path
     * @param r_hash2
     * @return
     */
    private List<String> upWebController(String download_url, String mk_path, String r_hash2){
        //:  cd /usr/ants/dns-api
        //:  wget wget download.aaaa.com/antsdns/ants-fast.1646876403.tar.gz
        //:  tar -zxvf ants-fast.1646876403.tar.gz
        //:  /usr/ants/dns-api/dns_start.sh
        StaticVariableUtils.is_in_update=true;
        StaticVariableUtils.up_name=CdnVersionEnum.M_JAVA_JAR.getKey();
        StaticVariableUtils.up_time=System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                String down_path = download_url.trim();

                String mk_dir=mk_path.substring(0,mk_path.lastIndexOf("/"));
                String fileName = down_path.substring(down_path.lastIndexOf("/")+1);

                //1 download file
                String cmd_1=String.format("cd %s && rm -f %s && curl -s -o %s %s",mk_dir,fileName,mk_path+fileName,down_path);
               //logger.debug("cmd1:"+cmd_1);
                ShellUtils.runShell(cmd_1,false);
                try{
                    Thread.sleep(1000*5);
                }catch (Exception e){
                    e.printStackTrace();
                }
                File file = new File(mk_path+fileName);
                if(file.isFile()){
                    String hash2=HashUtils.md5OfFile(file);
                    if(r_hash2.equals(hash2)){
                        //String cmd_2="cd "+mk_dir +  " &&  tar -zxvf "+fileName ;
                        String cmd_2=String.format("cd %s && tar -zxvf %s",mk_dir,fileName);
                        System.out.println("cmd2:"+cmd_2);
                        ShellUtils.runShell(cmd_2 ,false);
                        //                        cmd_2="cd "+mk_dir+"\n";
                        //                        cmd_2+="\n pid=$( ps -ef | grep 'antsxdp.jar' | grep -v grep | awk '{print $2}' ) "+" \n" ;
                        //                        cmd_2+="rm -f "+ fileName  +" \n";
                        //                        cmd_2+="kill $pid \n" ;
                        String cmd3=String.format("cd %s \npid=$( ps -ef | grep 'antsCdn.jar' | grep -v grep | awk '{print $2}' ) \n rm -f %s \n kill $pid",mk_dir,fileName);
                       //logger.debug("cmd3:"+cmd3);
                        String sh_path_1=mk_path+"up1.sh";
                        ShellUtils.createShell(sh_path_1,cmd3);
                        ShellUtils.runnexec(sh_path_1);
                        StaticVariableUtils.is_in_update=false;
                    }else{
                       //logger.debug("up_web_controller hash2 is error");
                    }

                }else {
                   //logger.debug("file is null");
                }
            }
        }).start();
        return null;
    }

    private List<String> upWebView(String download_url, String mk_path){
        //:  cd /www/wwwroot/www.dns.com/users
        //:  wget wget download.aaa.com/antsdns/web_view_xxx.1646876403.tar.gz
        //:  tar -zxvf web_view_xxx.1646876403.tar.gz
        StaticVariableUtils.is_in_update=true;
        StaticVariableUtils.up_name="web_view";
        StaticVariableUtils.up_time=System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String down_path = download_url.trim();
                //down_path.replace("http://","");
                String fileName = down_path.substring(down_path.lastIndexOf("/")+1);
                //String cmd=" cd "+mk_path+" && rm -f "+ fileName +" && "+" curl -o "+mk_path+fileName+"  " +down_path +" && "+"tar -zxvf "+fileName +" && rm -f "+ fileName +" ";
                String cmd=String.format("cd %s && rm -f %s && curl -s -o %s %s && tar -zxvf %s && rm -f %s ",mk_path,fileName,mk_path+fileName,down_path,fileName,fileName);
                System.out.println(cmd);
                List<String> l=  ShellUtils.runShell(cmd,false);
                System.out.println(l);
                StaticVariableUtils.is_in_update=false;
            }
        }).start();
        return null;

    }

    /**
     * 更新DB
     * @param json_file_path
     */
    private void upDbFromJsonFileHandle(String json_file_path){
        JSONObject db_jsonObject=new JSONObject();
        //声明流对象
        FileInputStream fis = null;
        try{
            //创建流对象
            //fis = new FileInputStream("/usr/ants/dns-api/db.1647318809.json");
            fis = new FileInputStream(json_file_path);
            //读取数据，并将读取到的数据存储到数组中
            byte[] data = new byte[1024*1024];
            //当前下标
            int i = 0;
            //读取流中的第一个字节数据，一次读一个字节
            int n = fis.read();
            //依次读取后续的数据
            //未到达流的末尾
            while(n != -1){
                //将有效数据存储到数组中，将已经读取到的数据n强制转换为byte，即取n中的有效数据——最后一个字节
                data[i] = (byte)n;
                //下标增加
                i++;
                //读取下一个字节的数据
                n = fis.read();
            }

            //解析数据
            String s = new String(data,0,i);
            //输出字符串
            //System.out.println(s);
            db_jsonObject=JSONObject.parseObject(s);
            for (Map.Entry<String, Object> entry: db_jsonObject.entrySet()) {
                String tableName = entry.getKey();
                List<Map> l_tb_res=tableDao.listTableOne(tableName);
                if( 0==l_tb_res.size()){
                    logger.info("not exist table:"+tableName);
                    //不存在这个表--创建表
                    String create_sql="CREATE TABLE IF NOT EXISTS `"+tableName+"` ( `id` INT UNSIGNED AUTO_INCREMENT,PRIMARY KEY ( `id` ) ) ENGINE=InnoDB DEFAULT CHARSET=utf8 ";
                    tableDao.update_sql(create_sql);
                }
                JSONArray column_array=db_jsonObject.getJSONArray(tableName);

                //同步远程的
                //logger.debug("同步远程的:"+tableName);
                for (int j = 0; j < column_array.size(); j++) {
                    JSONObject column_json=column_array.getJSONObject(j);
                    if(column_json.containsKey("COLUMN_NAME")   ){
                        String column_name=column_json.getString("COLUMN_NAME");
                        //String sql="select COLUMN_NAME,COLUMN_TYPE,COLUMN_DEFAULT  from information_schema.COLUMNS where TABLE_SCHEMA = (select database()) and TABLE_NAME='"+tableName+"' and COLUMN_NAME='"+column_name+"'";
                        //System.out.println("tableName"+"==="+column_name);
                        List<Map> column_res_list=tableDao.listTableOneColumn(tableName,column_name);
                        //System.out.println(column_res_list);
                        if(column_res_list.size()==1){
                            //本地存在 对比属性 ,属性不同修改
                            Map column_res=column_res_list.get(0);
                            //System.out.println(column_res.toString());
                            final String[] columnAttr={"COLUMN_NAME","COLUMN_TYPE","ORDINAL_POSITION","COLUMN_KEY","EXTRA"};
                            boolean need_update=false;
                            for (String attr:columnAttr){
                                if (column_json.containsKey(attr) && column_res.containsKey(attr)){
                                    if (column_json.get(attr).equals(column_res.get(attr))){
                                       //logger.debug(tableName+" "+column_name+" "+attr+" ==equal");
                                    }else {
                                        logger.error(tableName+" "+column_name+" "+attr+" ==unequal");
                                        need_update=true;
                                    }
                                }else {
                                   //logger.debug(tableName+" "+column_name+" "+attr+"not found!");
                                }
                            }
                            if (need_update){
                                String type="";
                                String extra=" null ";
                                String comment="";//

                                if (column_json.containsKey("COLUMN_TYPE")){
                                    type=column_json.getString("COLUMN_TYPE");
                                }
                                if (column_json.containsKey("EXTRA")){
                                    extra=column_json.getString("EXTRA");
                                }
                                if (column_json.containsKey("COLUMN_COMMENT")){
                                    if (StringUtils.isNotBlank(column_json.getString("COLUMN_COMMENT"))){
                                        comment=String.format(" comment '%s' ",column_json.getString("COLUMN_COMMENT"));
                                    }
                                }
                                //添加字段的语法：alter table tablename add (column datatype [default value][null/not null],….);
                                //修改字段的语法：alter table tablename modify (column datatype [default value][null/not null],….);
                                //删除字段的语法：alter table tablename drop (column);
                                // alter table tb_user modify mail varchar(63) null comment 'mail'
                                String up_sql=String.format("alter table %s modify  %s %s %s %s ",tableName,column_name,type,extra,comment);
                                tableDao.update_sql(up_sql);
                            }
//                            if( false && column_res.containsKey("COLUMN_DEFAULT") && column_json.containsKey("COLUMN_DEFAULT") ){
//                                Object r_c_def=column_json.get("COLUMN_DEFAULT");
//                                Object l_c_def=column_res.get("COLUMN_DEFAULT");
//                                if(r_c_def !=l_c_def ){
//                                    System.out.println("r_c_def-> != l_c_def->");
//                                    System.out.println(r_c_def);
//                                    System.out.println(l_c_def);
//                                    if(null==r_c_def){
//                                        //System.out.println("COLUMN_DEFAULT not eq,r="+column_json.toJSONString()+",l="+column_res.toString());
//                                        String up_sql="ALTER TABLE "+tableName+" MODIFY COLUMN "+column_name+" "+column_json.getString("COLUMN_TYPE") +" DEFAULT NULL ";
//                                        System.out.println(up_sql);
//                                        tableDao.update_sql(up_sql);
//                                    }else  {
//                                        // 远程default 存在值
//                                        String r_c_def_str=r_c_def.toString();
//                                        if( (null==l_c_def) || (null!=l_c_def && !r_c_def_str.equals(l_c_def.toString()))){
//                                            System.out.println(r_c_def_str+"!="+l_c_def.toString());
//                                            if (-1!=column_json.getString("COLUMN_TYPE").indexOf("varchar")){
//                                                String up_sql="ALTER TABLE "+tableName+" MODIFY COLUMN "+column_name+" "+column_json.getString("COLUMN_TYPE") +" DEFAULT '"+column_json.getString("COLUMN_DEFAULT")+"'";
//                                                tableDao.update_sql(up_sql);
//                                                System.out.println(up_sql);
//                                            }else {
//                                                String up_sql="ALTER TABLE "+tableName+" MODIFY COLUMN "+column_name+" "+column_json.getString("COLUMN_TYPE") +" DEFAULT "+column_json.getString("COLUMN_DEFAULT");
//                                                tableDao.update_sql(up_sql);
//                                                System.out.println(up_sql);
//                                            }
//                                        }
//
//                                    }
//                                }
//                            }
                        }else if(0==column_res_list.size()){
                            //本地不存在 --新增
                            List<Map> l_tb_res_2=tableDao.listTableOne(tableName);
                            if(1==l_tb_res_2.size()){
                                logger.info("not exist "+column_name +" ,r="+column_json.toJSONString());
                                //String up_sql="ALTER TABLE "+tableName+" ADD COLUMN "+column_name+" "+column_json.getString("COLUMN_TYPE") +" DEFAULT NULL ";
                                //tableDao.update_sql(up_sql);
                                //logger.debug(up_sql);
                                String type="";
                                String extra=" null ";
                                String comment="";//
                                if (column_json.containsKey("COLUMN_TYPE")){
                                    type=column_json.getString("COLUMN_TYPE");
                                }
                                if (column_json.containsKey("EXTRA")){
                                    extra=column_json.getString("EXTRA");
                                }
                                if (column_json.containsKey("COLUMN_COMMENT")){
                                    if (StringUtils.isNotBlank(column_json.getString("COLUMN_COMMENT"))){
                                        comment=String.format(" comment '%s' ",column_json.getString("COLUMN_COMMENT"));
                                    }
                                }
                                //添加字段的语法：alter table tablename add (column datatype [default value][null/not null],….);
                                //修改字段的语法：alter table tablename modify (column datatype [default value][null/not null],….);
                                //删除字段的语法：alter table tablename drop (column);
                                // alter table tb_user modify mail varchar(63) null comment 'mail'
                                String up_sql=String.format("alter table %s add  %s %s %s %s ",tableName,column_name,type,extra,comment);
                                tableDao.update_sql(up_sql);
                               //logger.debug(up_sql);
                            }else {
                                logger.error("更新"+tableName+"失败！tableName 不存在");
                            }
                        }
                    }
                }

                //远程表xx字段数量
                Integer r_column_count=column_array.size();
                List<Map> l_table_column_list=tableDao.listTableColumn(tableName);
                ////本地表xx字段数量
                Integer l_column_count=l_table_column_list.size();
                //查询本地的多余的删除
                if(!r_column_count.equals(l_column_count)){
                    for (int j = 0; j <l_table_column_list.size() ; j++) {
                        Map column_res=l_table_column_list.get(j);
                        if(column_res.containsKey("COLUMN_NAME")){
                            String l_column_name=column_res.get("COLUMN_NAME").toString();
                            boolean l_r_exist=false;
                            for (int k = 0; k <column_array.size() ; k++) {
                                JSONObject r_column_json=column_array.getJSONObject(k);
                                if(r_column_json.containsKey("COLUMN_NAME")){
                                    String r_column_name=r_column_json.getString("COLUMN_NAME");
                                    if(r_column_name.equals(l_column_name)){
                                        l_r_exist=true;
                                        break;
                                    }
                                }
                            }
                            if(!l_r_exist){
                                logger.info("r not exist,delete loctal ,l="+column_res.toString());
                                String up_sql="ALTER TABLE "+tableName+" DROP  COLUMN "+l_column_name ;
                                tableDao.update_sql(up_sql);
                            }
                        }
                    }
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try{
                //关闭流，释放资源
                fis.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }


    }

    private  List<String> up_db(String download_url, String mk_path,String r_hash2){
        StaticVariableUtils.is_in_update=true;
        StaticVariableUtils.up_name=CdnVersionEnum.M_MYSQL_DB.getName();
        StaticVariableUtils.up_time=System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                String down_path = download_url.trim();
                //down_path.replace("http://","");

                String mk_dir=mk_path.substring(0,mk_path.lastIndexOf("/"));
                String fileName = down_path.substring(down_path.lastIndexOf("/")+1);


                //1 download file
                //下载JSON 压缩包到mk_path
                String cmd_f=" cd "+mk_dir +" && rm -f "+ fileName +" && "+" curl -o "+mk_path+fileName+" " +down_path ;
                System.out.println(cmd_f);
                List<String> l=  ShellUtils.runShell(cmd_f,false);
                System.out.println(l);

                //check file-zip  hash2
                File file = new File(mk_path+fileName);
                if(file.isFile()){
                    String hash2=HashUtils.md5OfFile(file);
                    if(r_hash2.equals(hash2)){
                        System.out.println("start up db from json file");
                        upDbFromJsonFileHandle(mk_path+fileName);
                    }
                }
                StaticVariableUtils.is_in_update=false;
            }
        }).start();
        return null;
    }


    /**
     * @param client 获取version
     */
    private void getNodeAgentVersionByRedis(CdnClientEntity client){
        String v= redisUtils.get(String.format("version_%s",client.getClientIp()));
       //logger.debug("------"+v);
        //"1.32|{\"nginx_version\":\"1.19.9\",\"ants_waf\":\"2.31\"}"
        if (StringUtils.isNotBlank(v)){
            NodeVersionVo vo=NodeVersionVo.getVersionObj(v);
            if (null!=vo.getNginxWafVersion() || null!=vo.getAgentVersion()){
                if ( !vo.getNginxWafVersion().equals(client.getVersion())){
                    client.setNgxVersion(vo.getNginxVersion());
                    client.setVersion(vo.getNginxWafVersion());
                }
                if(!vo.getAgentVersion().equals(client.getAgentVersion())){
                    client.setAgentVersion(vo.getAgentVersion());

                }
                cdnClientDao.updateById(client);
            }

        }
    }


    private void updateLocalNgxVersion(){
        List<CdnClientEntity> clientList=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",1)
                .eq("status",1).
                isNotNull("reg_info")
        );
        //1初始化获取需要推送的节点IP
        for (CdnClientEntity client: clientList){
            String v= redisUtils.get(String.format("version_%s",client.getClientIp()));
            if (StringUtils.isNotBlank(v)){
                NodeVersionVo vo=NodeVersionVo.getVersionObj(v);
                if (!vo.getNginxWafVersion().equals(client.getVersion())){
                    client.setVersion(vo.getNginxWafVersion());
                    cdnClientDao.updateById(client);
                }
            }
        }
    }


    private void upNginx( CdnUpdateItemVo updateItemVo){
        //downloadFile
        String goods=updateItemVo.getGoods();
        StaticVariableUtils.is_in_update=true;
        StaticVariableUtils.up_name="up_nginx";
        StaticVariableUtils.up_time=System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //System.out.println(jsonObject.toJSONString());
                StaticVariableUtils.up_name=goods;


                //2 向节点推送
                String xaddSk="public"+ RedisStreamType.UPDATE_COMMAND.getName();
                String xaddK="normal";
                String xaddV=DataTypeConversionUtil.entity2jonsStr(updateItemVo);
                String commonXaddId= redisUtils.streamXAdd(xaddSk, xaddK,xaddV);
                logger.info(commonXaddId+"----"+xaddSk+"----"+xaddK+"----"+xaddV);
                //3 检测nginx结果
                try {
                    Thread.sleep(60*1000);
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
                updateLocalNgxVersion();
                StaticVariableUtils.is_in_update=false;
            }
        }).start();
    }


    private void updateLocalAgentVersion(){
        List<CdnClientEntity> clientList=cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>()
                .eq("client_type",1)
                .eq("status",1)
                .isNotNull("reg_info")
        );
        for(CdnClientEntity client:clientList){
            String v= redisUtils.get(String.format("version_%s",client.getClientIp()));
            if (StringUtils.isNotBlank(v)){
                NodeVersionVo vo=NodeVersionVo.getVersionObj(v);
                if (!vo.getAgentVersion().equals(client.getAgentVersion())){
                    client.setAgentVersion(vo.getAgentVersion());
                    cdnClientDao.updateById(client);
                }
            }
        }
    }



    /**
     * 更新节点(nginx,waf,agent)
     * @param updateItemVo
     */
    private void upAntsCdnAgent(CdnUpdateItemVo updateItemVo){
        StaticVariableUtils.is_in_update=true;
        StaticVariableUtils.up_name="up_node";
        StaticVariableUtils.up_time=System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String goods=updateItemVo.getGoods();
                StaticVariableUtils.up_name=goods;

                //1初始化获取需要推送的节点IP
                String xaddSk="public"+ RedisStreamType.UPDATE_COMMAND.getName();
                String xaddK="normal";
                String xaddV=DataTypeConversionUtil.entity2jonsStr(updateItemVo);
                String commonXaddId= redisUtils.streamXAdd(xaddSk, xaddK,xaddV);
                logger.info(commonXaddId+"----"+xaddSk+"----"+xaddK+"----"+xaddV);

                //3 检测agent结果
                try {
                    Thread.sleep(60*1000);
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
                updateLocalAgentVersion();
                StaticVariableUtils.is_in_update=false;
            }
        }).start();

    }

    @SysLog("版本更新")
    @GetMapping("/version/update")
    @RequiresPermissions("sys:version:update")
    public R versionUpdate(@RequestParam String goods, @RequestParam String hash){
        checkDemoModify();
        Long nowTm=System.currentTimeMillis();
        if(StaticVariableUtils.is_in_update && null!=StaticVariableUtils.up_time &&  Math.abs(nowTm-StaticVariableUtils.up_time)<600*1000 ){
            return R.error("存在后台更新任务，正在更新中...");
        }
        if (StringUtils.isNotBlank(QuerySysAuth.AUTH_GOODS_CODE_HEAD) && !StaticVariableUtils.goods_code.startsWith(QuerySysAuth.AUTH_GOODS_CODE_HEAD)){
            return R.error("auth code need start with ["+ QuerySysAuth.AUTH_GOODS_CODE_HEAD+"]");
        }
        if( null==StaticVariableUtils.authEndTime ||  DateUtils.stamp2date(StaticVariableUtils.authEndTime).before(new Date())){
            return R.error("授权未知");
        }
        if(!CdnVersionEnum.M_MYSQL_DB.getKey().equals(goods) && !StaticVariableUtils.db_sync_status){
            return R.error("有数据库未更新！请先更新数据库！");
        }
        final String url= QuerySysAuth.getTargetVersionInfo("3",goods,hash);
        String  data=  HttpRequest.curlHttpGet(url);
        if(StringUtils.isBlank(data)){
            return R.error("未获取到更新数据");
        }
        try {
            JSONObject jsonObject=JSONObject.parseObject(data);
            if(jsonObject.containsKey("code") && jsonObject.containsKey("msg") && 0==jsonObject.getInteger("code")){
                return R.error(jsonObject.getString("msg"));
            }else if(jsonObject.containsKey("code") && jsonObject.containsKey("data") && 1==jsonObject.getInteger("code")){
                JSONObject json_data=jsonObject.getJSONObject("data");
                CdnUpdateItemVo updateItemVo=DataTypeConversionUtil.json2entity(json_data,CdnUpdateItemVo.class);
                List<String> upResult=new ArrayList<>();
                String downUrl=updateItemVo.getUrl();
                String mkPath=updateItemVo.getMkurl();
                switch (CdnVersionEnum.getEnumItemByKey(goods)){
                    case M_JAVA_JAR:
                        upResult=this.upWebController(downUrl,mkPath,updateItemVo.getHash2());
                        break;
                    case M_WEB_USER:
                        if (true){
                            WebDirConfig webConf =getSysDir();
                            if (null==webConf){
                                upResult=this.upWebView(downUrl,mkPath);
                            }else {
                                upResult=this.upWebView(downUrl,webConf.getUserDir());
                            }
                        }
                        break;
                    case  M_WEB_MANAGER:
                        if (true){
                            WebDirConfig webConf =getSysDir();
                            if (null==webConf){
                                upResult=this.upWebView(downUrl,mkPath);
                            }else {
                                upResult=this.upWebView(downUrl,webConf.getAdminDir());
                            }
                        }
                        break;
                    case M_MYSQL_DB:
                        upResult=this.up_db(downUrl,mkPath,updateItemVo.getHash2());
                        break;
                    case    M_NODE_AGENT:
                        this.upAntsCdnAgent(updateItemVo);
                        break;
                    case    M_NODE_NGINX:
                        this.upNginx(updateItemVo);
                        break;
                    default:
                        logger.error("unknown type["+goods+"]");
                        break;
                }
                return R.ok().put("data",upResult);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return R.error("升级失败");
    }


}
