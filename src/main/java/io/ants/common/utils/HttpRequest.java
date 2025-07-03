package io.ants.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.ants.modules.app.form.RequestVo;
import io.ants.modules.sys.entity.TbCdnPublicMutAttrEntity;
import io.ants.modules.sys.enums.PublicEnum;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * @author Administrator
 */
public class HttpRequest {
    // 请求超时时间,这个时间定义了socket读数据的超时时间，也就是连接到服务器之后到从服务器获取响应数据需要等待的时间,发生超时，会抛出SocketTimeoutException异常。
    final static int SOCKET_TIME_OUT = 30000;
    // 连接超时时间,这个时间定义了通过网络与服务器建立连接的超时时间，也就是取得了连接池中的某个连接之后到接通目标url的连接等待时间。发生超时，会抛出ConnectionTimeoutException异常
    final static int CONNECT_TIME_OUT = 30000;
    static Certificate certificate = null;

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url
     *              发送请求的URL
     * @param param
     *              请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            // 建立实际的连接前，设置超时时间
            connection.setConnectTimeout(10 * 000);
            connection.setReadTimeout(10 * 1000);
            connection.connect();
            // 获取所有响应头字段
            /*
             * Map<String, List<String>> map = connection.getHeaderFields();
             * // 遍历所有的响应头字段
             * for (String key : map.keySet()) {
             * System.out.println(key + "--->" + map.get(key));
             * }
             */
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            in.close();
        } catch (ConnectException e) {
            // e.printStackTrace();
            // System.out.println(e.getMessage());
        } catch (Exception e) {
            // System.out.println("发送GET请求出现异常！" + e);
            // e.printStackTrace();
            // System.out.println(e.getMessage());
        }

        return result.toString();
    }

    public static String sendGetByUrl(String url) {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String urlNameString = url;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            // 建立实际的连接前，设置超时时间
            connection.setConnectTimeout(10 * 000);
            connection.setReadTimeout(10 * 1000);
            connection.connect();
            // 获取所有响应头字段
            /*
             * Map<String, List<String>> map = connection.getHeaderFields();
             * // 遍历所有的响应头字段
             * for (String key : map.keySet()) {
             * System.out.println(key + "--->" + map.get(key));
             * }
             */
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            in.close();
        } catch (ConnectException e) {
            // e.printStackTrace();
            // System.out.println(e.getMessage());
            result.append(e.getMessage());
        } catch (Exception e) {
            // System.out.println("发送GET请求出现异常！" + e);
            // e.printStackTrace();
            // System.out.println(e.getMessage());
            result.append(e.getMessage());
        }
        return result.toString();
    }

    public static String sendPostJsonStr(String httpsURL, String jsonStr) {
        String eMsg = "";
        try {
            // 1. 创建URL对象
            URL url = new URL(httpsURL);

            // 2. 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 3. 设置请求方法为POST，设置请求头
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // 4. 设置请求体，并发送请求
            String requestBody = jsonStr;// "{\"key1\":\"value1\",\"key2\":\"value2\"}";
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();
            os.close();

            // 5. 获取响应状态码
            int responseCode = connection.getResponseCode();

            // 6. 读取响应数据
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 7. 打印响应数据
            // System.out.println("Response Code : " + responseCode);
            // System.out.println("Response Data : " + response.toString());
            return response.toString();
        } catch (Exception e) {
            eMsg = String.format("%s,%s", httpsURL, e.getMessage());
            e.printStackTrace();
        }
        return eMsg;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url
     *              发送请求的 URL
     * @param param
     *              请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        return result.toString();
    }

    public static String curlHttpGet(String url) {
        StringBuilder builder = new StringBuilder();
        try {
            String[] cmds = { "curl", url };
            ProcessBuilder process = new ProcessBuilder(cmds);
            Process p;
            p = process.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                // System.out.println(line);
                // builder.append(System.getProperty("line.separator"));
            }
            reader.close();
            // System.out.println(line);
        } catch (IOException e) {
            builder.append(e.getMessage());
            // e.printStackTrace();
        }
        return builder.toString();
    }

    public static String execCurl(String[] cmds) {
        StringBuilder builder = new StringBuilder();
        try {
            ProcessBuilder process = new ProcessBuilder(cmds);
            Process p;
            p = process.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();

    }

    public static String okHttpPost(String url, String paramJsonString) {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/json; charset=UTF-8");
            RequestBody body = RequestBody.create(mediaType, paramJsonString);
            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-type", "application/json")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static R sendFormPostRequest(String url, String postData) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String eMsg = "";
        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");

            // 设置请求头
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            // 设置POST参数
            byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
            conn.setDoOutput(true);
            try (OutputStream outputStream = conn.getOutputStream()) {
                outputStream.write(postDataBytes, 0, postDataBytes.length);
            }

            // 获取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return R.ok().put("data", response.toString());
            } else {
                eMsg = "请求失败，响应代码：" + responseCode;
                // throw new IOException();
            }
        } catch (Exception e) {
            eMsg = e.getMessage();
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close(); // 关闭BufferedReader
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                conn.disconnect(); // 关闭连接
            }
        }
        return R.error(eMsg);
    }

    public static boolean isHttpUrl(String url) {
        String url_path_pattern = "^http[s]?://[^/]*.*";
        // String server_pattern = "://[^/]*";
        Pattern r = Pattern.compile(url_path_pattern);
        Matcher m = r.matcher(url);
        return m.matches();
    }

    public static String getServeByHttpUrl(String url) {
        String server_pattern = "://[^/]*";
        Pattern r = Pattern.compile(server_pattern);
        Matcher m = r.matcher(url);
        // System.out.println(m);
        if (m.find()) {
            String fs = m.group();
            return fs.replace("://", "");
        }
        return "";
    }

    public static String getUrlArgs(String url) {
        String server = getServeByHttpUrl(url);
        if (StringUtils.isNotBlank(server)) {
            int i = url.indexOf(server) + server.length();
            return url.substring(i);
        }
        return "/";
    }

    public static String getSchemeByServer(String serverName) {
        String ret = "http";
        if (StringUtils.isBlank(serverName)) {
            return ret;
        }
        try {
            String url = String.format("https://%s", serverName);
            String ret_str = HttpRequest.curlHttpGet(url);
            if (StringUtils.isNotBlank(ret_str)) {
                return "https";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static boolean isNormalHttp(String url) {
        boolean r = false;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            // System.out.println(""+response.code());
            if (response.code() < 400) {
                r = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    public static boolean isPortOpen(String ip, int port) {
        try (Socket socket = new Socket(ip, port)) {
            // 如果能够成功连接指定ip和端口，则返回true
            return true;
        } catch (Exception ex) {
            // 出现异常说明无法连接指定ip和端口，返回false
            return false;
        }
    }

    public static boolean isNormalReturnAcmeAccount(String domain) {
        String url = domain + "/.well-known/acme-challenge/acme_rdn_code";
        boolean r = false;
        try {
            String res = sendGet(url, null);
            r = res.startsWith("acme_rdn_code.");
        } catch (Exception e) {
            // System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {

        }
        return r;
    }

    public static boolean downloadFile(String fileUrl, String saveDir, String fileName) {
        boolean result = false;
        // String fileUrl = "https://example.com/file.zip";
        // String saveDir = "/path/to/save/directory";
        // String fileName = "file.zip";
        int bufferSize = 4096;

        try {
            // 创建目录
            File directory = new File(saveDir);
            directory.mkdirs();

            URL url = new URL(fileUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // 判断是否成功连接到文件 URL
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 获取文件大小
                int contentLength = httpConn.getContentLength();

                // 打开输入流
                InputStream inputStream = httpConn.getInputStream();

                // 打开输出流
                FileOutputStream outputStream = new FileOutputStream(saveDir + File.separator + fileName);

                // 缓冲区
                byte[] buffer = new byte[bufferSize];
                int bytesRead = -1;
                int totalBytesRead = 0;

                // 读取文件并写入输出流
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                // 关闭输入输出流
                outputStream.close();
                inputStream.close();

                // 判断文件是否下载完成
                if (totalBytesRead == contentLength) {
                    // System.out.println("文件下载完成");
                    result = true;
                } else {
                    System.out.println("文件下载未完成");
                }
            } else {
                System.out.println("无法连接到文件 URL");
            }
            httpConn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String erkHttp(String method, String uri, String password, String queryData) {
        String ret = "";
        HttpURLConnection httpConn = null;
        try {
            // URL url = new
            // URL("http://121.62.60.60:9200/sample/_search?filter_path=**.hits._source.message");
            URL url = new URL(uri);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod(method);
            httpConn.setRequestProperty("Content-Type", "application/json");
            // byte[] message = ("elastic:8881130py").getBytes("UTF-8");
            byte[] message = ("elastic:" + password).getBytes("UTF-8");
            String basicAuth = Base64.getEncoder().encodeToString(message);
            httpConn.setRequestProperty("Authorization", "Basic " + basicAuth);
            httpConn.setDoOutput(true);

            try (OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream())) {
                writer.write(queryData);
                writer.flush();
            }

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();

            try (Scanner s = new Scanner(responseStream).useDelimiter("\\A")) {
                String response = s.hasNext() ? s.next() : "";
                ret = response;
            }
        } catch (Exception e) {
            ret = e.getMessage();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return ret;
    }

    public static String erkHttpS(String uri, String password, String crtPath, String queryData) {
        String ret = "";
        // Load Certificate
        // curl --cacert /etc/elasticsearch/certstp_ca.crt -k -u
        // elastic:Omg3GxT=MGMkkWLa-+sd https://localhost:9200/sample/_search -d'{
        // "query": { "match": {"decode_data.domain": "test.com"} }}'
        StringBuilder result = new StringBuilder();
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            // FakeX509TrustManager.allowAllSSL();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            // 这里的路径为证书存放路径
            // String crtPath="";
            // String os = System.getProperty("os.name");
            // //Windows操作系统
            // if (os != null && os.toLowerCase().startsWith("windows")) {
            // // crtPath="D:\\ants\\dns\\ants-fast-master\\target\\http_ca.crt";
            // } else if (os != null && os.toLowerCase().startsWith("linux")) {
            // //Linux操作系统
            // // crtPath="/etc/elasticsearch/certs/http_ca.crt";
            // } else { //其它操作系统
            // // System.out.println(String.format("当前系统版本是:%s", os));
            // }
            if (ShellUtils.isWin()) {
                crtPath = "D:\\ants\\dns\\ants-dns\\ants-dns\\http_ca.crt";
            } else {
                File file = new File(crtPath);
                if (!file.exists()) {
                    String msg = crtPath + ",文件不存在";
                    System.out.println(msg);
                    return msg;
                }
            }
            if (null == certificate) {
                // Certificate certificate = certificateFactory.generateCertificate(new
                // FileInputStream(crtPath));
                try (FileInputStream inputStream = new FileInputStream(crtPath)) {
                    certificate = certificateFactory.generateCertificate(inputStream);
                    // 对 certificate 进行后续操作
                } catch (IOException | CertificateException e) {
                    // 处理异常情况
                } finally {

                }
                if (null == certificate) {
                    String msg = crtPath + ",读取ca文件失败";
                    System.out.println(msg);
                    return msg;
                }
            }

            // Create TrustStore
            KeyStore trustStoreContainingTheCertificate = KeyStore.getInstance("JKS");
            trustStoreContainingTheCertificate.load(null, null);

            // AddCertificate 第一个参数为证书别名, 可以任取
            trustStoreContainingTheCertificate.setCertificateEntry("XYZ", certificate);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStoreContainingTheCertificate);

            // Create SSLContext 我这里协议为TLSv1.2
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext);

            // Create custom httpClient 创建自定义httpClient连接
            httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
            try {
                // POST请求
                // HttpPost method = new HttpPost("https://www.apapanet.com/");
                // get
                HttpPost postRequest = new HttpPost(uri);
                // System.out.println("executing request" + postRequest.getRequestLine());
                postRequest.setHeader("Content-type", "application/json;charset=utf-8");
                postRequest.setHeader("Accept", "application/json");
                byte[] message = ("elastic:" + password).getBytes(StandardCharsets.UTF_8);
                String basicAuth = Base64.getEncoder().encodeToString(message);
                postRequest.setHeader("Authorization", "Basic " + basicAuth);

                // setConfig,添加配置,如设置请求超时时间,连接超时时间
                org.apache.http.client.config.RequestConfig reqConfig = org.apache.http.client.config.RequestConfig
                        .custom().setSocketTimeout(SOCKET_TIME_OUT).setConnectTimeout(CONNECT_TIME_OUT).build();
                postRequest.setConfig(reqConfig);

                // setEntity,添加内容
                if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(queryData)) {
                    // System.out.println(queryData);
                    HttpEntity entity = new StringEntity(queryData, StandardCharsets.UTF_8);
                    postRequest.setEntity(entity);
                }
                response = httpClient.execute(postRequest);
                // int status = response.getStatusLine().getStatusCode();
                // if (status == HttpStatus.SC_OK) {}
                String body = EntityUtils.toString(response.getEntity());
                if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(body)) {
                    // System.out.println(body);
                    result.append(body);
                }
                ret = result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                ret = e.getMessage();
            } finally {
                if (null != response) {
                    response.close();
                }
                if (null != httpClient) {
                    httpClient.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret = e.getMessage();
        }
        return ret;
    }

    private static R apiResponseHandler(long userId, JSONObject jsonObject, String url) {
        String uidKey = Long.toString(userId) + "_dns_rewrite_";
        byte[] bytes = uidKey.getBytes();
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        long crc = crc32.getValue();
        jsonObject.put("idCrc32", crc);
        jsonObject.put("userId", userId);
        if (0 == userId) {
            jsonObject.put("maxCount", 10000);
        }
        String hr_data = HttpRequest.okHttpPost(url, jsonObject.toJSONString());
        if (StringUtils.isBlank(hr_data)) {
            return R.error("参数错误！");
        }
        JSONObject rMap = DataTypeConversionUtil.string2Json(hr_data);
        if (null != rMap && rMap.containsKey("code")) {
            if (1 == rMap.getInteger("code")) {
                return R.ok().put("data", hr_data);
            } else {
                return R.error(rMap.get("msg").toString()).put("data", hr_data);
            }
        }
        return R.error("hr_data");
    }

    public static R apiRequest(Long userId, RequestVo requestVo) {
        if (requestVo.getMethod().equalsIgnoreCase("get")) {
            return R.ok().put("data", HttpRequest.curlHttpGet(requestVo.getUrl()));
        } else if (requestVo.getMethod().equalsIgnoreCase("post")) {
            if (requestVo.getUrl().contains("/app/rewrite/cname/")) {
                // url "https://dns.99dns.com/api//app/rewrite/cname/list"
                if (null != requestVo.getParams() && !requestVo.getParams().isEmpty()) {
                    // System.out.println(requestVo.getParams());
                    List<String> result = new ArrayList<>();
                    for (String param : requestVo.getParams()) {
                        JSONObject jsonObject = DataTypeConversionUtil.string2Json(param);
                        if (null != jsonObject) {
                            R r = apiResponseHandler(userId, jsonObject, requestVo.getUrl());
                            result.add(r.toJsonString());
                        }
                    }
                    return R.ok().put("data", result);
                }
                JSONObject jsonObject = DataTypeConversionUtil.string2Json(requestVo.getParam());
                if (null != jsonObject) {
                    return apiResponseHandler(userId, jsonObject, requestVo.getUrl());
                }
            }
            return R.ok().put("data", HttpRequest.okHttpPost(requestVo.getUrl(), requestVo.getParam()));
        } else {
            return R.error("[" + requestVo.getMethod() + "]不支持！");
        }
    }

    public static void requestGetHead(String rUrl) {
        try {
            // URL url = new URL("https://www.example.com");
            URL url = new URL(rUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");

            // 获取响应状态码
            int statusCode = connection.getResponseCode();
            System.out.println(statusCode);

            // 获取响应头
            Map<String, List<String>> headers = connection.getHeaderFields();
            for (String headerName : headers.keySet()) {
                System.out.println(headerName + ": " + headers.get(headerName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isExistInAntsWaf(String rUrl) {
        try {
            if (rUrl.contains("*")) {
                rUrl = rUrl.replace("*", "a");
            }
            // URL url = new URL("https://www.example.com");
            URL url = new URL(String.format("%s/check_cname_in_sys/", rUrl));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // 设置读取超时时间为秒
            connection.setReadTimeout(10 * 1000);
            // 获取响应状态码
            int statusCode = connection.getResponseCode();
            if (false) {
                connection.setRequestMethod("HEAD");
                // System.out.println(statusCode);
                // 获取响应头
                Map<String, List<String>> headers = connection.getHeaderFields();
                final String key = "Set-Cookie";
                final String sKey = "WAF-R-C=";
                if (headers.containsKey(key)) {
                    List<String> cValues = headers.get(key);
                    if (cValues.size() > 0) {
                        for (String v : cValues) {
                            if (v.contains(sKey)) {
                                return true;
                            }
                        }
                    }
                }
            }
            if (200 == statusCode) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // Output the response body
                // System.out.println("Response Body: " + response.toString());
                if (response.toString().equals("ok")) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("check_cname_error:" + e.getMessage());
        }
        return false;
    }

    public static String httpGetFromCmd(String cmdStr) {
        final Pattern URL_PATTERN = Pattern.compile(
                "((https?|ftp|gopher|telnet|file|notes|ms-help):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                Pattern.CASE_INSENSITIVE);

        // String text = "这是一个测试文本，其中包含了一个URL：https://www.example.com/path/to/page 和另一个
        // ftp://ftp.example.com/file.txt";
        StringBuilder sb = new StringBuilder();
        Matcher matcher = URL_PATTERN.matcher(cmdStr);
        while (matcher.find()) {
            // System.out.println(matcher.group());
            sb.append(sendGetByUrl(matcher.group()));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        // boolean r= isExistAntsWafCook("http://*.cdntest.91hu.top");

        // System.out.println(r);
        // int i=0;
        // while (true){
        // String r=HttpRequest.curlHttpGet("http://www.vedns.com");
        // System.out.println(i);
        // i++;
        // }

        // String aa="80,443";
        // System.out.println(Integer.parseInt(aa));
        // httpGetFromCmd("");
    }
}