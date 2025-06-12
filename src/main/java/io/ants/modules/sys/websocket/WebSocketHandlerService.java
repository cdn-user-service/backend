package io.ants.modules.sys.websocket;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import io.ants.modules.sys.dao.CdnClientDao;
import io.ants.modules.sys.entity.CdnClientEntity;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandlerService implements WebSocketHandler {

    @Autowired
    private CdnClientDao cdnClientDao;


    @Data
    public class JSshObjVo{
       private   com.jcraft.jsch.Session jSession=null;
       private   com.jcraft.jsch.Channel jChannel=null;
       private   PrintWriter sshPwOut =null;
       private   WebSocketSession wSession=null;
       private   Thread thread=null;
    }

    private static Map<String, JSshObjVo> sessionsMap = new ConcurrentHashMap<>();

    //    private   com.jcraft.jsch.Session jSession=null;
    //    private   com.jcraft.jsch.Channel jChannel=null;
    //    private   PrintWriter sshPwOut =null;


//
//    @OnMessage
//    public void onMessage(String message,javax.websocket.Session wSession) {
//        //当 websocket 建立的连接断开后会触发这个注解修饰的方法
//        try {
//            out.println(message);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    @OnClose
//    public void onClose(javax.websocket.Session wSession) {
//        //当 websocket 建立的连接断开后会触发这个注解修饰的方法
//        try {
//            channel.disconnect();
//            jSession.disconnect();
//            wSession.close();
//            System.out.println("Disconnected successfully!");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    @OnError
//    public void onError(Throwable t) {
//        //当 websocket 建立的连接断开后会触发这个注解修饰的方法
//        t.printStackTrace();
//    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wSession) throws Exception {
        //当 websocket 建立连接成功后会触发这个注解修饰的方法
       // System.out.println("ssh_ws_afterConnectionEstablished");
        //System.out.println(wSession.getRemoteAddress().getAddress());
        try {
            String wSessionId=wSession.getId();

           // System.out.println("wSession.getId()=="+wSession.getId());
            URI uri = wSession.getUri();
            String query = uri.getQuery();
            if (StringUtils.isBlank(query)){
                TextMessage textMessage = new TextMessage("query is null!");
                wSession.sendMessage(textMessage);
                return ;
            }
            // 解析查询参数
            Map<String, String> queryParams = new HashMap<>(64);
            String[] queryParamsArray = query.split("&");
            for (String queryParam : queryParamsArray) {
                String[] queryParamPair = queryParam.split("=");
                queryParams.put(queryParamPair[0], queryParamPair[1]);
            }
            String cid = queryParams.getOrDefault("cid", "");
            if (StringUtils.isBlank(cid)){
                TextMessage textMessage = new TextMessage("cid is null!");
                wSession.sendMessage(textMessage);
                return ;
            }
            CdnClientEntity clientEntity=cdnClientDao.selectById(cid);
            if (null==clientEntity){
                TextMessage textMessage = new TextMessage("client  is not exist !");
                wSession.sendMessage(textMessage);
                return;
            }


            JSch jsch = new JSch();
            String host = clientEntity.getClientIp();
            String user = clientEntity.getSshUser();
            String password =clientEntity.getSshPwd();
            int port = clientEntity.getSshPort();

            //    private   com.jcraft.jsch.Session jSession=null;
            //    private   com.jcraft.jsch.Channel jChannel=null;
            //    private   PrintWriter sshPwOut =null;

            com.jcraft.jsch.Session  jSession = jsch.getSession(user, host, port);
            jSession.setPassword(password);
            jSession.setConfig("StrictHostKeyChecking", "no");
            jSession.connect();

            //创建了一个 SSH shell 通道
            com.jcraft.jsch.Channel jChannel = jSession.openChannel("shell");
            //终端类型为 "vt102"
            //((ChannelShell) jChannel).setPtyType("vt102");
            ((ChannelShell) jChannel).setPtyType("vt102");

            //获取 OutputStream 对象,向 inputStream_for_the_channel 中写入指令
            OutputStream inputStream_for_the_channel = jChannel.getOutputStream();
            PrintWriter sshPwOut = new PrintWriter(new OutputStreamWriter(inputStream_for_the_channel), true);

            jChannel.connect();
            JSshObjVo jSshObjVo=new JSshObjVo();
            jSshObjVo.setJSession(jSession);
            jSshObjVo.setJChannel(jChannel);
            jSshObjVo.setSshPwOut(sshPwOut);
            jSshObjVo.setWSession(wSession);
            sessionsMap.put(wSessionId,jSshObjVo);
            //System.out.println("Connection established successfully!");
            //out.println("\r");
            channelThread(wSessionId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**向sh发送消息
     * @param wSession
     * @param msg
     */
    private void sendMsg(WebSocketSession wSession,String msg){
       //System.out.println("send:"+msg);
       try{
           for (int i = 0; i < msg.length(); i++) {
               char c = msg.charAt(i);
               // 在这里对每个字符进行处理
               //  System.out.println("Received character: " + c);
               String singleChar = Character.toString(c);
               TextMessage message = new TextMessage(singleChar);
               wSession.sendMessage(message);
           }

       }catch (Exception e){
           e.printStackTrace();
       }
    }

    /**
     * 一次性发送所有
     * @param wSession
     * @param msg
     */
    private void sendMsgOnce(WebSocketSession wSession,String msg){
        //System.out.println("send2Ws:"+msg);
        try{
            TextMessage message = new TextMessage(msg);
            wSession.sendMessage(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *ssh channel 输出字符
     * @param wSessionId
     */
    private void channelOutHandle(String wSessionId){
        try{
            //读取返回
            //System.out.println("channelOut-thread-wSessionId:"+wSessionId);

            if (!sessionsMap.containsKey(wSessionId)){
                System.out.println("wSessionId【"+wSessionId+"】 nor found");
                return;
            }
            JSshObjVo jSshObjVo=sessionsMap.get(wSessionId);

            InputStream inputStream_for_the_channel = jSshObjVo.getJChannel().getInputStream();
            BufferedInputStream in = new BufferedInputStream(inputStream_for_the_channel);

            // 一次性发磅
            byte[] tmp = new byte[1024];
            while (!Thread.currentThread().isInterrupted()) {
                while (in.available() >= 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i <= 0){
                        break;
                    }
                    String strResult = new String(tmp, 0, i);
                    //将ssh_channel的消息输出到web_ws
                    //System.out.println("sshOut:"+strResult);
                    sendMsgOnce(jSshObjVo.getWSession(),strResult);
                }
                if ( jSshObjVo.getJChannel().isClosed()) {
                    if (in.available() > 0)
                    {
                        continue;
                    }
                    jSshObjVo.getSshPwOut().println("exit-status: " + jSshObjVo.getJChannel().getExitStatus());
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void channelThread(String wSessionId){
        JSshObjVo jSshObjVo=sessionsMap.get(wSessionId);
        if (null==jSshObjVo){
            System.out.println("channelThread wSessionId is null");
            return;
        }
       Thread thread= new Thread(new Runnable() {
            @Override
            public void run() {
                channelOutHandle(wSessionId);
            }
        });
       thread.start();
       jSshObjVo.setThread(thread);
    }

    /**
     * 接收到消息事件
     * @param webSocketSession
     * @param webSocketMessage
     * @throws Exception
     */
    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) throws Exception {
        //System.out.println("handleMessage:"+webSocketSession.getId());
        JSshObjVo jSshObjVo=sessionsMap.get(webSocketSession.getId());
        if (null==jSshObjVo){
            sendMsgOnce(webSocketSession,"get ssh channel error,jSshObjVo is null [1]!");
            return;
        }
        //System.out.println(webSocketSession.getRemoteAddress().getAddress());
        //System.out.println(webSocketMessage.getPayloadLength());
        String message="";
        if (webSocketMessage.getPayload() instanceof TextMessage) {
            message= ((TextMessage) webSocketMessage.getPayload()).getPayload();
        }else {
            message=(String) webSocketMessage.getPayload();
        }
        //        if (StringUtils.isBlank(message)){
        //            System.out.println("message is null");
        //            //sendMsgOnce(webSocketSession,"message is null");
        //            return;
        //        }
        if (null==jSshObjVo.getJSession()){
            sendMsgOnce(webSocketSession,"jSession is null [1]!");
            return;
        }
        if (null== jSshObjVo.getSshPwOut()){
            sendMsgOnce(webSocketSession,"sshPwOut is null [2]!");
            return;
        }
        //System.out.println("rcv:"+new String(message.getBytes()));
        // 写入指令
        //sshPwOut.print(message.getBytes());
        //sshPwOut.flush();
        for (int i = 0; i < message.getBytes().length; i++) {
            byte c =  message.getBytes()[i];
            int intValue = c & 0xFF; // 将 byte 转换为无符号的 int 类型
            char charValue = (char) intValue;
            jSshObjVo.getSshPwOut().print(charValue);
            jSshObjVo.getSshPwOut().flush();
        }
        //sendMsgOnce(webSocketSession,message);
    }


    @Override
    public void handleTransportError(WebSocketSession wSession, Throwable throwable) throws Exception {
        //System.out.println("ssh_ws_handleTransportError");
        throwable.printStackTrace();
        JSshObjVo jSshObjVo=sessionsMap.get(wSession.getId());
        try{
            if (null!=jSshObjVo){
                jSshObjVo.getJChannel() .disconnect();
                jSshObjVo.getJSession() .disconnect();
            }
            wSession.close();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (null!=jSshObjVo){
                jSshObjVo.getThread().interrupt();
                sessionsMap.remove(wSession.getId());
            }
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession wSession, CloseStatus closeStatus) throws Exception {
        //System.out.println("ssh_ws_afterConnectionClosed");
        //System.out.println(closeStatus);
        JSshObjVo jSshObjVo=sessionsMap.get(wSession.getId());
        try {
            if (null!=jSshObjVo){
                jSshObjVo.getJChannel() .disconnect();
                jSshObjVo.getJSession() .disconnect();
            }
            wSession.close();
            //System.out.println("Disconnected successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            if (null!=jSshObjVo){
                jSshObjVo.getThread().interrupt();
                sessionsMap.remove(wSession.getId());
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        //init return false
       // System.out.println("ssh_ws_supportsPartialMessages");
        return false;
    }



}
