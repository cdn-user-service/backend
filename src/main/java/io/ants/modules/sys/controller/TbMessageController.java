package io.ants.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.ants.common.utils.Constant;
import io.ants.common.utils.PageUtils;

import io.ants.common.utils.R;
import io.ants.common.validator.ValidatorUtils;
import io.ants.modules.app.dao.TbMessageDao;
import io.ants.modules.app.dao.TbMessageRelationDao;
import io.ants.modules.app.entity.TbMessageEntity;
import io.ants.modules.app.entity.TbMessageRelationEntity;
import io.ants.modules.app.form.MessageForm;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/sys/message")
public class TbMessageController extends AbstractController {

    @Autowired
    private TbMessageDao tbMessageDao;

    @Autowired
    private TbMessageRelationDao tbMessageRelationDao;


    /**
     * 获取消息列表
     * @param params
     * @return
     */
    @PostMapping("/GetMessageList")
    public R getMessageList(@RequestBody Map<String, Object> params){
        String title=(String)params.get("title");
        long curPage = 1;
        long limit = 10;
        if(params.get(Constant.PAGE) != null){
            curPage = Long.parseLong((params.get(Constant.PAGE).toString()));
        }
        if(params.get(Constant.LIMIT) != null){
            limit = Long.parseLong(params.get(Constant.LIMIT).toString());
        }
        IPage<TbMessageEntity> page = tbMessageDao.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(curPage,limit),
                new QueryWrapper<TbMessageEntity>()
                        .orderByDesc("id")
                        .like(StringUtils.isNotBlank(title),"title",title)
        );
        return R.ok().put("data", new PageUtils(page));
    }


    /**
     * 创建|修改消息
     * @param form
     * @return
     */
    @PostMapping("/SaveMessage")
    public R saveMessage(@RequestBody MessageForm form){
        ValidatorUtils.validateEntity(form);
        if(null==form.getId() || 0==form.getId()){
            //创建消息
            TbMessageEntity m=new TbMessageEntity();
            if(2==form.getType()){
                //公告消息
                m.setType(2);
                m.setTitle(form.getTitle());
                m.setContent(form.getContent());
                m.setStatus(form.getStatus());
                tbMessageDao.insert(m);
            }
            else if(1==form.getType()){
                //站内消息
                if(0==form.getSendType() && null!=form.getSendObj()){
                    //指定user list-->向messagerelation保存未读记录
                    m.setType(1);
                    m.setSendType(0);
                    String s_ojbs=form.getSendObj();
                    if(!",".equals(s_ojbs.substring(0,1))){
                        s_ojbs=","+s_ojbs;
                    }
                    if(!",".equals(s_ojbs.substring(s_ojbs.length()-1))){
                        s_ojbs=s_ojbs+",";
                    }
                    m.setSendObj(s_ojbs);
                    m.setTitle(form.getTitle());
                    m.setContent(form.getContent());
                    m.setStatus(form.getStatus());
                    tbMessageDao.insert(m);
                    Integer messageId=m.getId();
                    String[] userIdS=form.getSendObj().split(",");
                    for(String userId:userIdS){
                        if(StringUtils.isNotBlank(userId)){
                            TbMessageRelationEntity mr=new TbMessageRelationEntity();
                            mr.setMessageId(messageId);
                            mr.setUserId(Long.parseLong(userId));
                            mr.setReadStatus(0);
                            tbMessageRelationDao.insert(mr);
                        }
                    }
                }else if(1==form.getSendType() && null!=form.getSendObj()){
                    //指定GROUP LIST  暂无分组
                    // TODO: 2021/11/18
                }else if(2==form.getSendType()){
                    //指定所有
                    m.setType(1);
                    m.setSendType(2);
                    m.setTitle(form.getTitle());
                    m.setContent(form.getContent());
                    m.setStatus(form.getStatus());
                    tbMessageDao.insert(m);
                }
            }
        }else {
            TbMessageEntity m=tbMessageDao.selectById(form.getId());
            if(null!=m){
                m.setType(form.getType());
                m.setSendType(form.getSendType());
                m.setSendObj(form.getSendObj());
                m.setTitle(form.getTitle());
                m.setContent(form.getContent());
                m.setStatus(form.getStatus());
                tbMessageDao.updateById(m);
            }
        }

        return R.ok();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") String id){
        TbMessageEntity messageEntity = tbMessageDao.selectById(id);
        return R.ok().put("data", messageEntity);
    }

    /**
     * 删除消息
     * @param ids
     * @return
     */
    @PostMapping("delete")
    public R deleteMessage(@RequestBody   Integer[] ids){
        Integer data_count=ids.length;
        Integer success_count=0;
        for(Integer id:ids){
            System.out.println(id);
            tbMessageRelationDao.delete( new QueryWrapper<TbMessageRelationEntity>().eq("message_id",id));
            int rt= tbMessageDao.deleteById(id);
            if(1==rt){
                success_count++;
            }

        }
        Map<String,Object> map=new HashMap<>();
        map.put("success_count",success_count);
        map.put("data_count",data_count);
        return R.ok().put("data",map);
    }

}
