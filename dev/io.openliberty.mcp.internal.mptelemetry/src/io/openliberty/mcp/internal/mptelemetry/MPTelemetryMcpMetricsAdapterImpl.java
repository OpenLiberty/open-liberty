/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.mptelemetry;

import static io.opentelemetry.semconv.incubating.ErrorIncubatingAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_PROMPT_NAME;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_TOOL_NAME;
import static io.opentelemetry.semconv.incubating.JsonrpcIncubatingAttributes.JSONRPC_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.incubating.McpIncubatingAttributes.MCP_METHOD_NAME;
import static io.opentelemetry.semconv.incubating.McpIncubatingAttributes.MCP_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.incubating.McpIncubatingAttributes.MCP_RESOURCE_URI;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_PROTOCOL_NAME;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_TRANSPORT;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_RESPONSE_STATUS_CODE;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

import io.openliberty.mcp.internal.monitor.metrics.McpMetricAdapter;
import io.openliberty.mcp.internal.monitor.metrics.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitor.metrics.McpSessionStatAttributes;
import io.openliberty.mcp.internal.mptelemetry.constants.Constants;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.openliberty.microprofile.telemetry.spi.OpenTelemetryInfo;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.context.Context;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPTelemetryMcpMetricsAdapterImpl implements McpMetricAdapter {

    private static final TraceComponent tc = Tr.register(MPTelemetryMcpMetricsAdapterImpl.class);

    private static final String INSTR_SCOPE = "io.openliberty.mcp";

    private static final String NO_APP_NAME_IDENTIFIER = "io.openliberty.mcp.no.app.name";

    private static final double NANO_CONVERSION = 0.000_000_001;
    private static final List<Double> BUCKET_BOUNDARIES_LIST = List.of(0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0);

    /**
     * Mapping between application name to a map of MCP stats ID mapped to OpenTelemetry attributes
     * i.e. Map<appName, Map<McpStatID, Attributes>>
     */
    private static Map<String, Map<String, Attributes>> appNameToAttributesMap = new ConcurrentHashMap<>();

    //All access to threadUnsafeHTTPHistogramMap must be synchronized using httpHistogramMapLock
    private final WeakHashMap<OpenTelemetry, DoubleHistogram> threadUnsafeMcpOperationHistogramMap = new WeakHashMap<>();
    private final ReadWriteLock mcpOperationHistogramMapLock = new ReentrantReadWriteLock();

    private final WeakHashMap<OpenTelemetry, DoubleHistogram> threadUnsafeMcpSessionHistogramMap = new WeakHashMap<>();
    private final ReadWriteLock mcpSessionHistogramMapLock = new ReentrantReadWriteLock();


    @Override
    public void updateMcpOperationMetrics(McpOperationStatAttributes mcpStatAttributes, Duration duration) {
        try {
            OpenTelemetryInfo otelInfo = OpenTelemetryAccessor.getOpenTelemetryInfo();
            if (!otelInfo.isEnabled()) {
                return;
            }

            OpenTelemetry otelInstance = otelInfo.getOpenTelemetry();

            DoubleHistogram mcpHistogram = getMcpOperationHistogram(otelInstance);

            Context ctx = Context.current();

            double seconds = duration.toNanos() * NANO_CONVERSION;

            String appName = getApplicationName();
            appName = appName == null ? NO_APP_NAME_IDENTIFIER : appName;

            String keyID = mcpStatAttributes.mcpOperationStat_ID();

            // Key is the mcpStatsID generated for each mcpStatsAttribute
            Map<String, Attributes> attributesMap = appNameToAttributesMap.computeIfAbsent(appName, x -> new ConcurrentHashMap<String, Attributes>());
            Attributes attributes = attributesMap.computeIfAbsent(keyID, x -> retrieveOperationAttributes(mcpStatAttributes));

            mcpHistogram.record(seconds, attributes, ctx);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error updating MCP operation metrics", e);
            }
        }
    }

    private Attributes retrieveOperationAttributes(McpOperationStatAttributes mcpStatAttributes) {

        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesBuilder.put(MCP_METHOD_NAME, mcpStatAttributes.mcpMethodName());

        String errorType = mcpStatAttributes.errorType();
        if (errorType != null) {
            attributesBuilder.put(ERROR_TYPE, errorType);
        }

        String genAiPromptName = mcpStatAttributes.genAiPromptName();
        if (genAiPromptName != null) {
            attributesBuilder.put(GEN_AI_PROMPT_NAME, genAiPromptName);
        }

        String genAiToolName = mcpStatAttributes.genAiToolName();
        if (genAiToolName != null) {
            attributesBuilder.put(GEN_AI_TOOL_NAME, genAiToolName);
        }

        String rpcResponseStatusCode = mcpStatAttributes.rpcResponseStatusCode();
        if (rpcResponseStatusCode != null) {
            attributesBuilder.put(RPC_RESPONSE_STATUS_CODE, rpcResponseStatusCode);
        }

        String genAiOperationName = mcpStatAttributes.genAiOperationName();
        if (genAiOperationName != null) {
            attributesBuilder.put(GEN_AI_OPERATION_NAME, genAiOperationName);
        }

        String jsonrpcProtocolVersion = mcpStatAttributes.jsonrpcProtocolVersion();
        if (jsonrpcProtocolVersion != null) {
            attributesBuilder.put(JSONRPC_PROTOCOL_VERSION, jsonrpcProtocolVersion);
        }

        String mcpProtocolVersion = mcpStatAttributes.mcpProtocolVersion();
        if (mcpProtocolVersion != null) {
            attributesBuilder.put(MCP_PROTOCOL_VERSION, mcpProtocolVersion);
        }

        String networkProtocolName = mcpStatAttributes.networkProtocolName();
        if (networkProtocolName != null) {
            attributesBuilder.put(NETWORK_PROTOCOL_NAME, networkProtocolName);
        }

        String networkProtocolVersion = mcpStatAttributes.networkProtocolVersion();
        if (networkProtocolVersion != null) {
            attributesBuilder.put(NETWORK_PROTOCOL_VERSION, networkProtocolVersion);
        }

        String networkTransport = mcpStatAttributes.networkTransport();
        if (networkTransport != null) {
            attributesBuilder.put(NETWORK_TRANSPORT, networkTransport);
        }

        String mcpResourceUri = mcpStatAttributes.mcpResourceUri();
        if (mcpResourceUri != null) {
            attributesBuilder.put(MCP_RESOURCE_URI, mcpResourceUri);
        }

        return attributesBuilder.build();
    }

    /*
     * We can re-use the (histogram) Meter created here.
     * The Meter is built using the same static values each time.
     * The instrument that is recorded/updated is distinct for each
     * MCP method/tool/status combination (corresponds with resolved attributes).
     *
     * However we cannot share it across multiple instances of OpenTelemetry
     */
    private DoubleHistogram getMcpOperationHistogram(OpenTelemetry otelInstance) {

        try {
            mcpOperationHistogramMapLock.readLock().lock();
            if (threadUnsafeMcpOperationHistogramMap.containsKey(otelInstance)) {
                return threadUnsafeMcpOperationHistogramMap.get(otelInstance);
            }
        } finally {
            mcpOperationHistogramMapLock.readLock().unlock();
        }

        try {
            mcpOperationHistogramMapLock.writeLock().lock();
            return threadUnsafeMcpOperationHistogramMap.computeIfAbsent(otelInstance,
                                                                        (OpenTelemetry openTelemetry) -> openTelemetry.getMeterProvider().get(INSTR_SCOPE)
                                                                                                                      .histogramBuilder(Constants.MCP_SERVER_OPERATION_DURATION_NAME)
                                                                                                                      .setUnit(OpenTelemetryConstants.OTEL_SECONDS_UNIT)
                                                                                                                      .setDescription(Constants.MCP_SERVER_OPERATION_DURATION_DESC)
                                                                                                                      .setExplicitBucketBoundariesAdvice(BUCKET_BOUNDARIES_LIST)
                                                                                                                      .build());
        } finally {
            mcpOperationHistogramMapLock.writeLock().unlock();
        }
    }

    @Override
    public void updateMcpSessionMetrics(McpSessionStatAttributes mcpStatAttributes, Duration duration) {
        try {
            OpenTelemetry otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo().getOpenTelemetry();

            DoubleHistogram mcpHistogram = getMcpSessionHistogram(otelInstance);

            Context ctx = Context.current();

            double seconds = duration.toNanos() * NANO_CONVERSION;

            String appName = getApplicationName();
            appName = appName == null ? NO_APP_NAME_IDENTIFIER : appName;

            String keyID = mcpStatAttributes.mcpSessionStat_ID();

            // Key is the mcpStatsID generated for each mcpStatsAttribute
            Map<String, Attributes> attributesMap = appNameToAttributesMap.computeIfAbsent(appName, x -> new ConcurrentHashMap<String, Attributes>());
            Attributes attributes = attributesMap.computeIfAbsent(keyID, x -> retrieveSessionAttributes(mcpStatAttributes));

            mcpHistogram.record(seconds, attributes, ctx);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Error updating MCP session metrics", e);
            }
        }
    }

    private Attributes retrieveSessionAttributes(McpSessionStatAttributes mcpStatAttributes) {

        AttributesBuilder attributesBuilder = Attributes.builder();

        String errorType = mcpStatAttributes.errorType();
        if (errorType != null) {
            attributesBuilder.put(ERROR_TYPE, errorType);
        }

        String jsonrpcProtocolVersion = mcpStatAttributes.jsonrpcProtocolVersion();
        if (jsonrpcProtocolVersion != null) {
            attributesBuilder.put(JSONRPC_PROTOCOL_VERSION, jsonrpcProtocolVersion);
        }

        String mcpProtocolVersion = mcpStatAttributes.mcpProtocolVersion();
        if (mcpProtocolVersion != null) {
            attributesBuilder.put(MCP_PROTOCOL_VERSION, mcpProtocolVersion);
        }

        String networkProtocolName = mcpStatAttributes.networkProtocolName();
        if (networkProtocolName != null) {
            attributesBuilder.put(NETWORK_PROTOCOL_NAME, networkProtocolName);
        }

        String networkProtocolVersion = mcpStatAttributes.networkProtocolVersion();
        if (networkProtocolVersion != null) {
            attributesBuilder.put(NETWORK_PROTOCOL_VERSION, networkProtocolVersion);
        }

        String networkTransport = mcpStatAttributes.networkTransport();
        if (networkTransport != null) {
            attributesBuilder.put(NETWORK_TRANSPORT, networkTransport);
        }

        return attributesBuilder.build();
    }

    /*
     * We can re-use the (histogram) Meter created here.
     * The Meter is built using the same static values each time.
     * The instrument that is recorded/updated is distinct for each
     * MCP session attribute combination (corresponds with resolved attributes).
     *
     * However we cannot share it across multiple instances of OpenTelemetry
     */
    private DoubleHistogram getMcpSessionHistogram(OpenTelemetry otelInstance) {

        try {
            mcpSessionHistogramMapLock.readLock().lock();
            if (threadUnsafeMcpSessionHistogramMap.containsKey(otelInstance)) {
                return threadUnsafeMcpSessionHistogramMap.get(otelInstance);
            }
        } finally {
            mcpSessionHistogramMapLock.readLock().unlock();
        }

        try {
            mcpSessionHistogramMapLock.writeLock().lock();
            return threadUnsafeMcpSessionHistogramMap.computeIfAbsent(otelInstance,
                                                                      (OpenTelemetry openTelemetry) -> openTelemetry.getMeterProvider().get(INSTR_SCOPE)
                                                                                                                    .histogramBuilder(Constants.MCP_SERVER_SESSION_DURATION_NAME)
                                                                                                                    .setUnit(OpenTelemetryConstants.OTEL_SECONDS_UNIT)
                                                                                                                    .setDescription(Constants.MCP_SERVER_SESSION_DURATION_DESC)
                                                                                                                    .setExplicitBucketBoundariesAdvice(BUCKET_BOUNDARIES_LIST)
                                                                                                                    .build());
        } finally {
            mcpSessionHistogramMapLock.writeLock().unlock();
        }
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

    /** {@inheritDoc} */
    @Override
    public void removeMetricsForApp(String appName) {
        Map<String, Attributes> map = appNameToAttributesMap.remove(appName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc,
                     String.format("Removing metrics for application %s. Removed a corresponding Map<String, Attributes> entry? [%b]", appName, (map != null)));
        }
    }
}
