package io.ants.modules.sys.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.common.utils.*;
import io.ants.modules.sys.dao.CdnClientDao;
import io.ants.modules.sys.entity.CdnClientEntity;
import io.ants.modules.sys.enums.CommandEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.*;

@RestController
@RequestMapping("/sys/cdnsys/admin/tool/")
public class AdminToolsController extends AbstractController {

    @Autowired
    private CdnClientDao cdnClientDao;
    @Autowired
    private RedisUtils redisUtils;

    @GetMapping("/enums")
    public R enums() {
        Map<String, Object> map = new HashMap(1024);
        map.put("commands", CommandEnum.GetAll());
        return R.ok().put("data", map);
    }

    @GetMapping("/node/ping")
    public R nodePing() {
        Map map = new HashMap(1024);
        List<CdnClientEntity> list = cdnClientDao.selectList(new QueryWrapper<CdnClientEntity>());
        for (CdnClientEntity client : list) {
            String ip = client.getClientIp();
            if (IPUtils.isValidIPV4ByCustomRegex(ip)) {
                try {
                    // 超时应该在3钞以上
                    int timeOut = 3000;
                    boolean status = InetAddress.getByName(ip).isReachable(timeOut);
                    // 当返回值是true时，说明host是可用的，false则不可。
                    map.put(ip, status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return R.ok().put("data", map);
    }

    @PostMapping("/redis/data")
    public R getRedisDate(@RequestBody Map map) {
        Set<Object> result = new HashSet<>();
        String key = "";
        if (map.containsKey("key")) {
            key = map.get("key").toString();
        }
        if (StringUtils.isBlank(key)) {
            // Set<String> set= redisUtils.scanAll("*:file-path");
            // result.addAll(set);
            result.add("allfile-path*");
            result.add("PUB_CONF_SET:file-path");
            result.add("PUB_SINGLE_SET:*:file-path");
            result.add("PUB_WAF_SET:file-path");
            result.add("ETC_WAF_IP_SET:file-path");
            // result.add("NFT:file-path");
            result.add("SITE_*_SET:file-path");
            result.add("STREAM_*_SET:file-path");
            result.add("REWRITE_*_SET:file-path");
            return R.ok().put("list", result);
        } else {
            if (key.contains("*") && !key.endsWith(".conf")) {
                Set<String> set = redisUtils.scanAll(key);
                if (set.size() > 0) {
                    result.addAll(set);
                    return R.ok().put("list", result);
                }
            } else if (key.endsWith("file-path")) {
                Set<Object> set = redisUtils.setSMembers(key);
                result.addAll(set);
                return R.ok().put("list", result);
            } else if (key.contains("allfile-path")) {
                Set<Object> set = redisUtils.setSMembers(key);
                result.addAll(set);
                return R.ok().put("list", result);
            }
            String data = redisUtils.get(key);
            return R.ok().put("data", data);
        }

    }

    @PostMapping("/node/setLogLevel")
    public R nodeLog(@RequestBody Map map) {
        if (!map.containsKey("level") || !map.containsKey("ids")) {
            return R.error("参数缺失！");
        }
        String level = map.get("level").toString();
        String ids = map.get("ids").toString();
        List<String> list = Arrays.asList(StaticVariableUtils.NGX_ERR_LOG_TYPES);
        if (!list.contains(level)) {
            return R.error("类型不符！").put("data", StaticVariableUtils.NGX_ERR_LOG_TYPES);
        }
        String[] id_s = ids.split(",");
        for (String id : id_s) {
            CdnClientEntity client = cdnClientDao.selectById(id);
            if (null == client) {
                continue;
            }
            if (1 != client.getStatus()) {
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            if (StringUtils.isNotBlank(client.getConfInfo())) {
                jsonObject = DataTypeConversionUtil.string2Json(client.getConfInfo());
            }
            jsonObject.put("error_log", "error_log  logs/error.log  " + level + ";");
            client.setConfInfo(jsonObject.toJSONString());
            cdnClientDao.updateById(client);
        }
        // 推送nginx.conf
        return R.ok();
    }

    private String getPath() {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        if (System.getProperty("os.name").contains("dows")) {
            path = path.substring(1, path.length());
        }
        if (path.contains("jar")) {
            path = path.substring(0, path.lastIndexOf("."));
            return path.substring(0, path.lastIndexOf("/"));
        }
        return path.replace("target/classes/", "");
    }

    @RequestMapping("/error.log")
    public R viewErrLog(@RequestParam Integer id) {
        CdnClientEntity client = cdnClientDao.selectById(id);
        if (null == client) {
            return R.error("节点不存在");
        }
        if (1 != client.getStatus()) {
            return R.error("节点状态不可获取");
        }
        // # curl 121.62.60.60/error_log/error.log
        String url = "http://" + client.getClientIp() + "/error_log/error.log";
        String savaName = HashUtils.md5ofString("client-" + client.getClientIp()) + "error.log";
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("windows") >= 0) {
            String dir = this.getPath() + "upload\\" + client.getClientIp() + "\\";
            dir = dir.replace("/", "\\");
            String path = dir + savaName;
            String cmd1 = "cmd /c mkdir \"" + dir + "\"";
            String cmd2 = "cmd /c curl -s -o " + path + " " + url;
            ShellUtils.runShell(cmd1, false);
            ShellUtils.runShell(cmd2, false);
        } else if (os.indexOf("linux") >= 0) {
            String dir = this.getPath() + "/upload/" + client.getClientIp() + "/";
            dir = dir.replace("file:", "");
            // : /bin/sh -c mkdir "file:/usr/ants/cdn-apiupload/121.62.60.60/"
            String path = dir + savaName;
            String cmd1 = "mkdir \"" + dir + "\"";
            String cmd2 = "curl -s -o " + path + " " + url;
            // logger.debug(cmd1);
            //// logger.debug(cmd2);
            ShellUtils.runShell(cmd1, false);
            ShellUtils.runShell(cmd2, false);
        }
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        if (null == request) {
            return R.error("request is null");
        }
        String host = request.getServerName();
        Integer port = request.getServerPort();
        // http://cdn20.antsxdp.com/nodedownload/121.62.60.60/error.log
        String downloadPath = StaticVariableUtils.MasterProtocol + "://" + host + ":" + port + "/nodedownload/"
                + client.getClientIp() + "/" + savaName;
        return R.ok().put("path", downloadPath);

    }

}
