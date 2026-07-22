package com.guenbon.jochuckhub.config.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Logs external API outcomes without recording credentials or response bodies. */
@Component
@Slf4j
public class ExternalApiLogger {

    public void requested(String provider, String operation) {
        log.info("external_api provider={} operation={} status=requested", provider, operation);
    }

    public void succeeded(String provider, String operation, int status, long durationMillis) {
        log.info("external_api provider={} operation={} status={} duration_ms={}",
                provider, operation, status, durationMillis);
    }

    public void failed(String provider, String operation, Integer status, String reason, long durationMillis) {
        log.error("external_api provider={} operation={} status={} reason={} duration_ms={}",
                provider, operation, status, reason, durationMillis);
    }
}
