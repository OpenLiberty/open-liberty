/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.mpmetrics;

import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.ERROR_TYPE;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.GEN_AI_OPERATION_NAME;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.GEN_AI_PROMPT_NAME;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.GEN_AI_TOOL_NAME;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.JSONRPC_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.MCP_METHOD_NAME;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.MCP_RESOURCE_URI;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.NETWORK_PROTOCOL_NAME;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.NETWORK_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.NETWORK_TRANSPORT;
import static io.openliberty.mcp.internal.mpmetrics.tags.McpTagConstants.RPC_RESPONSE_STATUS_CODE;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

import io.openliberty.mcp.internal.monitor.metrics.McpMetricAdapter;
import io.openliberty.mcp.internal.monitor.metrics.McpOperationStatAttributes;
import io.openliberty.mcp.internal.monitor.metrics.McpSessionStatAttributes;
import io.openliberty.mcp.internal.mpmetrics.constants.Constants;
import io.openliberty.microprofile.metrics50.SharedMetricRegistries;

/**
 *
 */
@Component(service = { McpMetricAdapter.class, ApplicationStateListener.class },
           configurationPolicy = ConfigurationPolicy.IGNORE)
public class MPMetricsMcpMetricsAdapterImpl implements McpMetricAdapter, ApplicationStateListener {

    static SharedMetricRegistries sharedMetricRegistries;

    private static final TraceComponent tc = Tr.register(MPMetricsMcpMetricsAdapterImpl.class);

    private static final String NO_APP_NAME_IDENTIFIER = "io.openliberty.microprofile.metrics50.internal.http.no.app.name";

    /**
     * Mapping between application name to a map of MCP stats ID mapped to
     * MicroProfile Metrics' Tags i.e. Map<appName, Map<McpStatID, Tags>>
     */
    private static Map<String, Map<String, Tag[]>> appNameToTagsMap = new ConcurrentHashMap<>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        MPMetricsMcpMetricsAdapterImpl.sharedMetricRegistries = sharedMetricRegistry;
    }

    public void unsetSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        if (MPMetricsMcpMetricsAdapterImpl.sharedMetricRegistries == sharedMetricRegistry) {
            MPMetricsMcpMetricsAdapterImpl.sharedMetricRegistries = null;
        }
    }

    @Override
    public void updateMcpOperationMetrics(McpOperationStatAttributes mcpStatAttributes, Duration duration) {
        if (sharedMetricRegistries == null) {
            return;
        }

        MetricRegistry vendorRegistry = sharedMetricRegistries.getOrCreate(MetricRegistry.VENDOR_SCOPE);

        Metadata md = new MetadataBuilder().withUnit("nanoseconds")
                                           .withName(Constants.MCP_SERVER_OPERATION_DURATION_NAME)
                                           .withDescription(Constants.MCP_SERVER_OPERATION_DURATION_DESC).build();

        String appName = getApplicationName();
        appName = appName == null ? NO_APP_NAME_IDENTIFIER : appName;

        String keyID = mcpStatAttributes.toString();

        // Key is the McpStatsID generated for each mcpStatsAttribute
        Map<String, Tag[]> attributesMap = appNameToTagsMap.computeIfAbsent(appName,
                                                                            x -> new ConcurrentHashMap<String, Tag[]>());
        Tag[] tags = attributesMap.computeIfAbsent(keyID, x -> retrieveOperationTags(mcpStatAttributes));

        Histogram mcpTimer = vendorRegistry.histogram(md, tags);
        mcpTimer.update(duration.toNanos());
    }

    private Tag[] retrieveOperationTags(McpOperationStatAttributes mcpStatAttributes) {

        Tag mcpMethodNameTag = new Tag(MCP_METHOD_NAME,
                                       mcpStatAttributes.mcpMethodName());

        String errorType = mcpStatAttributes.errorType();
        Tag errorTypeTag = new Tag(ERROR_TYPE, (errorType == null ? "" : errorType));

        String genAiPromptName = mcpStatAttributes.genAiPromptName();
        Tag genAiPromptNameTag = new Tag(GEN_AI_PROMPT_NAME,
                                         (genAiPromptName == null ? "" : genAiPromptName));

        String genAiToolName = mcpStatAttributes.genAiToolName();
        Tag genAiToolNameTag = new Tag(GEN_AI_TOOL_NAME,
                                       (genAiToolName == null ? "" : genAiToolName));

        String rpcResponseStatusCode = mcpStatAttributes.rpcResponseStatusCode();
        Tag rpcResponseStatusCodeTag = new Tag(RPC_RESPONSE_STATUS_CODE,
                                               (rpcResponseStatusCode == null ? "" : rpcResponseStatusCode));

        String genAiOperationName = mcpStatAttributes.genAiOperationName();
        Tag genAiOperationNameTag = new Tag(GEN_AI_OPERATION_NAME,
                                            (genAiOperationName == null ? "" : genAiOperationName));

        String jsonrpcProtocolVersion = mcpStatAttributes.jsonrpcProtocolVersion();
        Tag jsonrpcProtocolVersionTag = new Tag(JSONRPC_PROTOCOL_VERSION,
                                                (jsonrpcProtocolVersion == null ? "" : jsonrpcProtocolVersion));

        String mcpProtocolVersion = mcpStatAttributes.mcpProtocolVersion();
        Tag mcpProtocolVersionTag = new Tag(MCP_PROTOCOL_VERSION,
                                            (mcpProtocolVersion == null ? "" : mcpProtocolVersion));

        String networkProtocolName = mcpStatAttributes.networkProtocolName();
        Tag networkProtocolNameTag = new Tag(NETWORK_PROTOCOL_NAME,
                                             (networkProtocolName == null ? "" : networkProtocolName));

        String networkProtocolVersion = mcpStatAttributes.networkProtocolVersion();
        Tag networkProtocolVersionTag = new Tag(NETWORK_PROTOCOL_VERSION,
                                                (networkProtocolVersion == null ? "" : networkProtocolVersion));

        String networkTransport = mcpStatAttributes.networkTransport();
        Tag networkTransportTag = new Tag(NETWORK_TRANSPORT,
                                          (networkTransport == null ? "" : networkTransport));

        String mcpResourceUri = mcpStatAttributes.mcpResourceUri();
        Tag mcpResourceUriTag = new Tag(MCP_RESOURCE_URI,
                                        (mcpResourceUri == null ? "" : mcpResourceUri));

        Tag[] ret = new Tag[] { mcpMethodNameTag, errorTypeTag, genAiPromptNameTag, genAiToolNameTag,
                                rpcResponseStatusCodeTag, genAiOperationNameTag, jsonrpcProtocolVersionTag, mcpProtocolVersionTag,
                                networkProtocolNameTag, networkProtocolVersionTag, networkTransportTag, mcpResourceUriTag };

        return ret;
    }

    @Override
    public void updateMcpSessionMetrics(McpSessionStatAttributes mcpStatAttributes, Duration duration) {
        if (sharedMetricRegistries == null) {
            return;
        }

        MetricRegistry vendorRegistry = sharedMetricRegistries.getOrCreate(MetricRegistry.VENDOR_SCOPE);

        Metadata md = new MetadataBuilder().withUnit("nanoseconds")
                                           .withName(Constants.MCP_SERVER_SESSION_DURATION_NAME)
                                           .withDescription(Constants.MCP_SERVER_SESSION_DURATION_DESC).build();

        String appName = getApplicationName();
        appName = appName == null ? NO_APP_NAME_IDENTIFIER : appName;

        String keyID = mcpStatAttributes.toString();

        // Key is the McpStatsID generated for each mcpStatsAttribute
        Map<String, Tag[]> attributesMap = appNameToTagsMap.computeIfAbsent(appName,
                                                                            x -> new ConcurrentHashMap<String, Tag[]>());
        Tag[] tags = attributesMap.computeIfAbsent(keyID, x -> retrieveSessionTags(mcpStatAttributes));

        Histogram mcpTimer = vendorRegistry.histogram(md, tags);
        mcpTimer.update(duration.toNanos());
    }

    private Tag[] retrieveSessionTags(McpSessionStatAttributes mcpStatAttributes) {

        String errorType = mcpStatAttributes.errorType();
        Tag errorTypeTag = new Tag(ERROR_TYPE, (errorType == null ? "" : errorType));

        String jsonrpcProtocolVersion = mcpStatAttributes.jsonrpcProtocolVersion();
        Tag jsonrpcProtocolVersionTag = new Tag(JSONRPC_PROTOCOL_VERSION,
                                                (jsonrpcProtocolVersion == null ? "" : jsonrpcProtocolVersion));

        String mcpProtocolVersion = mcpStatAttributes.mcpProtocolVersion();
        Tag mcpProtocolVersionTag = new Tag(MCP_PROTOCOL_VERSION,
                                            (mcpProtocolVersion == null ? "" : mcpProtocolVersion));

        String networkProtocolName = mcpStatAttributes.networkProtocolName();
        Tag networkProtocolNameTag = new Tag(NETWORK_PROTOCOL_NAME,
                                             (networkProtocolName == null ? "" : networkProtocolName));

        String networkProtocolVersion = mcpStatAttributes.networkProtocolVersion();
        Tag networkProtocolVersionTag = new Tag(NETWORK_PROTOCOL_VERSION,
                                                (networkProtocolVersion == null ? "" : networkProtocolVersion));

        String networkTransport = mcpStatAttributes.networkTransport();
        Tag networkTransportTag = new Tag(NETWORK_TRANSPORT,
                                          (networkTransport == null ? "" : networkTransport));

        Tag[] ret = new Tag[] { errorTypeTag, jsonrpcProtocolVersionTag, mcpProtocolVersionTag,
                                networkProtocolNameTag, networkProtocolVersionTag, networkTransportTag, };

        return ret;
    }

    private String getApplicationName() {
        ComponentMetaData metaData = com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl
                                                                                           .getComponentMetaDataAccessor().getComponentMetaData();
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
        Map<String, Tag[]> map = appNameToTagsMap.remove(appName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, String.format(
                                             "Detected that application %s has stopped. Removed a corresponding Map<String, Attributes> entry? [%b]",
                                             appName, (map != null)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeMetricsForApp(String appName) {
        Map<String, Tag[]> map = appNameToTagsMap.remove(appName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, String.format(
                                             "Removing metrics for application %s. Removed a corresponding Map<String, Tag[]> entry? [%b]",
                                             appName, (map != null)));
        }
    }

}
