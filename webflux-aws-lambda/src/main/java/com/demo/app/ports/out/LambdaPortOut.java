package com.demo.app.ports.out;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface LambdaPortOut {
    Mono<String> invoke(Map<String, Object> payload);
}
