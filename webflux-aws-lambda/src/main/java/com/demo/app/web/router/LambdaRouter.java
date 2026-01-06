package com.demo.app.web.router;

import com.demo.app.web.handler.LambdaHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class LambdaRouter {

    @Bean
    public RouterFunction<ServerResponse> routes(LambdaHandler handler) {
        return RouterFunctions.route()
                .POST("/lambda/invoke", handler::invoke)
                .build();
    }
}
