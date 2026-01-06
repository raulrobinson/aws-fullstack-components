package com.demo.app.service;

import com.demo.app.ports.in.LambdaPortIn;
import com.demo.app.ports.out.LambdaPortOut;
import com.demo.app.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LambdaService implements LambdaPortIn {

    private final LambdaPortOut lambdaPortOut;

    @Override
    public Mono<String> invoke(Map<String, Object> payload) {
        log.info("[LAMBDA-SERVICE] Invoking Lambda with payload: {}", payload);
        return lambdaPortOut.invoke(payload)
                .flatMap(response -> {

                    // 1. Parsear respuesta completa de Lambda
                    Map<String, Object> lambdaResult =
                            JsonUtil.fromJson(response, new TypeReference<>() {});

                    // 2. Extraer body (sigue siendo String)
                    String bodyJson = (String) lambdaResult.get("body");

                    // 3. Parsear body a objeto real
                    //UserDto user =
                    //        JsonUtil.fromJson(bodyJson, UserDto.class);

                    return Mono.just(bodyJson);
                })
                .doOnSuccess(response -> log
                        .info("[LAMBDA-SERVICE] Lambda invocation successful. Response: {}", response))
                .doOnError(error -> log
                        .error("[LAMBDA-SERVICE] Lambda invocation failed. Error: {}", error.getMessage()));
    }
}
