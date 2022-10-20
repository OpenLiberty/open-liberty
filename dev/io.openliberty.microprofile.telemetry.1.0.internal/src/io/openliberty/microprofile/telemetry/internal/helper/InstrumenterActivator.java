package io.openliberty.microprofile.telemetry.internal.helper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;

public class InstrumenterActivator implements BundleActivator {
    //The J2S exception occurs due to a call during the initialization of Instrumenter and InstrumenterBuilder
    @Override
    public void start(BundleContext ctx) throws Exception {
        Class.forName(Instrumenter.class.getName());
        Class.forName(InstrumenterBuilder.class.getName());
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        // Unregister the EclipseLinke provider
    }
}
