/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.management.ServiceNotFoundException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

// We want this to start before CDI so it clears the list of stopped apps before CDI gets to them.
@Component(property = { "service.vendor=IBM",  "service.ranking:Integer=150" })
public class OpenTelemetryInfoFactory implements ApplicationStateListener {

    /*
     * Class summary. 
     * 
     * This class consists of three parts
     * 1: Static methods for producing Objects of: OpenTelemetryInfo, OpenTelemetry, Tracer, Span, Baggage.
     *    If invoked after an app has been shutdown these will return a no-op object.
     * 
     * 2. A cache of OpenTelemetryInfo (using J2EENames as the key). OpenTelemetryInfo in turn contains OpenTelemetry objects. This is populated lazily using computeIfAbsent and the use of ConcurrentHashMap ensures thread safety.
     *    
     * 3. An ApplicationStateListener. 
     *    On startup the ASL only removes applications from the list of apps that have shut down so they will not return a no-op
     *    On shutdown the ASL cleans up an OpenTelemetry object, removes its parent OpenTelemetryInfo from the cache, and registers it as an app which has shut down.
     */

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";
    private static final String ENV_DISABLE_PROPERTY = "OTEL_SDK_DISABLED";
    private static final String CONFIG_DISABLE_PROPERTY = "otel.sdk.disabled";
    private static final String ENV_METRICS_EXPORTER_PROPERTY = "OTEL_METRICS_EXPORTER";
    private static final String CONFIG_METRICS_EXPORTER_PROPERTY = "otel.metrics.exporter";
    private static final String ENV_LOGS_EXPORTER_PROPERTY = "OTEL_LOGS_EXPORTER";
    private static final String CONFIG_LOGS_EXPORTER_PROPERTY = "otel.logs.exporter";
    private static final String SERVICE_NAME_PROPERTY = "otel.service.name";

    private static final TraceComponent tc = Tr.register(OpenTelemetryInfoFactory.class);

    private static final Map<J2EEName, OpenTelemetryInfo> appToInfo = new ConcurrentHashMap<J2EEName, OpenTelemetryInfo>();
    private static final Set<J2EEName> shutDownApplications = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<J2EEName, Boolean>())); //We have no good place to say forget about an app entirely, so I use weakreferences. 

    //The key methods that provide access to telemetry objects begin here
    public static OpenTelemetryInfo getOpenTelemetryInfo(J2EEName j2EEName) {
        return appToInfo.computeIfAbsent(j2EEName, OpenTelemetryInfoFactory::createOpenTelemetryInfo);
    }

    public static Tracer getTracer(J2EEName j2EEName) {
        try {
            return getOpenTelemetryInfo(j2EEName).getOpenTelemetry().getTracer(INSTRUMENTATION_NAME);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return OpenTelemetry.noop().getTracerProvider().get("");
        }
    }

    //The key methods that provide access to telemetry objects end here

    private static OpenTelemetryInfo createOpenTelemetryInfo(J2EEName j2EEName) {
        //If the app has already shut down but something still tries to get OpenTelemetry objects return a no-op object
        if (shutDownApplications.contains(j2EEName)){ //There is a low risk race condition here. Thread A gets past this check just as thread B shuts everything down.
            Tr.warning(tc, Tr.formatMessage(tc, "CWMOT5003.factory.used.after.shutdown", j2EEName.getApplication()));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Exception e = new Exception();
                ByteArrayOutputStream stackStream = new ByteArrayOutputStream();
                PrintStream stackPrintStream = new PrintStream(stackStream);
                e.printStackTrace(stackPrintStream);
                
                Tr.event(tc, "OpenTelemetryInfoFactory", "The stack that led to OpenTelemetryInfoFactory being called after " + j2EEName.getApplication() + " has shutdown is:.");
                Tr.event(tc, stackStream.toString());
            }
            return new OpenTelemetryInfo(false, OpenTelemetry.noop());
        }

        try {
            if (AgentDetection.isAgentActive()) {
                // If we're using the agent, it will have set GlobalOpenTelemetry and we must use its instance
                // all config is handled by the agent in this case
                return new OpenTelemetryInfo(true, GlobalOpenTelemetry.get());
            }

            final Map<String, String> telemetryProperties = getTelemetryProperties();

            //Builds tracer provider if user has enabled tracing aspects with config properties
            if (!checkDisabled(telemetryProperties)) {
                OpenTelemetry openTelemetry = AccessController.doPrivileged( (PrivilegedAction<OpenTelemetry>) () -> {
                    //This contains API calls that change between the upstream open telemetry version.
                    //We use @Reference because the callers coming into  OpenTelemetryInfoFactory via
                    //The static accessor do not know which version of mpTelemetry will be in use.
                    BundleContext bc = getBundleContext(OpenTelemetryInfoFactory.class);
                    OpenTelemetrySdkBuilderSupplier supplier = getService(bc, OpenTelemetrySdkBuilderSupplier.class);

                    return supplier
                                    .getPartiallyConfiguredOpenTelemetrySDKBuilder()
                                    .addPropertiesCustomizer(x -> telemetryProperties) //Overrides OpenTelemetry's property order
                                    .addResourceCustomizer(OpenTelemetryInfoFactory::customizeResource) //Defaults service name to application name
                                    .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                                    .build()
                                    .getOpenTelemetrySdk();
                });

                if (openTelemetry != null) {
                    return new OpenTelemetryInfo(true, openTelemetry);
                }
            }
            //By default, MicroProfile Telemetry tracing is off.
            //The absence of an installed SDK is a “no-op” API
            //Operations on a Tracer, or on Spans have no side effects and do nothing
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            String applicationName = cData.getJ2EEName().getApplication();
            Tr.info(tc, "CWMOT5100.tracing.is.disabled", applicationName);

            return new OpenTelemetryInfo(false, OpenTelemetry.noop());
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new OpenTelemetryInfo(false, OpenTelemetry.noop());
        }

    }


    public synchronized void disposeOpenTelemetry(J2EEName j2EEName) {
        try {
            if (AgentDetection.isAgentActive()) {
                return;
            }

            OpenTelemetry openTelemetry = null;

            if (appToInfo.containsKey(j2EEName)) {
                openTelemetry = appToInfo.get(j2EEName).getOpenTelemetry();
            }

            if (openTelemetry instanceof OpenTelemetrySdk) {
                OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetry;
                List<CompletableResultCode> results = new ArrayList<>();

                SdkTracerProvider tracerProvider = sdk.getSdkTracerProvider();
                if (tracerProvider != null) {
                    results.add(tracerProvider.shutdown());
                }

                SdkMeterProvider meterProvider = sdk.getSdkMeterProvider();
                if (meterProvider != null) {
                    results.add(meterProvider.shutdown());
                }

                SdkLoggerProvider loggerProvider = sdk.getSdkLoggerProvider();
                if (loggerProvider != null) {
                    results.add(loggerProvider.shutdown());
                }

                CompletableResultCode.ofAll(results).join(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }
    }

    private static boolean checkDisabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(ENV_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(ENV_DISABLE_PROPERTY));
        } else if (oTelConfigs.get(CONFIG_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(CONFIG_DISABLE_PROPERTY));
        }
        return true;
    }

    private static HashMap<String, String> getTelemetryProperties() {
        try {
            Config config = ConfigProvider.getConfig();

            HashMap<String, String> telemetryProperties = new HashMap<>();
            for (String propertyName : config.getPropertyNames()) {
                if (propertyName.startsWith("otel.")) {
                    config.getOptionalValue(propertyName, String.class).ifPresent(
                                                                                  value -> telemetryProperties.put(propertyName, value));
                }
            }
            //Metrics and logs are disabled by default
            telemetryProperties.put(CONFIG_METRICS_EXPORTER_PROPERTY, "none");
            telemetryProperties.put(CONFIG_LOGS_EXPORTER_PROPERTY, "none");
            telemetryProperties.put(ENV_METRICS_EXPORTER_PROPERTY, "none");
            telemetryProperties.put(ENV_LOGS_EXPORTER_PROPERTY, "none");
            return telemetryProperties;
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new HashMap<String, String>();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        //We do not actually initilize on application starting, we do that lazily if this is needed.
        //However setting the j2EEName in the constructor explodes so we set it slightly afterwords in this method.
        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        J2EEName j2EEName = extAppInfo.getMetaData().getJ2EEName();
        shutDownApplications.remove(j2EEName);
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException { } //no-op

    @Override
    public void applicationStopping(ApplicationInfo appInfo) { } //no-op

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        ExtendedApplicationInfo extAppInfo = (ExtendedApplicationInfo) appInfo;
        J2EEName j2EEName = extAppInfo.getMetaData().getJ2EEName();
        shutDownApplications.add(j2EEName);
        disposeOpenTelemetry(j2EEName);
        appToInfo.remove(j2EEName);
    }

    //Adds the service name to the resource attributes
    private static Resource customizeResource(Resource resource, ConfigProperties c) {
        ResourceBuilder builder = resource.toBuilder();
        builder.put(ResourceAttributes.SERVICE_NAME, getServiceName(c));
        return builder.build();
    }

    //Uses application name if the user has not given configured service.name resource attribute
    private static String getServiceName(ConfigProperties c) {
        String appName = c.getString(SERVICE_NAME_PROPERTY);
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (appName == null) {
            if (cmd != null) {
                appName = cmd.getModuleMetaData().getApplicationMetaData().getName();
            }
        }
        return appName;
    }

    public interface OpenTelemetrySdkBuilderSupplier {
        public AutoConfiguredOpenTelemetrySdkBuilder getPartiallyConfiguredOpenTelemetrySDKBuilder();
    }

    //OSGi Helper methods

    /**
     * Get the OSGi bundle context for the given class
     *
     * @param clazz the class to find the context for
     * @return the bundle context
     */
    static BundleContext getBundleContext(Class<?> clazz) {
        BundleContext context = null; //we'll return null if not running inside an OSGi framework (e.g. unit test) or framework is shutting down
        if (FrameworkState.isValid()) {
            Bundle bundle = FrameworkUtil.getBundle(clazz);

            if (bundle != null) {
                context = bundle.getBundleContext();
            }
        }
        return context;
    }

    /**
     * Find a service of the given type
     *
     * @param bundleContext The context to use to find the service
     * @param serviceClass The class of the required service
     * @return the service instance
     * @throws InvalidFrameworkStateException if the server OSGi framework is being shutdown
     * @throws ServiceNotFoundException if an instance of the requested service can not be found
     */
    private static <T> T getService(BundleContext bundleContext, Class<T> serviceClass) throws BundleLoadingException {
        if (!FrameworkState.isValid()) {
            throw new BundleLoadingException("Invalid OSGi Framework State");
        }

        ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);

        T service = null;
        if (ref != null) {
            service = bundleContext.getService(ref);
        }

        if (service == null) {
            //One last check to make sure the framework didn't start to shutdown after we last looked
            if (!FrameworkState.isValid()) {
                throw new BundleLoadingException("Invalid OSGi Framework State");
            } else {
                throw new BundleLoadingException("Service not Found");
            }
        }
        return service;
    }

    //RuntimeException so we can pass it out a doPriv.
    private static class BundleLoadingException extends RuntimeException {

        private static final long serialVersionUID = -4981796156801061535L;

        public BundleLoadingException(String string) {
            super(string);
        }

    }

}
