package io.ants.modules.sys.controller;

import io.ants.common.annotation.SysLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.sys.form.AdminRechargeForm;
import io.ants.modules.sys.service.PayService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/cdn/pay")
public class PayController extends AbstractController {

    @Autowired
    private PayService payService;


    @SysLog("管理员人工充值")
    @PostMapping("/adminRecharge")
    @RequiresPermissions("generator:order:admin_recharge")
    public R adminRecharge(@RequestBody Map<String, Object> params){
        AdminRechargeForm form= DataTypeConversionUtil.map2entity(params, AdminRechargeForm.class);
        ValidatorUtils.validateEntity(form);
        return payService.adminRecharge(form.getUserId(),form.getAmount(),form.getRemark());
    }


    @GetMapping("/order/detail")
    public R orderDetail(@RequestParam String SerialNumber){
        return payService.getOrderStatus(null,SerialNumber);
    }
}
