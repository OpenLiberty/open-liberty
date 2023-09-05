package io.openliberty.microprofile.telemetry.internal.cdi;

import java.security.PrivilegedAction;
import java.util.HashMap;

import io.openliberty.microprofile.telemetry.common.internal.cdi.AbstractOpenTelemetryProducer;
import io.openliberty.microprofile.telemetry.common.internal.cdi.BaggageProxy;
import io.openliberty.microprofile.telemetry.common.internal.cdi.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.common.internal.cdi.SpanProxy;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class OpenTelemetryProducer extends AbstractOpenTelemetryProducer {

	@Override
	protected PrivilegedAction<OpenTelemetry> getSDKBuilderPrivilegedAction(HashMap<String, String> telemetryProperties) {
		return (PrivilegedAction<OpenTelemetry>) () -> {
            return AutoConfiguredOpenTelemetrySdk.builder()
                            .addPropertiesCustomizer(x -> telemetryProperties) //Overrides OpenTelemetry's property order
                            .addResourceCustomizer(this::customizeResource)//Defaults service name to application name
                            .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                            .disableShutdownHook()
                            .build()
                            .getOpenTelemetrySdk();
        };
	}
	
    @Override
    @ApplicationScoped
    @Produces
    public OpenTelemetry getOpenTelemetry(OpenTelemetryInfo openTelemetryInfo) {
        return super.getOpenTelemetry(openTelemetryInfo);
    }

    @Override
    @Produces
    public Tracer getTracer(OpenTelemetry openTelemetry) {
        return super.getTracer(openTelemetry);
    }

    @Override
    @Produces
    @ApplicationScoped
    public Span getSpan() {
        return super.getSpan();
    }

    @Override
    @Produces
    @ApplicationScoped
    public Baggage getBaggage() {
        return super.getBaggage();
    }


    @Override
    @ApplicationScoped
    @Produces
    public OpenTelemetryInfo getOpenTelemetryInfo() {
        return super.getOpenTelemetryInfo();
    }

}
