package io.ants.modules.sys.redis;


import org.springframework.util.ErrorHandler;

public class StreamErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        System.out.println(throwable.getMessage());
    }
    //org.springframework.util

}
