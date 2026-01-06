package com.demo.app.web.handler;

import com.demo.app.ports.in.LambdaPortIn;
import com.demo.app.utils.DynamoDbFormatCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LambdaHandler {

    private final LambdaPortIn lambda;
    private final DynamoDbFormatCleaner cleaner;

    public Mono<ServerResponse> invoke(ServerRequest request) {

        log.info("[LAMBDA-HANDLER] Received request to invoke Lambda");
        return request.bodyToMono(Map.class)
                .flatMap(body -> lambda.invoke(body))
                //.map(raw -> cleaner.normalize(raw.toString()))
                .flatMap(cleanJson ->
                        ServerResponse.ok()
                                .header("Content-Type", "application/json")
                                .bodyValue(cleanJson)
                );
    }
}
