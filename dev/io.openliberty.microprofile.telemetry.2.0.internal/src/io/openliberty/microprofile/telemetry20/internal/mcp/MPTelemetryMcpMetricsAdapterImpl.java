/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.mcp;

import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.ErrorIncubatingAttributes.ERROR_TYPE;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.GenAiIncubatingAttributes.GEN_AI_PROMPT_NAME;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.GenAiIncubatingAttributes.GEN_AI_TOOL_NAME;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.JsonrpcIncubatingAttributes.JSONRPC_PROTOCOL_VERSION;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.McpIncubatingAttributes.MCP_METHOD_NAME;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.McpIncubatingAttributes.MCP_PROTOCOL_VERSION;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.McpIncubatingAttributes.MCP_RESOURCE_URI;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.NetworkIncubatingAttributes.NETWORK_PROTOCOL_NAME;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.NetworkIncubatingAttributes.NETWORK_PROTOCOL_VERSION;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.NetworkIncubatingAttributes.NETWORK_TRANSPORT;
import static io.openliberty.microprofile.telemetry20.internal.mcp.attributes.RpcIncubatingAttributes.RPC_RESPONSE_STATUS_CODE;

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

import io.openliberty.mcp.internal.monitor.McpStatAttributes;
import io.openliberty.mcp.internal.monitor.metrics.McpMetricAdapter;
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
@Component(service = { McpMetricAdapter.class, ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPTelemetryMcpMetricsAdapterImpl implements McpMetricAdapter, ApplicationStateListener {

    private static final TraceComponent tc = Tr.register(MPTelemetryMcpMetricsAdapterImpl.class);

    private static final String INSTR_SCOPE = "io.openliberty.microprofile.telemetry20.internal.mcp";

    private static final String NO_APP_NAME_IDENTIFIER = "io.openliberty.microprofile.telemetry20.internal.mcp.no.app.name";

    private static final double NANO_CONVERSION = 0.000000001;
    private static final Double[] BUCKET_BOUNDARIES = { 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0 };
    private static final List<Double> BUCKET_BOUNDARIES_LIST = Arrays.asList(BUCKET_BOUNDARIES);

    /**
     * Mapping between application name to a map of HTTP stats ID mapped to OpenTelemetry attributes
     * i.e. Map<appName, Map<HttpStatID, Attributes>>
     */
    private static Map<String, Map<String, Attributes>> appNameToAttributesMap = new ConcurrentHashMap<String, Map<String, Attributes>>();

    //All access to threadUnsafeHTTPHistogramMap must be synchronized using httpHistogramMapLock
    private final WeakHashMap<OpenTelemetry, DoubleHistogram> threadUnsafeMcpHistogramMap = new WeakHashMap<OpenTelemetry, DoubleHistogram>();
    private final ReadWriteLock mcpHistogramMapLock = new ReentrantReadWriteLock();

    @Override
    public void updateMcpMetrics(McpStatAttributes mcpStatAttributes, Duration duration) {

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
                             String.format("Unable to resolve an OpenTelemetry instance for the McpStatAttributes [%s]", mcpStatAttributes.toString()));
                }
                //do nothing - return
                return;
            }
        }

        DoubleHistogram mcpHistogram = getMcpHistogram(otelInstance);

        Context ctx = Context.current();

        double seconds = duration.toNanos() * NANO_CONVERSION;

        String appName = getApplicationName();
        appName = appName == null ? NO_APP_NAME_IDENTIFIER : appName;

        String keyID = mcpStatAttributes.getMcpStat_ID();

        // Key is the mcpStasID generated for each httpStatsAttribute
        Map<String, Attributes> attributesMap = appNameToAttributesMap.computeIfAbsent(appName, x -> new ConcurrentHashMap<String, Attributes>());
        Attributes attributes = attributesMap.computeIfAbsent(keyID, x -> retrieveAttributes(mcpStatAttributes));

        mcpHistogram.record(seconds, attributes, ctx);

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

    private Attributes retrieveAttributes(McpStatAttributes mcpStatAttributes) {

        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesBuilder.put(MCP_METHOD_NAME, mcpStatAttributes.getMcpMethodName());

        String errorType = mcpStatAttributes.getErrorType();
        if (errorType != null) {
            attributesBuilder.put(ERROR_TYPE, errorType);
        }

        String genAiPromptName = mcpStatAttributes.getGenAiPromptName();
        if (genAiPromptName != null) {
            attributesBuilder.put(GEN_AI_PROMPT_NAME, genAiPromptName);
        }

        String genAiToolName = mcpStatAttributes.getGenAiToolName();
        if (genAiPromptName != null) {
            attributesBuilder.put(GEN_AI_TOOL_NAME, genAiToolName);
        }

        String rpcResponseStatusCode = mcpStatAttributes.getRpcResponseStatusCode();
        if (rpcResponseStatusCode != null) {
            attributesBuilder.put(RPC_RESPONSE_STATUS_CODE, rpcResponseStatusCode);
        }

        String genAiOperationName = mcpStatAttributes.getGenAiOperationName();
        if (genAiOperationName != null) {
            attributesBuilder.put(GEN_AI_OPERATION_NAME, genAiOperationName);
        }

        String jsonrpcProtocolVersion = mcpStatAttributes.getJsonrpcProtocolVersion();
        if (jsonrpcProtocolVersion != null) {
            attributesBuilder.put(JSONRPC_PROTOCOL_VERSION, jsonrpcProtocolVersion);
        }

        String mcpProtocolVersion = mcpStatAttributes.getMcpProtocolVersion();
        if (mcpProtocolVersion != null) {
            attributesBuilder.put(MCP_PROTOCOL_VERSION, mcpProtocolVersion);
        }

        String networkProtocolName = mcpStatAttributes.getNetworkProtocolName();
        if (networkProtocolName != null) {
            attributesBuilder.put(NETWORK_PROTOCOL_NAME, networkProtocolName);
        }

        String networkProtocolVersion = mcpStatAttributes.getNetworkProtocolVersion();
        if (networkProtocolVersion != null) {
            attributesBuilder.put(NETWORK_PROTOCOL_VERSION, networkProtocolVersion);
        }

        String networkTransport = mcpStatAttributes.getNetworkTransport();
        if (networkTransport != null) {
            attributesBuilder.put(NETWORK_TRANSPORT, networkTransport);
        }

        String mcpResourceUri = mcpStatAttributes.getMcpResourceUri();
        if (mcpResourceUri != null) {
            attributesBuilder.put(MCP_RESOURCE_URI, mcpResourceUri);
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
    private DoubleHistogram getMcpHistogram(OpenTelemetry otelInstance) {

        try {
            mcpHistogramMapLock.readLock().lock();
            if (threadUnsafeMcpHistogramMap.containsKey(otelInstance)) {
                return threadUnsafeMcpHistogramMap.get(otelInstance);
            }
        } finally {
            mcpHistogramMapLock.readLock().unlock();
        }

        try {
            mcpHistogramMapLock.writeLock().lock();
            return threadUnsafeMcpHistogramMap.computeIfAbsent(otelInstance,
                                                               (OpenTelemetry openTelemetry) -> openTelemetry.getMeterProvider().get(INSTR_SCOPE)
                                                                               .histogramBuilder(OpenTelemetryConstants.MCP_SERVER_OPERATION_DURATION_NAME)
                                                                               .setUnit(OpenTelemetryConstants.OTEL_SECONDS_UNIT)
                                                                               .setDescription(OpenTelemetryConstants.MCP_SERVER_OPERATION_DURATION_DESC)
                                                                               .setExplicitBucketBoundariesAdvice(BUCKET_BOUNDARIES_LIST).build());
        } finally {
            mcpHistogramMapLock.writeLock().unlock();
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
