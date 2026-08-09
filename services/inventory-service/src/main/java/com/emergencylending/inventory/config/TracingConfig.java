package com.emergencylending.inventory.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the OTel SDK and Micrometer Tracing bridge manually.
 *
 * Spring Boot 4.x extracted its tracing autoconfiguration into a module that is
 * not pulled in transitively by spring-boot-starter-actuator alone, so no
 * Tracer or SpanExporter beans appear in the context without this class.
 */
@Configuration
public class TracingConfig {

    @Bean
    public OpenTelemetry openTelemetry(
            @Value("${spring.application.name}") String serviceName) {

        Resource resource = Resource.create(
                Attributes.of(AttributeKey.stringKey("service.name"), serviceName));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .build();

        Runtime.getRuntime().addShutdownHook(
                new Thread(tracerProvider::close, "otel-shutdown"));

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer micrometerTracer(OpenTelemetry openTelemetry,
                                   @Value("${spring.application.name}") String serviceName) {
        OtelCurrentTraceContext traceContext = new OtelCurrentTraceContext();
        io.opentelemetry.api.trace.Tracer otelTracer = openTelemetry.getTracer(serviceName);
        return new OtelTracer(otelTracer, traceContext, event -> {});
    }

    @Bean
    public DefaultTracingObservationHandler tracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }
}
