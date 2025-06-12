package io.ants.modules.sys.controller;


import io.ants.common.utils.DataTypeConversionUtil;
import io.ants.common.utils.DateUtils;
import io.ants.common.utils.R;
import io.ants.modules.sys.entity.TbWorkOrderCategoryEntity;
import io.ants.modules.sys.entity.TbWorkOrderListEntity;
import io.ants.modules.sys.entity.TbWorkOrderMessageEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.service.TbWorkOrderService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/tb/work/order/")
public class TbWorkOrderController extends AbstractController{


    @Autowired
    private TbWorkOrderService tbWorkOrderService;


    @GetMapping("/category/list")
    public R categoryList(@RequestParam Integer parentId){
        List<TbWorkOrderCategoryEntity> ls= tbWorkOrderService.workOrderCategoryList(parentId,null);
        return R.ok().put("data",ls);
    }

    @PostMapping("/category/save")
    public R categorySave(@RequestBody Map param){
        TbWorkOrderCategoryEntity category= DataTypeConversionUtil.map2entity(param, TbWorkOrderCategoryEntity.class);
        return R.ok().put("data",tbWorkOrderService.saveWorkOrderCategory(category));
    }

    @GetMapping("/category/delete")
    public R categoryDeletes(Integer id){
        tbWorkOrderService.deleteWorkOrderCategory(id);
       return R.ok();
    }

    @PostMapping("/wol/list")
    public R wolList(@RequestBody Map param){
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
        return R.ok().put("data",tbWorkOrderService.WOL_PageList(null,startDate,endDate,urgentLevel,status,page,limit));
    }

    @PostMapping("/wol/save")
    public R wolSave(@RequestBody Map param){
        TbWorkOrderListEntity wol=DataTypeConversionUtil.map2entity(param, TbWorkOrderListEntity.class);
        return R.ok().put("data",tbWorkOrderService.save_WOL(wol));
    }

    @GetMapping("/wol/detail")
    public R wolDetail(Integer id){
        return R.ok().put("data",tbWorkOrderService.WOL_detail(null,id));
    }


    @PostMapping("/wol/bat/delete")
    public R wolDelete(@RequestBody Map param){
        if (param.containsKey("ids") && StringUtils.isNotBlank(param.get("ids").toString())){
            String ids=param.get("ids").toString();
            tbWorkOrderService.batDelete_WOL(null,ids);
        }
        return R.ok();
    }

    @PostMapping("/wol/send/message")
    public R woSendMessage(@RequestBody Map param){
        TbWorkOrderMessageEntity wo_message=DataTypeConversionUtil.map2entity(param, TbWorkOrderMessageEntity.class);
        wo_message.setSenderType(UserTypeEnum.MANAGER_TYPE.getId());
        wo_message.setSenderId(getSysUserId());
        tbWorkOrderService.sendWorkOrderMessage(wo_message);
        return R.ok();
    }


    @GetMapping("/wol/new/msg")
    public R getWolNewMsg(){
        //int newSum= StaticVariableUtils.NEW_WO_SUM;
        //StaticVariableUtils.NEW_WO_SUM=0;
        //
        return tbWorkOrderService.getWolNewMsg();
    }

}


