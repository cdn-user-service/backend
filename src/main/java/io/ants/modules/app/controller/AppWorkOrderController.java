package io.ants.modules.app.controller;

import io.ants.common.annotation.UserLog;
import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.DateUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.annotation.Login;
import io.ants.modules.app.form.CreateWorkOrder;
import io.ants.modules.sys.entity.TbWorkOrderCategoryEntity;
import io.ants.modules.sys.entity.TbWorkOrderListEntity;
import io.ants.modules.sys.entity.TbWorkOrderMessageEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.service.TbWorkOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app/work/order/")
@Api(tags = "APP 工单管理")
public class AppWorkOrderController {

    @Autowired
    private TbWorkOrderService tbWorkOrderService;

    @Login
    @GetMapping("/category/list")
    @ApiOperation("工单分类列表")
    public R category_list(@RequestParam Integer parentId){
        List<TbWorkOrderCategoryEntity> ls= tbWorkOrderService.workOrderCategoryList(parentId,null);
        return R.ok().put("data",ls);
    }

    @Login
    @PostMapping("/wol/list")
    @ApiOperation("工单列表")
    public R wol_list(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody Map param){
        Integer urgentLevel=null;
        Integer status=null;
        Integer page=1;
        Integer limit=20;
        Date startDate=null;
        Date endDate=null;
        if (param.containsKey("urgentLevel") && StringUtils.isNotBlank(param.get("urgentLevel").toString())){
            urgentLevel=Integer.parseInt(param.get("urgentLevel").toString());
        }
        if (param.containsKey("status") && StringUtils.isNotBlank(param.get("status").toString())){
            status=Integer.parseInt(param.get("status").toString());
        }
        if (param.containsKey("page") && StringUtils.isNotBlank(param.get("page").toString())){
            page=Integer.parseInt(param.get("page").toString());
        }
        if (param.containsKey("limit") && StringUtils.isNotBlank(param.get("limit").toString())){
            limit=Integer.parseInt(param.get("limit").toString());
        }
        if (param.containsKey("start_date") && StringUtils.isNotBlank(param.get("start_date").toString())){
            startDate = DateUtils.stringToDate(param.get("start_date").toString(),DateUtils.DATE_TIME_PATTERN) ;
        }
        if (param.containsKey("end_date") && StringUtils.isNotBlank(param.get("end_date").toString())){
            endDate = DateUtils.stringToDate(param.get("end_date").toString(),DateUtils.DATE_TIME_PATTERN) ;
        }
        return R.ok().put("data",tbWorkOrderService.WOL_PageList(userId,startDate,endDate,urgentLevel,status,page,limit));
    }

    @Login
    @PostMapping("/wol/save")
    @ApiOperation("创建工单")
    @UserLog("创建工单")
    public R wol_save(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody CreateWorkOrder form){
        Map wol_map= DataTypeConversionUtil.entity2map(form);
        if (null==wol_map){
            return R.error("param is empty!");
        }
        TbWorkOrderListEntity wol=DataTypeConversionUtil.map2entity(wol_map, TbWorkOrderListEntity.class);
        wol.setUserId(userId);
        wol.setStatus(0);
        return R.ok().put("data",tbWorkOrderService.save_WOL(wol));
    }

    @Login
    @GetMapping("/wol/detail")
    @ApiOperation("工单详情")
    public R wol_detail(@ApiIgnore @RequestAttribute("userId") Long userId,Integer id){
        return R.ok().put("data",tbWorkOrderService.WOL_detail(userId,id));
    }

    @Login
    @GetMapping("/wol/change/status")
    @ApiOperation("修改工单状态")
    @UserLog("修改工单状态")
    public R wol_change_status(@ApiIgnore @RequestAttribute("userId") Long userId,Integer id,Integer status,Integer score){
        if (null==id ){
            return R.error("id is empty!");
        }
        if (null==status ){
            return R.error("status is empty!");
        }
        TbWorkOrderListEntity wol= tbWorkOrderService.change_wol_status(userId,id,status,score);
        if(null!=wol){
            return R.ok().put("data",wol);
        }
        return R.error("修改失败！");
    }

    @Login
    @PostMapping("/wol/bat/delete")
    @ApiOperation("删除工单")
    @UserLog("删除工单")
    public R wol_delete(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody Map param){
        if (param.containsKey("ids") && StringUtils.isNotBlank(param.get("ids").toString())){
            String ids=param.get("ids").toString();
            tbWorkOrderService.batDelete_WOL(userId,ids);
        }
        return R.ok();
    }

    @Login
    @PostMapping("/wo/send/message")
    @ApiOperation("回复工单")
    @UserLog("回复工单")
    public R wo_send_message(@ApiIgnore @RequestAttribute("userId") Long userId,@RequestBody Map param){
        if (!param.containsKey("workOrderId") ){
            return R.error("[workOrderId]缺失！");
        }
        if ( !param.containsKey("content")){
            return R.error("[content]缺失！");
        }
        TbWorkOrderMessageEntity wo_message= DataTypeConversionUtil.map2entity(param, TbWorkOrderMessageEntity.class);
        wo_message.setSenderType(UserTypeEnum.USER_TYPE.getId());
        wo_message.setSenderId(userId);
        tbWorkOrderService.sendWorkOrderMessage(wo_message);
        return R.ok();
    }
}
