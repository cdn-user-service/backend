package io.ants.modules.sys.controller;

import io.ants.common.utils.R;
import io.ants.modules.sys.enums.PreciseWafParamEnum;
import io.ants.modules.sys.enums.PublicEnum;
import io.ants.modules.sys.service.CdnPublicAttrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/sys/cdn/public/")
public class CdnPublicController  extends AbstractController{



    @Autowired
    private CdnPublicAttrService cdnPublicAttrService;


    @GetMapping("/nginx/attr/enum")
    public R attrEnum(){
        Map map=new HashMap();
        for (String s : PublicEnum.allGroup()) {
            map.put(s,PublicEnum.getAllByGroupName(s));
        }
        return  R.ok().put("data", map).put("PreciseWaf", PreciseWafParamEnum.getAllMap());
    }



    @PostMapping("/nginx/attr/save")
    public R attrSave(@RequestBody Map<String,Object> map){
        return cdnPublicAttrService.pubAttrSave(map);
    }

    @PostMapping("/nginx/attr/status/change")
    public R statusChange(@RequestBody Map<String,String> params){
        return cdnPublicAttrService.statusChange(params);

    }

    @PostMapping("/nginx/attr/weight/change")
    public R weightChange(@RequestBody Map<String,String> params){
        return  cdnPublicAttrService.changeWeight(params);

    }

    @PostMapping("/nginx/attr/delete")
    public R attrDelete(@RequestBody Map<String,String> map){
        return  cdnPublicAttrService.deletePubAttr(map);

    }

    @PostMapping("/nginx/attr/key/detail")
    public R attrKey(@RequestBody Map map){
        return  cdnPublicAttrService.getPubKeyDetail(map);
    }

    @PostMapping("/nginx/attr/list")
    public R attrList(@RequestBody Map<String,String> map){
        return cdnPublicAttrService.pubAttrList(map);
    }



}
