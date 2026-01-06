package com.demo.app.ports.in;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface LambdaPortIn {
    Mono<String> invoke(Map<String, Object> payload);
}
