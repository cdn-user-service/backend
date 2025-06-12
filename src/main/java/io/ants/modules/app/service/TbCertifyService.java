package io.ants.modules.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.ants.common.utils.PageUtils;
import io.ants.common.utils.R;
import io.ants.modules.app.entity.TbCertifyEntity;
import io.ants.modules.app.form.QueryCertPageForm;
import io.ants.modules.app.vo.ZeroSslAPiCreateCertForm;
import io.ants.modules.sys.vo.CertApplyVo;
import io.ants.modules.sys.vo.CertReIssuedVo;
import io.ants.modules.sys.vo.CertRemarkVo;

public interface TbCertifyService extends IService<TbCertifyEntity> {

    PageUtils certPageList(QueryCertPageForm form);

    TbCertifyEntity saveCert(TbCertifyEntity certify);

    void batDeleteCert(Long userId, Long[] Ids);

    R getCertifyList(Long userId, String host);

    TbCertifyEntity updateCertByAcmeSh(TbCertifyEntity certify);

    R getCertDetailById(Long userId, Integer id);

    R zeroSslApiCreateCert(ZeroSslAPiCreateCertForm form);

    R reIssued(Long userId, CertReIssuedVo params);

    R saveCertRemark(CertRemarkVo vo);

    R applyCertificate(Long userId,CertApplyVo vo);

    R getCertStatistics(Long userId);
}
