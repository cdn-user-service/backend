package io.ants.common.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    private static ExecutorService singleThreadPool;

    static {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()  .setNameFormat("ants-cdn-pool-%d").build();
        ThreadUtils.singleThreadPool = new ThreadPoolExecutor(
                1,
                1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    protected void finalize() {
        // 析构方法
        ThreadUtils.singleThreadPool.shutdown();
    }
    public static void testThread(){
        try{

            ThreadUtils.singleThreadPool.execute(()-> System.out.println(Thread.currentThread().getName()));

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static void startThreadFunc(){
        try{

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

    }
}
