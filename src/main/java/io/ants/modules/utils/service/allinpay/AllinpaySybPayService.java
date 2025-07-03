package io.ants.modules.utils.service.allinpay;

import io.ants.common.utils.HttpContextUtils;
import io.ants.common.utils.IPUtils;
import io.ants.modules.utils.config.AllinpayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.TreeMap;

public class AllinpaySybPayService {

	protected static Logger logger = LoggerFactory.getLogger(AllinpaySybPayService.class);

	private static AllinpayConfig conf = new AllinpayConfig();

	private static final String SYB_APIURL = "https://vsp.allinpay.com/apiweb/unitorder";
	private static final String SIGN_TYPE = "MD5";// "SM2";

	public AllinpayConfig getConf() {
		return this.conf;
	}

	public AllinpaySybPayService(AllinpayConfig conf) {
		this.conf = conf;
	}

	public Map<String, String> pay(long amount, String sn, String paytype) throws Exception {
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		String ip = IPUtils.getIpAddr(request);
		double fAmount = (double) amount * conf.getEXCHANGE_RMB();
		return pay(Math.round(fAmount), sn, paytype, "cdn", "cdn", "", "30", "", "", "", "", "", "", "", "", "", "", ip,
				"");
	}

	/**
	 * 
	 * @param trxamt
	 * @param reqsn
	 * @param paytype
	 * @param body
	 * @param remark
	 * @param acct
	 * @param validtime
	 * @param limit_pay
	 * @param idno
	 * @param truename
	 * @param asinfo
	 * @param sub_appid
	 * @param goods_tag    单品优惠信息
	 * @param chnlstoreid
	 * @param subbranch
	 * @param extendparams 具体见接口文档
	 * @param cusip        限云闪付JS支付业务
	 * @param fqnum        限支付宝分期业务
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> pay(long trxamt, String reqsn, String paytype, String body, String remark, String acct,
			String validtime, String limit_pay,
			String idno, String truename, String asinfo, String sub_appid, String goods_tag, String benefitdetail,
			String chnlstoreid, String subbranch, String extendparams, String cusip, String fqnum) throws Exception {
		// System.out.println("url:");
		// System.out.println(conf.getSYB_APIURL()+"/pay");
		HttpConnectionUtil http = new HttpConnectionUtil(SYB_APIURL + "/pay");
		http.init();

		TreeMap<String, String> params = new TreeMap<String, String>();
		if (!SybUtil.isEmpty(conf.getSYB_ORGID())) {
			params.put("orgid", conf.getSYB_ORGID());
		}
		params.put("cusid", conf.getSYB_CUSID());
		params.put("appid", conf.getSYB_APPID());
		params.put("version", "11");
		params.put("trxamt", String.valueOf(trxamt));
		params.put("reqsn", reqsn);
		params.put("paytype", paytype);
		params.put("randomstr", SybUtil.getValidatecode(8));
		params.put("body", body);
		params.put("remark", remark);
		params.put("validtime", validtime);
		params.put("acct", acct);
		params.put("notify_url", conf.getNOTIFY_URL());
		params.put("limit_pay", limit_pay);
		params.put("sub_appid", sub_appid);
		params.put("goods_tag", goods_tag);
		params.put("benefitdetail", benefitdetail);
		params.put("chnlstoreid", chnlstoreid);
		params.put("subbranch", subbranch);
		params.put("extendparams", extendparams);
		params.put("cusip", cusip);
		params.put("fqnum", fqnum);
		params.put("idno", idno);
		params.put("truename", truename);
		params.put("asinfo", asinfo);
		params.put("signtype", SIGN_TYPE);
		String appkey = "";
		if (SIGN_TYPE.equals("RSA")) {
			appkey = conf.getSYB_RSACUSPRIKEY();
		} else if (SIGN_TYPE.equals("SM2")) {
			appkey = conf.getSYB_SM2PPRIVATEKEY();
		} else {
			appkey = conf.getSYB_MD5_APPKEY();
		}
		params.put("sign", SybUtil.unionSign(params, appkey, SIGN_TYPE));
		logger.info(params.toString());
		byte[] bys = http.postParams(params, true);
		String result = new String(bys, "UTF-8");
		Map<String, String> map = handleResult(result);
		return map;

	}

	public Map<String, String> cancel(long trxamt, String reqsn, String oldtrxid, String oldreqsn) throws Exception {
		HttpConnectionUtil http = new HttpConnectionUtil(SYB_APIURL + "/cancel");
		http.init();
		TreeMap<String, String> params = new TreeMap<String, String>();
		if (!SybUtil.isEmpty(conf.getSYB_ORGID()))
			params.put("orgid", conf.getSYB_ORGID());
		params.put("cusid", conf.getSYB_CUSID());
		params.put("appid", conf.getSYB_APPID());
		params.put("version", "11");
		params.put("trxamt", String.valueOf(trxamt));
		params.put("reqsn", reqsn);
		params.put("oldtrxid", oldtrxid);
		params.put("oldreqsn", oldreqsn);
		params.put("randomstr", SybUtil.getValidatecode(8));
		params.put("signtype", SIGN_TYPE);
		String appkey = "";
		if (SIGN_TYPE.equals("RSA"))
			appkey = conf.getSYB_RSACUSPRIKEY();
		else if (SIGN_TYPE.equals("SM2"))
			appkey = conf.getSYB_SM2PPRIVATEKEY();
		else
			appkey = conf.getSYB_MD5_APPKEY();
		params.put("sign", SybUtil.unionSign(params, appkey, SIGN_TYPE));
		byte[] bys = http.postParams(params, true);
		String result = new String(bys, "UTF-8");
		Map<String, String> map = handleResult(result);
		return map;
	}

	public Map<String, String> refund(long trxamt, String reqsn, String oldtrxid, String oldreqsn) throws Exception {
		HttpConnectionUtil http = new HttpConnectionUtil(SYB_APIURL + "/refund");
		http.init();
		TreeMap<String, String> params = new TreeMap<String, String>();
		if (!SybUtil.isEmpty(conf.getSYB_ORGID())) {
			params.put("orgid", conf.getSYB_ORGID());
		}
		params.put("cusid", conf.getSYB_CUSID());
		params.put("appid", conf.getSYB_APPID());
		params.put("version", "11");
		params.put("trxamt", String.valueOf(trxamt));
		params.put("reqsn", reqsn);
		params.put("oldreqsn", oldreqsn);
		params.put("oldtrxid", oldtrxid);
		params.put("randomstr", SybUtil.getValidatecode(8));
		params.put("signtype", SIGN_TYPE);
		String appkey = "";
		if (SIGN_TYPE.equals("RSA"))
			appkey = conf.getSYB_RSACUSPRIKEY();
		else if (SIGN_TYPE.equals("SM2"))
			appkey = conf.getSYB_SM2PPRIVATEKEY();
		else
			appkey = conf.getSYB_MD5_APPKEY();
		params.put("sign", SybUtil.unionSign(params, appkey, SIGN_TYPE));
		byte[] bys = http.postParams(params, true);
		String result = new String(bys, "UTF-8");
		Map<String, String> map = handleResult(result);
		return map;
	}

	public Map<String, String> query(String reqsn, String trxid) throws Exception {
		HttpConnectionUtil http = new HttpConnectionUtil(SYB_APIURL + "/query");
		http.init();
		TreeMap<String, String> params = new TreeMap<String, String>();
		if (!SybUtil.isEmpty(conf.getSYB_ORGID()))
			params.put("orgid", conf.getSYB_ORGID());
		params.put("cusid", conf.getSYB_CUSID());
		params.put("appid", conf.getSYB_APPID());
		params.put("version", "11");
		params.put("reqsn", reqsn);
		params.put("trxid", trxid);
		params.put("randomstr", SybUtil.getValidatecode(8));
		params.put("signtype", SIGN_TYPE);
		String appkey = "";
		if (SIGN_TYPE.equals("RSA"))
			appkey = conf.getSYB_RSACUSPRIKEY();
		else if (SIGN_TYPE.equals("SM2"))
			appkey = conf.getSYB_SM2PPRIVATEKEY();
		else
			appkey = conf.getSYB_MD5_APPKEY();
		params.put("sign", SybUtil.unionSign(params, appkey, SIGN_TYPE));
		byte[] bys = http.postParams(params, true);
		String result = new String(bys, "UTF-8");
		Map<String, String> map = handleResult(result);
		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map<String, String> handleResult(String result) throws Exception {
		logger.info("ret:" + result);
		Map map = SybUtil.json2Obj(result, Map.class);
		if (map == null) {
			throw new Exception("返回数据错误");
		}
		if ("SUCCESS".equals(map.get("retcode"))) {
			TreeMap tmap = new TreeMap();
			tmap.putAll(map);
			String appkey = "";
			if (SIGN_TYPE.equals("RSA"))
				appkey = conf.getSYB_RSATLPUBKEY();
			else if (SIGN_TYPE.equals("SM2"))
				appkey = conf.getSYB_SM2TLPUBKEY();
			else
				appkey = conf.getSYB_MD5_APPKEY();
			if (SybUtil.validSign(tmap, appkey, SIGN_TYPE)) {
				logger.info("签名成功");
				return map;
			} else {
				throw new Exception("验证签名失败");
			}

		} else {
			throw new Exception(map.get("retmsg").toString());
		}
	}

	public Map<String, String> scanPay(long trxamt, String reqsn, String body, String remark, String authcode,
			String limit_pay, String idno, String truename, String asinfo) throws Exception {
		// TODO Auto-generated method stub
		HttpConnectionUtil http = new HttpConnectionUtil(SYB_APIURL + "/scanqrpay");
		http.init();
		TreeMap<String, String> params = new TreeMap<String, String>();
		if (!SybUtil.isEmpty(conf.getSYB_ORGID()))
			params.put("orgid", conf.getSYB_ORGID());
		params.put("cusid", conf.getSYB_CUSID());
		params.put("appid", conf.getSYB_APPID());
		params.put("version", "11");
		params.put("trxamt", String.valueOf(trxamt));
		params.put("reqsn", reqsn);
		params.put("randomstr", SybUtil.getValidatecode(8));
		params.put("body", body);
		params.put("remark", remark);
		params.put("authcode", authcode);
		params.put("limit_pay", limit_pay);
		params.put("asinfo", asinfo);
		params.put("signtype", SIGN_TYPE);
		String appkey = "";
		if (SIGN_TYPE.equals("RSA"))
			appkey = conf.getSYB_RSACUSPRIKEY();
		else if (SIGN_TYPE.equals("SM2"))
			appkey = conf.getSYB_SM2PPRIVATEKEY();
		else
			appkey = conf.getSYB_MD5_APPKEY();
		params.put("sign", SybUtil.unionSign(params, appkey, SIGN_TYPE));
		byte[] bys = http.postParams(params, true);
		String result = new String(bys, "UTF-8");
		Map<String, String> map = handleResult(result);
		return map;
	}

	public void callBackDemo(HttpServletRequest request, HttpServletResponse response) {
		try {
			request.setCharacterEncoding("UTF-8");// 通知传输的编码为GBK
			response.setCharacterEncoding("UTF-8");
			TreeMap<String, String> params = getParams(request);// 动态遍历获取所有收到的参数,此步非常关键,因为收银宝以后可能会加字段,动态获取可以兼容
			try {
				String appkey = "";
				if ("RSA".equals(params.get("signtype"))) {
					appkey = conf.getSYB_RSATLPUBKEY();
				} else if ("SM2".equals(params.get("signtype"))) {
					appkey = conf.getSYB_SM2TLPUBKEY();
				} else {
					appkey = conf.getSYB_MD5_APPKEY();
				}
				boolean isSign = SybUtil.validSign(params, appkey, params.get("signtype"));// 接受到推送通知,首先验签
				logger.info("验签结果:" + isSign);
				// todo 验签完毕进行业务处理
			} catch (Exception e) {// 处理异常
				// TODO: handle exception
				e.printStackTrace();
			} finally {
				// 收到通知,返回success
				response.getOutputStream().write("success".getBytes());
				response.flushBuffer();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TreeMap<String, String> getParams(HttpServletRequest request) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		Map reqMap = request.getParameterMap();
		for (Object key : reqMap.keySet()) {
			String value = ((String[]) reqMap.get(key))[0];
			// System.out.println(key+";"+value);
			map.put(key.toString(), value);
		}
		return map;
	}

}
