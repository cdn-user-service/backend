package io.ants.modules.sys.service;

import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.form.PageSimpleForm;
import io.ants.modules.app.form.QuerySuitBytes;
import io.ants.modules.app.form.SubmitOrderForm;
import io.ants.modules.sys.entity.CdnProductAttrEntity;
import io.ants.modules.sys.entity.CdnProductEntity;
import io.ants.modules.sys.entity.CdnSuitEntity;
import io.ants.modules.sys.form.QueryCdnProductForm;

import java.util.List;
import java.util.Map;

public interface CdnSuitService {


    PageUtils getProductList(QueryCdnProductForm form);

    List<CdnProductEntity> getAllProductByType(String productTypes);

    CdnProductEntity saveProduct(Map<String, Object> params);

    boolean deleteProduct(Integer id);

    PageUtils getProductAttrList(PageSimpleForm form);

    Map getProductAttrObj();

    Object getAllProductAttr();

    CdnProductAttrEntity SaveProductAttr(Map<String, Object> params);

    boolean deleteProductAttr(Integer id);

    R createOrder(SubmitOrderForm form);

    PageUtils orderList(Map params);


    CdnSuitEntity getSuitDetailBySuitId(Integer id);

    /**
     *
     */

    CdnSuitEntity getSuitDetailBySerial  (Long userId, String serialNumber,boolean bindSite,boolean usedInfo);

    PageUtils suitList(Map params);

    R allSuitListByUser(Long userId,Integer all);

    boolean cancellationSuit(Long userId,String serialNumber);

    void liquidationSuit(Long userId,String serialNumber);

    R user_bytes_detail(QuerySuitBytes querySuitBytes);

    Integer updateSuitStatus(String SerialNumber,Integer status );

    List<String> getSuitBind(Long userId,String SerialNumber, Integer mode);

    /**
     * 更新字段
     */
    void reInitDbColumns();


}
