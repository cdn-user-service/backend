package io.ants.modules.app.controller;

import io.ants.common.annotation.UserLog;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.form.QueryCertPageForm;
import io.ants.modules.app.service.TbCertifyService;
import io.ants.modules.app.vo.ZeroSslAPiCreateCertForm;
import io.ants.modules.sys.vo.CertApplyVo;
import io.ants.modules.sys.vo.CertReIssuedVo;
import io.ants.modules.sys.vo.CertRemarkVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/certify/")
@Tag(name = "APP证书接口")
public class AppCertifyController {

    @Autowired
    private TbCertifyService tbCertifyService;

    @Login
    @GetMapping("/statistics")
    @Operation(summary = "证书统计")
    public R statistics(@Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        return tbCertifyService.getCertStatistics(userId);
    }

    @Login
    @PostMapping("/list")
    @Operation(summary = "证书列表")
    public R list(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody QueryCertPageForm form) {
        // Long[] userIds, Integer page, Integer limit, String key
        List<Long> uids = new ArrayList<>();
        uids.add(userId);
        form.setUids(uids);
        PageUtils pageData = tbCertifyService.certPageList(form);
        return R.ok().put("data", pageData);
    }

    @Login
    @PostMapping("/save")
    @Operation(summary = "证书保存")
    @UserLog("证书保存")
    public R save(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody CertRemarkVo vo) {
        ValidatorUtils.validateEntity(vo);
        vo.setUserId(userId);
        return tbCertifyService.saveCertRemark(vo);
    }

    @Login
    @PostMapping("/zero/api/create/cert")
    @Operation(summary = "zeroSsl API证书申请")
    @UserLog("zeroSsl API证书申请")
    public R zeroSslCreate(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody ZeroSslAPiCreateCertForm form) {
        form.setUserId(userId);
        ValidatorUtils.validateEntity(form);
        return tbCertifyService.zeroSslApiCreateCert(form);
    }

    @Login
    @GetMapping("/detail")
    @Operation(summary = "证书详情")
    @UserLog("证书详情")
    public R detail(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestParam Integer id) {
        return tbCertifyService.getCertDetailById(userId, id);
    }

    @Login
    @PostMapping("/batDelete")
    @Operation(summary = "证书删除")
    @UserLog("证书删除")
    public R batDelete(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestBody Map params) {
        if (params.containsKey("ids")) {
            String[] id_s = params.get("ids").toString().split(",");
            Long[] Ids = new Long[id_s.length];
            for (int i = 0; i < id_s.length; i++) {
                Ids[i] = Long.parseLong(id_s[i]);
            }
            tbCertifyService.batDeleteCert(userId, Ids);
            return R.ok();
        }
        return R.error("参数不完整");
    }

    @Login
    @PostMapping("/reauth")
    @Operation(summary = "证书重签")
    @UserLog("证书重签")
    public R reAuth(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody CertReIssuedVo params) {
        // Integer CertId
        ValidatorUtils.validateEntity(params);
        return tbCertifyService.reIssued(userId, params);
    }

    @Login
    @PostMapping("/apply/certificate")
    @Operation(summary = "申请证书")
    @UserLog("申请证书")
    public R applyCertificate(@Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody CertApplyVo vo) {
        ValidatorUtils.validateEntity(vo);
        return tbCertifyService.applyCertificate(userId, vo);
    }

    @Login
    @GetMapping("/get/certify/by/host")
    public R get_cert(@Parameter(hidden = true) @RequestAttribute("userId") Long userId, @RequestParam String host) {
        return tbCertifyService.getCertifyList(userId, host);
    }
}
