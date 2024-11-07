/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.http;

import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

import io.openliberty.http.monitor.HttpStatAttributes;
import io.openliberty.http.monitor.metrics.HTTPMetricAdapter;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.context.Context;

/**
 *
 */
@Component(service = { HTTPMetricAdapter.class, ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPTelemetryHTTPMetricsAdapterImpl implements HTTPMetricAdapter, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(MPTelemetryHTTPMetricsAdapterImpl.class);

    private static final String INSTR_SCOPE = "io.openliberty.microprofile.telemetry20.internal.http";

    private static final String NO_APP_NAME_IDENTIFIER = "io.openliberty.microprofile.telemetry20.internal.http.no.app.name";

    private static final double NANO_CONVERSION = 0.000000001;
    private static final Double[] BUCKET_BOUNDARIES = { 0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0 };
    private static final List<Double> BUCKET_BOUNDARIES_LIST = Arrays.asList(BUCKET_BOUNDARIES);

    /**
     * Mapping between application name to a map of HTTP stats ID mapped to OpenTelemetry attributes
     * i.e. Map<appName, Map<HttpStatID, Attributes>>
     */
    private static Map<String, Map<String, Attributes>> appNameToAttributesMap = new ConcurrentHashMap<String, Map<String, Attributes>>();

    //All access to threadUnsafeHTTPHistogramMap must be synchronized using httpHistogramMapLock
    private final WeakHashMap<OpenTelemetry, DoubleHistogram> threadUnsafeHTTPHistogramMap = new WeakHashMap<OpenTelemetry, DoubleHistogram>();
    private final ReadWriteLock httpHistogramMapLock = new ReentrantReadWriteLock();

    @Override
    public void updateHttpMetrics(HttpStatAttributes httpStatAttributes, Duration duration) {

        OpenTelemetry otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo().getOpenTelemetry();

        /*
         * Even if the HTTP call is served by the server/runtime, the "appName" can be non null.
         * The AppName is retrieved through a ServletContext property and the "appname" can be the originating bundle.
         * This would not be "registered" as an appname with the Otel runtime and will return null.
         * We will then below retrieve a server/runtime instance.
         *
         */
        if (otelInstance == null) {
            otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo().getOpenTelemetry();
            if (otelInstance == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             String.format("Unable to resolve an OpenTelemetry instance for the HttpStatAttributes [%s]", httpStatAttributes.toString()));
                }
                //do nothing - return
                return;
            }
        }

        DoubleHistogram httpHistogram = getHTTPHistogram(otelInstance);

        Context ctx = Context.current();

        double seconds = duration.toNanos() * NANO_CONVERSION;

        String appName = getApplicationName();
        appName = appName == null ? NO_APP_NAME_IDENTIFIER : appName;

        String keyID = httpStatAttributes.getHttpStatID();

        // Key is the HttpStasID generated for each httpStatsAttribute
        Map<String, Attributes> attributesMap = appNameToAttributesMap.computeIfAbsent(appName, x -> new ConcurrentHashMap<String, Attributes>());
        Attributes attributes = attributesMap.computeIfAbsent(keyID, x -> retrieveAttributes(httpStatAttributes));

        httpHistogram.record(seconds, attributes, ctx);

    }

    private String getApplicationName() {
        ComponentMetaData metaData = com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (metaData != null) {
            J2EEName name = metaData.getJ2EEName();
            if (name != null) {
                return name.getApplication();
            }
        }
        return null;

    }

    private Attributes retrieveAttributes(HttpStatAttributes httpStatAttributes) {

        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesBuilder.put(HTTP_REQUEST_METHOD, httpStatAttributes.getRequestMethod());
        attributesBuilder.put(URL_SCHEME, httpStatAttributes.getScheme());

        Integer responseStatus = httpStatAttributes.getResponseStatus();
        if (responseStatus != null) {
            attributesBuilder.put(HTTP_RESPONSE_STATUS_CODE, Long.valueOf(responseStatus));
        }

        String httpRoute = httpStatAttributes.getHttpRoute();
        if (httpRoute != null) {
            attributesBuilder.put(HTTP_ROUTE, httpRoute);

        }

        attributesBuilder.put(NETWORK_PROTOCOL_VERSION, httpStatAttributes.getNetworkProtocolVersion());

        attributesBuilder.put(SERVER_ADDRESS, httpStatAttributes.getServerName());
        attributesBuilder.put(SERVER_PORT, Long.valueOf(httpStatAttributes.getServerPort()));

        String errorType = httpStatAttributes.getErrorType();
        if (errorType != null) {
            attributesBuilder.put(ERROR_TYPE, errorType);
        }

        return attributesBuilder.build();
    }

    /*
     * We can re-use the (histogram) Meter created here.
     * The Meter is built using the same static values each time.
     * The instrument that is recorded/updated is distinct for each
     * http-route/response/method combination (corresponds with resolved attributes).
     *
     * However we cannot share it across multiple instances of OpenTelemetry
     */
    private DoubleHistogram getHTTPHistogram(OpenTelemetry otelInstance) {

        try {
            httpHistogramMapLock.readLock().lock();
            if (threadUnsafeHTTPHistogramMap.containsKey(otelInstance)) {
                return threadUnsafeHTTPHistogramMap.get(otelInstance);
            }
        } finally {
            httpHistogramMapLock.readLock().unlock();
        }

        try {
            httpHistogramMapLock.writeLock().lock();
            return threadUnsafeHTTPHistogramMap.computeIfAbsent(otelInstance,
                                                                (OpenTelemetry openTelemetry) -> openTelemetry.getMeterProvider().get(INSTR_SCOPE)
                                                                                .histogramBuilder(OpenTelemetryConstants.HTTP_SERVER_REQUEST_DURATION_NAME)
                                                                                .setUnit(OpenTelemetryConstants.OTEL_SECONDS_UNIT)
                                                                                .setDescription(OpenTelemetryConstants.HTTP_SERVER_REQUEST_DURATION_DESC)
                                                                                .setExplicitBucketBoundariesAdvice(BUCKET_BOUNDARIES_LIST).build());
        } finally {
            httpHistogramMapLock.writeLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getDeploymentName();
        Map<String, Attributes> map = appNameToAttributesMap.remove(appName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc,
                     String.format("Detected that application %s has stopped. Removed a corresponding Map<String, Attributes> entry? [%b]", appName, (map != null)));
        }
    }
}
