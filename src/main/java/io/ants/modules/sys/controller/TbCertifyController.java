package io.ants.modules.sys.controller;

import io.ants.common.annotation.SysLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.form.QueryCertPageForm;
import io.ants.modules.app.service.TbCertifyService;
import io.ants.modules.app.service.TbUserService;
import io.ants.modules.app.vo.ZeroSslAPiCreateCertForm;
import io.ants.modules.sys.enums.CertSrcTypeEnums;
import io.ants.modules.sys.vo.CertApplyVo;
import io.ants.modules.sys.vo.CertReIssuedVo;
import io.ants.modules.sys.vo.CertRemarkVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/certify/")
public class TbCertifyController extends AbstractController {

    @Autowired
    private TbCertifyService tbCertifyService;
    @Autowired
    private TbUserService userService;


    @GetMapping("/statistics")
    public R statistics(){
        return tbCertifyService.getCertStatistics(null);
    }

    @PostMapping("/list")
    public R list(@RequestBody Map params){
        //Long[] userIds, Integer page, Integer limit, String key
        QueryCertPageForm form= DataTypeConversionUtil.map2entity(params, QueryCertPageForm.class);

        if(StringUtils.isNotBlank(form.getUser())){
            List<Long> uids=new ArrayList<>();
            uids.add(0L);
            String userIds=userService.key2userIds(form.getUser());
            String[]  id_s=userIds.split(",");
            for(int i=0;i<id_s.length;i++){
                uids.add(Long.parseLong(id_s[i])) ;
            }
            form.setUids(uids);
        }
        PageUtils pageData=tbCertifyService.certPageList(form);
        return R.ok().put("data",pageData);
    }

    @PostMapping("/save")
    public R save(@RequestBody CertRemarkVo vo){
        //TbCertifyEntity certify= DataTypeConversionUtil.map2entity(params,io.cdn.modules.app.entity.TbCertifyEntity.class);
        //TbCertifyEntity s=tbCertifyService.saveCert(certify);
        ValidatorUtils.validateEntity(vo);
        return  tbCertifyService.saveCertRemark(vo);
    }



    @PostMapping("/zero/api/create/cert")
    //@SysLog("zeroSsl API证书申请")
    public R zeroSslCreate(@RequestBody ZeroSslAPiCreateCertForm form){
        ValidatorUtils.validateEntity(form);
        return tbCertifyService.zeroSslApiCreateCert(form);
    }

    @PostMapping("/apply/certificate")
    @SysLog("申请证书")
    public R applyCertificate(@RequestBody CertApplyVo vo){
        ValidatorUtils.validateEntity(vo);
        //logger.info(""+CertSrcTypeEnums.LetsencryptDns.getType()+" "+vo.getUseMode());
//        if (CertSrcTypeEnums.LetsencryptDns.getType()==vo.getUseMode() || CertSrcTypeEnums.CertServerDnsV2.getType()==vo.getUseMode()){
//            return R.error("仅用户端支持该模式证书");
//        }
        return tbCertifyService.applyCertificate(null,vo);
    }

    @PostMapping("/reauth")
    @SysLog("证书重签")
    public R reAuth( @RequestBody CertReIssuedVo params){
        //Integer CertId
        ValidatorUtils.validateEntity(params);

        return tbCertifyService.reIssued(null ,params);
    }

    @PostMapping("/batDelete")
    public R batDelete(@RequestBody Map params){
        if(params.containsKey("ids")){
            String[]  id_s=params.get("ids").toString().split(",");
            Long[] Ids= new Long[id_s.length];
            for(int i=0;i<id_s.length;i++){
                Ids[i] = Long.parseLong(id_s[i]);
            }
            tbCertifyService.batDeleteCert(null,Ids);
            return R.ok();
        }
        return R.error("参数不完整");
    }

    @GetMapping("/get/certify/by/host")
    public R get_cert(@RequestParam  String host){
        return tbCertifyService.getCertifyList(null,host);

    }


    @GetMapping("/detail")
    public R detail( @RequestParam Integer id){
        return tbCertifyService.getCertDetailById(null,id);
    }

}
