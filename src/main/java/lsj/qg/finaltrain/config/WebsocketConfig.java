package lsj.qg.finaltrain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebsocketConfig {

    @Bean //将返回值给sb框架管理
    public ServerEndpointExporter serverEndpointExporter() {return new ServerEndpointExporter();}
}
