package com.demo.app.adapter;

import com.demo.app.ports.out.LambdaPortOut;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import java.util.Map;

@Slf4j
@Component
public class LambdaAdapter implements LambdaPortOut {

    private final LambdaAsyncClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.lambda.name}") String FUNCTION_NAME;

    public LambdaAdapter(LambdaAsyncClient lambdaClient, ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> invoke(Map<String, Object> payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(FUNCTION_NAME)
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .build();

            return Mono.fromFuture(lambdaClient.invoke(request))
                    .map(resp -> {
                        log.info("[LAMBDA-ADAPTER] value: {}", resp.toString());
                        return resp.payload().asUtf8String();
                    });
        }
        catch (Exception e) {
            log.error("[LAMBDA-ADAPTER] error: {}", e.getMessage());
            return Mono.error(e);
        }
    }
}

