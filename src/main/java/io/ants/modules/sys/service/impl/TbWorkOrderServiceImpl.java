package io.ants.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.ants.modules.app.dao.TbUserDao;
import io.ants.modules.app.entity.TbUserEntity;
import io.ants.modules.sys.dao.TbWorkOrderCategoryDao;
import io.ants.modules.sys.dao.TbWorkOrderListDao;
import io.ants.modules.sys.dao.TbWorkOrderMessageDao;
import io.ants.modules.sys.entity.TbWorkOrderCategoryEntity;
import io.ants.modules.sys.entity.TbWorkOrderListEntity;
import io.ants.modules.sys.entity.TbWorkOrderMessageEntity;
import io.ants.modules.sys.enums.UserTypeEnum;
import io.ants.modules.sys.enums.WorkOrderStatusEnum;
import io.ants.modules.sys.service.TbWorkOrderService;
import io.ants.common.utils.DateUtils;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.common.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Administrator
 */
@Service
public  class TbWorkOrderServiceImpl implements TbWorkOrderService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String wl_sum_key="wl_sum_key";

    @Autowired
    private TbWorkOrderCategoryDao tbWorkOrderCategoryDao;
    @Autowired
    private TbWorkOrderListDao tbWorkOrderListDao;
    @Autowired
    private TbWorkOrderMessageDao tbWorkOrderMessageDao;
    @Autowired
    private TbUserDao tbUserDao;
    @Autowired
    private RedisUtils redisUtils;

    @Override
    public List<TbWorkOrderCategoryEntity> workOrderCategoryList(Integer parentId, Integer status) {
        List<TbWorkOrderCategoryEntity> final_list=new ArrayList<>();
        if (null==parentId){
            List<TbWorkOrderCategoryEntity> list=tbWorkOrderCategoryDao.selectList(new QueryWrapper<TbWorkOrderCategoryEntity>().isNull("parent_id").eq(null!=status,"status",status));
            final_list.addAll(list);
        }else {
            List<TbWorkOrderCategoryEntity> list=tbWorkOrderCategoryDao.selectList(new QueryWrapper<TbWorkOrderCategoryEntity>().eq(null!=parentId,"parent_id",parentId).eq(null!=status,"status",status));
            final_list.addAll(list);
        }
        final_list.forEach(item->{
            Integer count=tbWorkOrderCategoryDao.selectCount(new QueryWrapper<TbWorkOrderCategoryEntity>().eq("parent_id",item.getId()));
            item.setChildCount(count);
        });
        return final_list;
    }

    @Override
    public TbWorkOrderCategoryEntity saveWorkOrderCategory(TbWorkOrderCategoryEntity categoryEntity) {
        if (null==categoryEntity.getId()|| 0==categoryEntity.getId()){
            //create
            tbWorkOrderCategoryDao.insert(categoryEntity);
        }else{
            //update
            tbWorkOrderCategoryDao.updateById(categoryEntity);
        }
        return categoryEntity;
    }

    //获取所有子ID
    private Set<Integer> getAllChildId(Integer pid, Set<Integer> idList){
        List<TbWorkOrderCategoryEntity> ls=tbWorkOrderCategoryDao.selectList(new QueryWrapper<TbWorkOrderCategoryEntity>().eq("parent_id",pid));
        for (TbWorkOrderCategoryEntity category:ls){
            Set<Integer> buf_lst=  getAllChildId(category.getId(),idList);
            idList.addAll(buf_lst);
            idList.add(category.getId());
        }
        return idList;
    }

    @Override
    public void deleteWorkOrderCategory(Integer id) {
        TbWorkOrderCategoryEntity category=tbWorkOrderCategoryDao.selectById(id);
        if (null==category){return;}
        //找其子 ,删除子
        Set<Integer> setls = new HashSet<Integer>();
        setls.add(id);
        setls.addAll(this.getAllChildId(id,setls));
        for (Integer cid:setls){
            //logger.debug("==>"+cid);
            tbWorkOrderCategoryDao.deleteById(cid);
        }
    }

    /**
     * 关闭过期工单
     */
    private void closeWorkOrderByTimeOut(){
        Date befDate= DateUtils.addDateDays(new Date(),-7);
        Integer[] statusList={1,2,3};
        List<TbWorkOrderListEntity>ls=tbWorkOrderListDao.selectList(new QueryWrapper<TbWorkOrderListEntity>()
                .notIn("status",statusList)
                .le("createdate",befDate)
        );
        //对14天前的工单，最后一个回复是管理，关闭工单
        for (TbWorkOrderListEntity entity:ls){
            TbWorkOrderMessageEntity messageEntity=tbWorkOrderMessageDao.selectOne(new QueryWrapper<TbWorkOrderMessageEntity>().eq("work_order_id",entity.getId()).orderByDesc("id").last("limit 1"));
            if (null!=messageEntity){
                if (UserTypeEnum.MANAGER_TYPE.getId().equals(messageEntity.getSenderType())  && messageEntity.getCreatedate().before(befDate) ){
                    entity.setStatus(WorkOrderStatusEnum.DISABLE.getId());
                    tbWorkOrderListDao.updateById(entity);
                    TbWorkOrderMessageEntity endMessageEntity=new TbWorkOrderMessageEntity();
                    endMessageEntity.setWorkOrderId(entity.getId());
                    endMessageEntity.setSenderType(UserTypeEnum.MANAGER_TYPE.getId());
                    endMessageEntity.setSenderId(messageEntity.getSenderId());
                    endMessageEntity.setContent("{\"text\":\"工单超时，系统自动结单！\",\"img\":\"\"}");
                    tbWorkOrderMessageDao.insert(endMessageEntity);
                }
            }
        }
    }

    @Override
    public PageUtils WOL_PageList(Long userId, Date startDate, Date endDate, Integer urgentLevel, Integer Status, Integer page, Integer limit) {
        this.closeWorkOrderByTimeOut();
        IPage<TbWorkOrderListEntity> ipage=tbWorkOrderListDao.selectPage(
                new Page<TbWorkOrderListEntity>(page,limit),
                new QueryWrapper<TbWorkOrderListEntity>()
                        .eq(null!=userId,"user_id",userId)
                        .eq(null!=urgentLevel,"urgent_level",urgentLevel)
                        .ge(null!=startDate,"createdate",startDate)
                        .le(null!=endDate,"createdate",endDate)
                        .eq(null!=Status,"status",Status)
                        .orderByDesc("id")
        );
        ipage.getRecords().forEach(item->{
            TbWorkOrderMessageEntity lastEntity=tbWorkOrderMessageDao.selectOne(new QueryWrapper<TbWorkOrderMessageEntity>().eq("work_order_id",item.getId()).orderByDesc("id").last("limit 1"));
            if (null==lastEntity){
                item.setLastSenderType(UserTypeEnum.MANAGER_TYPE.getId());
                item.setLastSubmitDate(item.getCreatedate());
            }else {
                item.setLastSenderType(lastEntity.getSenderType());
                item.setLastSubmitDate(lastEntity.getCreatedate());
            }
            if (null!=item.getUserId()){
                TbUserEntity user=tbUserDao.selectById(item.getUserId());
                if (null!=user){
                    item.setUserName(user.getUsername());
                }
            }
            TbWorkOrderCategoryEntity category=tbWorkOrderCategoryDao.selectById(item.getCategoryId());
            item.setCategoryObj(category);
        });
        return new PageUtils(ipage);
    }

    @Override
    public TbWorkOrderListEntity save_WOL(TbWorkOrderListEntity listEntity) {
        if (null==listEntity.getId()|| 0==listEntity.getId()){
            //create
            listEntity.setStatus(0);
            tbWorkOrderListDao.insert(listEntity);
        }else{
            //update
            tbWorkOrderListDao.updateById(listEntity);
        }
        redisUtils.longSet(wl_sum_key,listEntity.getId());
        return listEntity;
    }

    @Override
    public TbWorkOrderListEntity change_wol_status(Long userId, Integer id, Integer status, Integer score) {
        TbWorkOrderListEntity wol= tbWorkOrderListDao.selectOne(new QueryWrapper<TbWorkOrderListEntity>().eq("user_id",userId).eq("id",id).last("limit 1"));
        if (null==wol){return null;}
        boolean op_status=false;
        switch (status){
            case 1:
                if (true){
                    if (0==wol.getStatus()){
                        wol.setStatus(1);
                        op_status=true;
                        tbWorkOrderListDao.updateById(wol);
                    }
                }
                break;
            case 2:
                if (true){
                    if (1==wol.getStatus()){
                        wol.setStatus(2);
                        op_status=true;
                        tbWorkOrderListDao.updateById(wol);
                    }
                }
                break;
            case 3:
                if (true){
                    wol.setStatus(3);
                    op_status=true;
                    tbWorkOrderListDao.updateById(wol);
                }
                break;
            default:
                break;

        }
        if(3==wol.getStatus() && null !=score){
            wol.setScore(score);
            op_status=true;
            tbWorkOrderListDao.updateById(wol);
        }
        if(!op_status){
            return null;
        }
        return wol;
    }



    @Override
    public TbWorkOrderListEntity WOL_detail(Long userId,Integer id) {
        TbWorkOrderListEntity wol=tbWorkOrderListDao.selectOne(new QueryWrapper<TbWorkOrderListEntity>().eq(null!=userId,"user_id",userId).eq("id",id).last("limit 1"));
        if (null==wol){return null;}

        if (null!=wol.getUserId()){
            TbUserEntity user=tbUserDao.selectById(wol.getUserId());
            if (null!=user){
                wol.setUserName(user.getUsername());
            }
        }
        TbWorkOrderCategoryEntity category=tbWorkOrderCategoryDao.selectById(wol.getCategoryId());
        wol.setCategoryObj(category);

        List<TbWorkOrderMessageEntity> ls= tbWorkOrderMessageDao.selectList(new QueryWrapper<TbWorkOrderMessageEntity>().eq("work_order_id",id));
        wol.setSenderInfos(ls);
        if (ls.size()>0){
            TbWorkOrderMessageEntity lastEntity=ls.get(ls.size()-1);
            wol.setLastSenderType(lastEntity.getSenderType());
            wol.setLastSubmitDate(lastEntity.getCreatedate());
        }

        return wol;
    }

    @Override
    public void batDelete_WOL(Long userId,String ids) {
        for (String id:ids.split(",")){
            TbWorkOrderListEntity wol=tbWorkOrderListDao.selectById(id);
            if (null==userId || 0==userId){
                 tbWorkOrderListDao.deleteById(id);
            }else {
                if (null!=wol && wol.getUserId().equals(userId)){
                    tbWorkOrderListDao.deleteById(id);
                }
            }
        }
    }

    @Override
    public TbWorkOrderMessageEntity sendWorkOrderMessage(TbWorkOrderMessageEntity senderEntity) {
        redisUtils.longSet(wl_sum_key,senderEntity.getWorkOrderId());
        tbWorkOrderMessageDao.insert(senderEntity);
        return  senderEntity;
    }

    @Override
    public R getWolNewMsg() {
        String sum=redisUtils.get(wl_sum_key);
        redisUtils.longSet(wl_sum_key,"0");
        return R.ok().put("newSum",sum);
    }


}