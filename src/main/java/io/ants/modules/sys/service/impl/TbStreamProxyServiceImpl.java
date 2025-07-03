package io.ants.modules.sys.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.common.exception.RRException;
import io.ants.common.utils.*;
import io.ants.modules.app.dao.TbStreamProxyAttrDao;
import io.ants.modules.app.dao.TbStreamProxyDao;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbStreamProxyAttrEntity;
import io.ants.modules.app.entity.TbStreamProxyEntity;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.app.form.QueryStreamListForm;
import io.ants.modules.app.vo.StreamInfoVo;
import io.ants.modules.sys.dao.CdnSuitDao;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.enums.PushTypeEnum;
import io.ants.modules.sys.service.CdnMakeFileService;
import io.ants.modules.sys.service.TbStreamProxyService;
import io.ants.modules.sys.service.CdnSuitService;
import io.ants.modules.sys.service.InputAvailableService;
import io.ants.modules.sys.vo.ProductAttrVo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 四层转发
 * 
 * @author Administrator
 */
@Service
public class TbStreamProxyServiceImpl implements TbStreamProxyService {
    // private static final String SET_SUFFIX=":file-path";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TbStreamProxyDao tbStreamProxyDao;
    @Autowired
    private TbStreamProxyAttrDao tbStreamProxyAttrDao;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private CdnSuitService suitService;
    @Autowired
    private InputAvailableService inputAvailableService;
    @Autowired
    private CdnMakeFileService cdnMakeFileService;
    @Autowired
    private CdnSuitDao cdnSuitDao;

    private String getSiteUserName(Long userId) {
        TbUserEntity user = tbUserDao.selectById(userId);
        if (null != user) {
            if (StringUtils.isNotBlank(user.getUsername())) {
                return user.getUsername();
            } else if (StringUtils.isNotBlank(user.getMobile())) {
                return user.getMobile();
            } else if (StringUtils.isNotBlank(user.getMail())) {
                return user.getMail();
            } else {
                return user.getUserId() + "";
            }
        }
        return null;
    }

    private CdnSuitEntity getSiteSuitInfo(String serialNumber) {
        // 套餐信息
        return suitService.getSuitDetailBySerial(null, serialNumber, false, false);
    }

    /**
     * 获取4层转发的分页列表
     * 
     * @param params
     * @return
     */
    @Override
    public PageUtils streamList(QueryStreamListForm params) {
        // {"listen":88,"protocol":"TCP/UDP","server_mode":"weight","proxy_protocol":1,"proxy_timeout":"30s","proxy_connect_timeout":"60s","server":["1.1.1.1:120
        // weight=1"]}
        List<Integer> ids = new ArrayList<>();
        if (StringUtils.isNotBlank(params.getSourceIp()) || null != params.getListenPort()
                || null != params.getSourcePort()) {
            ids.add(0);
            List<TbStreamProxyEntity> list = tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
                    .eq(null != params.getListenPort(), "bind_port", params.getListenPort())
                    .like(StringUtils.isNotBlank(params.getSourceIp()), "conf_info", params.getSourceIp())
                    .like(null != params.getSourcePort(), "conf_info", params.getSourcePort()));
            list.forEach(item -> {
                StreamInfoVo vo = DataTypeConversionUtil.string2Entity(item.getConfInfo(), StreamInfoVo.class);
                if (null != vo) {
                    if (null != params.getListenPort() && params.getListenPort().toString().equals(vo.getListen())) {
                        ids.add(item.getId());
                    } else if (StringUtils.isNotBlank(params.getSourceIp()) || null != params.getSourcePort()) {
                        // "server": ["1.1.1.1:89 weight=1"]
                        for (int i = 0; i < vo.getServer().size(); i++) {
                            String sv = vo.getServer().getString(i);
                            if (StringUtils.isNotBlank(params.getSourceIp()) && sv.contains(params.getSourceIp())) {
                                ids.add(item.getId());
                            } else if (null != params.getSourcePort()
                                    && sv.contains(":" + params.getSourcePort().toString())) {
                                ids.add(item.getId());
                            }
                        }
                    }

                }
            });
        }
        if (StringUtils.isBlank(params.getUserIds())) {
            params.setUserIds("");
        }
        IPage<TbStreamProxyEntity> page = tbStreamProxyDao.selectPage(
                new Page<>(params.getPage(), params.getLimit()),
                new QueryWrapper<TbStreamProxyEntity>()
                        .in(ids.size() > 0, "id", ids)
                        .in(StringUtils.isNotBlank(params.getUserIds()), "user_id", params.getUserIds().split(",")));
        page.getRecords().forEach(item -> {
            item.setUser(this.getSiteUserName(item.getUserId()));
            CdnSuitEntity suitObj = null;
            if (StringUtils.isNotBlank(item.getSuitJsonObj())) {
                JSONObject obj = DataTypeConversionUtil.string2Json(item.getSuitJsonObj());
                if (null != obj.get("startTime") && null != obj.get("endTime")) {
                    obj.put("startTime", DateUtils.parseDate(obj.getString("startTime")));
                    obj.put("endTime", DateUtils.parseDate(obj.getString("endTime")));
                }
                suitObj = DataTypeConversionUtil.json2entity(obj, CdnSuitEntity.class);
            }
            if (null != suitObj) {
                item.setSuit(suitObj);
                if (StringUtils.isNotBlank(suitObj.getCname())) {
                    String top = Objects.requireNonNull(HashUtils.md5ofString(item.getId() + "")).substring(0, 4);
                    String cname = suitObj.getCname().toString().replace("*.", "");
                    item.setCname(top + "." + cname);
                }
            }

        });
        return new PageUtils(page);
    }

    // 检测端口占用
    private boolean streamCheckListenIsAvailable(Integer listen, TbStreamProxyEntity proxy) {
        return inputAvailableService.checkListenIsAvailable(listen, proxy.getAreaId(), "proxy", proxy, null);
    }

    private void insertStreamProxyTask(String type, TbStreamProxyEntity streamProxy) {
        Map<String, String> map = new HashMap<>(8);
        if (type.equals("save") && 1 == streamProxy.getStatus()) {
            map.put(PushTypeEnum.STREAM_CONF.getName(), streamProxy.getId().toString());
            cdnMakeFileService.pushByInputInfo(map);
        } else if (type.equals("stop")) {
            map.put(PushTypeEnum.CLEAN_STOP_STREAM_CONF.getName(), streamProxy.getId().toString());
            cdnMakeFileService.pushByInputInfo(map);
        } else if (type.equals("delete")) {
            map.put(PushTypeEnum.CLEAN_DEL_STREAM_CONF.getName(), streamProxy.getId().toString());
            cdnMakeFileService.pushByInputInfo(map);
        } else {
            logger.error(type + " : unknown ");
        }

    }

    /**
     * 检测
     * 
     * @param userId
     * @param serialNumber
     */
    private void checkCreateLimit(Long userId, String serialNumber) {
        TbUserEntity user = tbUserDao.selectById(userId);
        if (null == user) {
            throw new RRException("无此用户");
        }
        CdnSuitEntity suitEntity = cdnSuitDao.selectOne(new QueryWrapper<CdnSuitEntity>()
                .eq("user_id", userId)
                .eq("serial_number", serialNumber)
                .last("limit 1"));
        if (null == suitEntity) {
            throw new RRException("无此套餐");
        }

        CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(null, serialNumber, false, true);
        if (null == suitObj) {
            throw new RRException("套餐数据有误！");
        }
        if (null == suitObj.getAttr()) {
            throw new RRException("当前套餐无创建站点权限[0]！");
        }
        // 套餐中可建转发数量
        // ProductAttrVo
        // attrObj=DataTypeConversionUtil.json2entity(suitObj.getAttr(),ProductAttrVo.class)
        // ;
        ProductAttrVo attrObj = suitObj.getAttr();
        if (null == attrObj || null == attrObj.getPort_forwarding() || 0 == attrObj.getPort_forwarding()) {
            throw new RRException("当前套餐无权限[1]！");
        }
        if (null != suitObj.getConsume()) {
            // ProductAttrVo
            // vo=DataTypeConversionUtil.json2entity(suitObj.getConsume(),ProductAttrVo.class);
            ProductAttrVo vo = suitObj.getConsume();
            if (null == vo || attrObj.getPort_forwarding() <= vo.getPort_forwarding()) {
                throw new RRException("创建失败！当前套餐可创建数量已超出限制【" + attrObj.getPort_forwarding() + "】");
            }

        }

    }

    private void checkConfInfoRule(StreamInfoVo streamInfoVo) {
        // check streamInfoVo
        /**
         * "listen": 88,
         * "protocol": "TCP/UDP",
         * "server_mode": "weight",
         * "proxy_timeout": "30s",
         * "proxy_connect_timeout": "60s",
         * "server": ["1.1.11:80 weight=1"]
         */
        if (null == streamInfoVo || StringUtils.isBlank(streamInfoVo.getServer_mode())) {
            throw new RRException("参数缺少！");
        }

        switch (streamInfoVo.getServer_mode()) {
            case "weight":
                if (true) {
                    // {"listen":"12345","server_mode":" ",
                    // "proxy_timeout":"30s","proxy_connect_timeout":"60s","server":["119.97.137.46:22
                    // weight=NaN"]}
                    JSONArray serverArray = streamInfoVo.getServer();
                    for (int i = 0; i < serverArray.size(); i++) {
                        // eg "1.1.11:80 weight=1"
                        String s = serverArray.getString(i);
                        String[] ps = s.split("\\s+|:|=");
                        if (ps.length != 4) {
                            throw new RRException(s + "参数格式有误[!=4]！");
                        }
                        if (!IPUtils.isValidIPV4(ps[0])) {
                            throw new RRException(s + "参数格式有误[ip]！");
                        }
                        if (!IPUtils.isValidPort(ps[1])) {
                            throw new RRException(s + "参数格式有误[port]！");
                        }
                        if (!"weight".equals(ps[2])) {
                            throw new RRException(s + "参数格式有误[weight]！");
                        }
                        if (!IPUtils.isValidPort(ps[3])) {
                            throw new RRException(s + "参数格式有误[value]！");
                        }
                    }
                }
                break;
            case "polling":
            case "hash":
                if (true) {
                    JSONArray serverArray = streamInfoVo.getServer();
                    for (int i = 0; i < serverArray.size(); i++) {
                        String s = serverArray.getString(i);
                        // "1.1.11.1:80"
                        String[] ps = s.split(":");
                        if (2 != ps.length) {
                            throw new RRException(s + "参数格式有误[!=2]！");
                        }
                        if (!IPUtils.isValidIPV4(ps[0])) {
                            throw new RRException(s + "参数格式有误[ip]！");
                        }
                        if (!IPUtils.isValidPort(ps[1])) {
                            throw new RRException(s + "参数格式有误[port]！");
                        }
                    }
                }
                break;
            default:
                logger.error("[" + streamInfoVo.getServer_mode() + "] 格式有误！");
                throw new RRException(streamInfoVo.getServer_mode() + "参数类型有误！");
        }

    }

    private void saveListenToAttr(String[] listenList, TbStreamProxyEntity streamProxy) {
        List<String> ListenBuf = new ArrayList<>(listenList.length);
        for (String listenP : listenList) {
            if (ListenBuf.contains(listenP)) {
                continue;
            }
            ListenBuf.add(listenP);
            TbStreamProxyAttrEntity attrEntity = new TbStreamProxyAttrEntity();
            attrEntity.setStreamId(streamProxy.getId());
            attrEntity.setAreaId(streamProxy.getAreaId());
            attrEntity.setPkey("port");
            attrEntity.setPvalue(listenP);
            tbStreamProxyAttrDao.insert(attrEntity);
        }
    }

    /**
     * 保存
     * 
     * @return
     */
    @Override
    public TbStreamProxyEntity saveProxy(TbStreamProxyEntity streamProxy) {
        /*
         * "{"id":0,"userId":1,"serialNumber":"1122","status":1,"confInfo":"{
         * \"listen\":1456,\"server\":[\"172.16.1.5:80 weight=5 max_fails=3 fail_timeout=30s\",\"2.2.2.2:880\"],\"server_mode\":\"hash $remote_addr consistent\",\"proxy_connect_timeout\":\"60s\",\"proxy_timeout\":\"30s\"}"
         * }"
         */
        JSONObject info = DataTypeConversionUtil.string2Json(streamProxy.getConfInfo());
        if (!info.containsKey("listen") || !info.containsKey("server") || null == info.get("listen")) {
            // logger.debug("insert fail,conf is error");
            throw new RRException(" 配置有误！");
        }

        if (info.containsKey("listen") && null != info.get("listen")) {
            info.put("listen", info.get("listen").toString());
        }
        StreamInfoVo streamInfoVo = DataTypeConversionUtil.json2entity(info, StreamInfoVo.class);
        if (null == streamInfoVo || null == streamInfoVo.getListen()) {
            throw new RRException("参数不完整！");
        }

        // 检测参数规则
        this.checkConfInfoRule(streamInfoVo);

        if (null == streamProxy.getId() || 0 == streamProxy.getId()) {
            // insert
            if (null == streamProxy.getUserId() || StringUtils.isBlank(streamProxy.getSerialNumber())
                    || StringUtils.isBlank(streamProxy.getConfInfo())) {
                // logger.debug("insert fail,param is empty");
                throw new RRException(" 参数不足！");
            }

            String[] listenList = streamInfoVo.getListen().split("\\|");
            // 检测端口占用
            streamProxy.setAreaId(Integer
                    .parseInt(cdnMakeFileService.getNodeAreaGroupIdBySerialNumber(streamProxy.getSerialNumber())));

            for (String listen : listenList) {
                if (!this.streamCheckListenIsAvailable(Integer.parseInt(listen), streamProxy)) {
                    break;
                }
            }
            // 检测套餐中可建proxy数据
            this.checkCreateLimit(streamProxy.getUserId(), streamProxy.getSerialNumber());
            // 同步bindPort
            streamProxy.setBindPort(listenList[0]);
            tbStreamProxyDao.insert(streamProxy);
            this.saveListenToAttr(listenList, streamProxy);
            this.insertStreamProxyTask("save", streamProxy);
            return streamProxy;
        } else {
            // modify
            TbStreamProxyEntity sourceSp = tbStreamProxyDao.selectById(streamProxy.getId());
            if (null == sourceSp) {
                // logger.debug("insert fail,id is error ");
                throw new RRException("  源数据为空！");
            }
            if (null != streamProxy.getUserId()) {
                if (!sourceSp.getUserId().equals(streamProxy.getUserId())) {
                    // logger.debug("insert fail,id is error ");
                    throw new RRException("用户归属有误！");
                }
            }

            // suit
            if (StringUtils.isNotBlank(streamProxy.getSerialNumber())) {
                CdnSuitEntity suitInfos = suitService.getSuitDetailBySerial(sourceSp.getUserId(),
                        streamProxy.getSerialNumber(), false, true);
                if (null == suitInfos) {
                    // logger.debug("insert fail,suitInfos is empty");
                    throw new RRException(" 套餐有误！");
                }
                sourceSp.setSerialNumber(streamProxy.getSerialNumber());
            }
            if (StringUtils.isBlank(streamProxy.getConfInfo())) {
                throw new RRException(" 配置有误！");
            }
            sourceSp.setConfInfo(streamProxy.getConfInfo());
            // 检测端口占用
            String[] listenList = streamInfoVo.getListen().split("\\|");
            // 检测端口占用
            streamProxy.setAreaId(Integer
                    .parseInt(cdnMakeFileService.getNodeAreaGroupIdBySerialNumber(streamProxy.getSerialNumber())));
            for (String listen : listenList) {
                if (!this.streamCheckListenIsAvailable(Integer.parseInt(listen), streamProxy)) {
                    break;
                }
            }
            if (null != streamProxy.getStatus()) {
                sourceSp.setStatus(streamProxy.getStatus());
            }
            // 同步bindPort
            streamProxy.setBindPort(listenList[0]);
            streamProxy.setAreaId(Integer
                    .parseInt(cdnMakeFileService.getNodeAreaGroupIdBySerialNumber(streamProxy.getSerialNumber())));
            tbStreamProxyDao.updateById(sourceSp);
            tbStreamProxyAttrDao.delete(new QueryWrapper<TbStreamProxyAttrEntity>().eq("stream_id", sourceSp.getId()));
            this.saveListenToAttr(listenList, streamProxy);
            this.insertStreamProxyTask("save", streamProxy);
            if (0 == sourceSp.getStatus()) {
                this.insertStreamProxyTask("stop", sourceSp);
            }
            return sourceSp;
        }
    }

    /**
     * 批量删除
     * 
     * @param ids
     */
    @Override
    public void batchDelete(Long userId, String ids) {
        for (String id : ids.split(",")) {
            TbStreamProxyEntity sp = tbStreamProxyDao.selectOne(new QueryWrapper<TbStreamProxyEntity>()
                    .eq("id", id)
                    .eq(null != userId, "user_id", userId));
            if (null != sp) {
                this.insertStreamProxyTask("delete", sp);
                sp.setStatus(2);
                tbStreamProxyDao.updateById(sp);
            }

        }
    }

    @Override
    public Integer changeProxyStatus(Long userId, Integer streamProxyId, Integer status) {
        TbStreamProxyEntity proxy = tbStreamProxyDao.selectById(streamProxyId);
        if (null == proxy) {
            throw new RRException("ID 有误【1】");
        }
        if (null != userId) {
            if (!userId.equals(proxy.getUserId())) {
                throw new RRException("ID 有误【2】");
            }
        }
        if (0 == status) {
            proxy.setStatus(status);
            tbStreamProxyDao.updateById(proxy);
            // public：command-stream
            // normal
            // rm -rf path
            this.insertStreamProxyTask("stop", proxy);
        } else if (1 == status) {
            // 开启
            // 检测套餐 1是否存在 2是否超出流量
            CdnSuitEntity suitObj = suitService.getSuitDetailBySerial(userId, proxy.getSerialNumber(), false, true);
            if (null == suitObj) {
                throw new RRException("开启失败,套餐数据有误！");
            }
            if (new Date().after(suitObj.getEndTime())) {
                throw new RRException("开启失败,套餐过期！");
            }
            if (null == suitObj.getAttr()) {
                throw new RRException("开启失败,获取当前套餐权限失败[0]！");
            }
            // ProductAttrVo
            // maxAttrInfo=DataTypeConversionUtil.json2entity(suitObj.getAttr(),ProductAttrVo.class)
            // ;
            ProductAttrVo maxAttrInfo = suitObj.getAttr();
            if (null != maxAttrInfo && null != suitObj.getConsume()) {
                // ProductAttrVo usedConsumeInfo=
                // DataTypeConversionUtil.json2entity(suitObj.getConsume(),ProductAttrVo.class);
                ProductAttrVo usedConsumeInfo = suitObj.getConsume();
                // 检测套餐流量
                if (null != usedConsumeInfo && usedConsumeInfo.getFlow() > maxAttrInfo.getFlow()) {
                    throw new RRException("开启失败,【流量】已超出！");
                }

            }
            proxy.setStatus(status);
            tbStreamProxyDao.updateById(proxy);
            this.insertStreamProxyTask("save", proxy);
        }
        return null;
    }

    /**
     * 更新数据库后 做数据更新处理
     */
    @Override
    public void reInitDefaultParam() {
        // 2023 03 24 添加areaId listPort
        List<TbStreamProxyEntity> list = tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
                .and(q -> q.isNull("bind_port").or().isNull("area_id")));
        for (TbStreamProxyEntity proxy : list) {
            StreamInfoVo streamInfoVo = DataTypeConversionUtil.string2Entity(proxy.getConfInfo(), StreamInfoVo.class);
            if (null != streamInfoVo) {
                proxy.setBindPort(streamInfoVo.getListen().toString());
            }
            proxy.setAreaId(
                    Integer.parseInt(cdnMakeFileService.getNodeAreaGroupIdBySerialNumber(proxy.getSerialNumber())));
            tbStreamProxyDao.updateById(proxy);
        }

        // 2023 03 27 添加proxyAttr
        Long count = tbStreamProxyAttrDao
                .selectCount(new QueryWrapper<TbStreamProxyAttrEntity>().isNotNull("stream_id"));
        // logger.debug("count:"+count);
        if (0 == count) {
            List<TbStreamProxyEntity> list2 = tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>());
            for (TbStreamProxyEntity proxy : list2) {
                StreamInfoVo streamInfoVo = DataTypeConversionUtil.string2Entity(proxy.getConfInfo(),
                        StreamInfoVo.class);
                if (null != streamInfoVo) {
                    String[] listenS = streamInfoVo.getListen().split("\\|");
                    this.saveListenToAttr(listenS, proxy);
                }
            }
        }

    }

    @Override
    public R getAllPort(Long userId) {
        List<TbStreamProxyEntity> spList = tbStreamProxyDao.selectList(new QueryWrapper<TbStreamProxyEntity>()
                .eq(null != userId, "user_id", userId)
                .select("id"));
        List<String> ports = new ArrayList<>();
        for (TbStreamProxyEntity proxy : spList) {
            List<TbStreamProxyAttrEntity> ls = tbStreamProxyAttrDao
                    .selectList(new QueryWrapper<TbStreamProxyAttrEntity>()
                            .eq("stream_id", proxy.getId())
                            .eq("pkey", "port"));
            ports.addAll(ls.stream().map(q -> q.getPvalue()).collect(Collectors.toList()));
        }
        return R.ok().put("data", ports);
    }

    @Override
    public R getDetailById(Long userId, Integer id) {
        TbStreamProxyEntity tbStreamProxyEntity = tbStreamProxyDao.selectOne(new QueryWrapper<TbStreamProxyEntity>()
                .eq("id", id)
                .eq(null != userId, "user_id", userId)
                .last("limit 1"));
        if (null == tbStreamProxyEntity) {
            return R.error("id is error");
        }
        CdnSuitEntity suitObj = this.getSiteSuitInfo(tbStreamProxyEntity.getSerialNumber());
        tbStreamProxyDao.update(null,
                new UpdateWrapper<TbStreamProxyEntity>()
                        .set("suit_json_obj", DataTypeConversionUtil.entity2jonsStr(suitObj))
                        .eq("id", tbStreamProxyEntity.getId()));
        if (null != suitObj) {
            tbStreamProxyEntity.setSuit(suitObj);
            if (StringUtils.isNotBlank(suitObj.getCname())) {
                String top = Objects.requireNonNull(HashUtils.md5ofString(tbStreamProxyEntity.getId() + ""))
                        .substring(0, 4);
                String cname = suitObj.getCname().toString().replace("*.", "");
                tbStreamProxyEntity.setCname(top + "." + cname);
            }
        }
        return R.ok().put("data", tbStreamProxyEntity);
    }

}
