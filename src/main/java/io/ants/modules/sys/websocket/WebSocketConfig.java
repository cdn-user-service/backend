package io.ants.modules.sys.websocket;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketHandlerService webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {

        webSocketHandlerRegistry.addHandler(webSocketHandler, "/ws_ssh")
                .setAllowedOrigins("*");
    }

    //    @Bean
    //    public SimpleUrlHandlerMapping handlerMapping() {
    //        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
    //        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
    //        mapping.setUrlMap(Collections.singletonMap("/ws_ssh/{cid}", webSocketHandler));
    //        return mapping;
    //    }
}
