/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry20.logging.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.data.FFDCData;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.logs.LogRecordBuilder;

@Component(name = OpenTelemetryLogHandler.COMPONENT_NAME, service = { Handler.class }, configurationPolicy = ConfigurationPolicy.OPTIONAL, property = { "service.vendor=IBM" })
public class OpenTelemetryLogHandler implements SynchronousHandler {

    private static final TraceComponent tc = Tr.register(OpenTelemetryLogHandler.class, "TELEMETRY", "io.openliberty.microprofile.telemetry.internal.common.resources.MPTelemetry");

    public static final String COMPONENT_NAME = "io.openliberty.microprofile.telemetry20.logging.internal.OpenTelemetryLogHandler";

    protected static volatile boolean isInit = false;

    protected CollectorManager collectorMgr = null;

    // Config Source attribute name
    private static final String SOURCE_LIST_KEY = "source";

    private OpenTelemetryInfo openTelemetry;

    private List<String> sourcesList = new ArrayList<String>();

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "In activate()");
        }

        // Get the Runtime OpenTelemetry instance, if it is configured.
        this.openTelemetry = OpenTelemetryAccessor.getOpenTelemetryInfo();

        // Validate the configured sources
        this.sourcesList = validateSources(configuration);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "In deactivate()");
        }

        //To ensure that we don't hold any reference to the collector manager after
        //the component gets deactivated.
        collectorMgr = null;
    }

    @SuppressWarnings("unchecked")
    @Modified
    protected void modified(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "In modified()");
        }

        // Validate the configured sources
        List<String> newSources = validateSources(configuration);

        if (collectorMgr == null || isInit == false) {
            this.sourcesList = newSources;
            return;
        }

        try {
            // Old sources
            ArrayList<String> oldSources = new ArrayList<String>(sourcesList);

            //Sources to remove -> In Old Sources, the difference between oldSource and newSource
            ArrayList<String> sourcesToRemove = new ArrayList<String>(oldSources);
            sourcesToRemove.removeAll(newSources);
            collectorMgr.unsubscribe(this, convertToSourceIDList(sourcesToRemove));

            //Sources to Add -> In New Sources, the difference between newSource and oldSource
            ArrayList<String> sourcesToAdd = new ArrayList<String>(newSources);
            sourcesToAdd.removeAll(oldSources);
            collectorMgr.subscribe(this, convertToSourceIDList(sourcesToAdd));

            sourcesList = newSources; //new primary sourcesList
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /** {@inheritDoc} */
    @Override
    public void init(CollectorManager collectorManager) {
        try {
            this.collectorMgr = collectorManager;
            collectorMgr.subscribe(this, convertToSourceIDList(sourcesList));
            isInit = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    /*
     * Format the received event, by mapping the fields to the OTel Logs Data model
     */
    private void formatEvent(LogRecordBuilder builder, String source, Object event) {
        // Get the Liberty event type
        String eventType = MpTelemetryLogMappingUtils.getLibertyEventType(source);

        // Map the Liberty event to the Open Telemetry Logs Data Model
        MpTelemetryLogMappingUtils.mapLibertyEventToOpenTelemetry(builder, eventType, event);
    }

    /** {@inheritDoc} */
    @Override
    public void synchronousWrite(Object event) {
        OpenTelemetryInfo otelInstance = null;
        if (OpenTelemetryAccessor.isRuntimeEnabled()) {
            // Runtime OpenTelemetry instance
            otelInstance = this.openTelemetry;
        } else {
            // Application OpenTelemetry Instance
            otelInstance = OpenTelemetryAccessor.getOpenTelemetryInfo();
        }

        if (otelInstance != null && otelInstance.getEnabled()) {
            /*
             * Given an 'object' we must determine what type of log event it originates from.
             * Knowing that it is a *Data object, we can figure what type of source it is.
             */
            GenericData genData = null;
            if (event instanceof GenericData) {
                genData = (GenericData) event;
            } else {
                throw new IllegalArgumentException("Event is not an instance of GenericData.");
            }

            String eventSourceName = getSourceNameFromDataObject(genData);
            if (!eventSourceName.isEmpty()) {
                // Check if the event is a mapped OpenTelemetry log event, if it is, skip, to avoid infinite loop.
                if (isOTelMappedEvent(event)) {
                    // Do not map an already mapped OTel Log event.
                    return;
                }

                // Get the LogRecordBuilder from the OpenTelemetry Logs Bridge API.
                LogRecordBuilder builder = otelInstance.getOpenTelemetry().getLogsBridge().loggerBuilder(OpenTelemetryConstants.INSTRUMENTATION_NAME).build().logRecordBuilder();

                // Format the event to map the logs to the OpenTelemetry Logs Data Model.
                formatEvent(builder, eventSourceName, event);

                if (builder != null) {
                    // Emit the constructed logs to the configured OpenTelemetry Logs Exporter
                    builder.emit();
                }
            }
        }

    }

    /*
     * Check if the received event is an already mapped OTel event.
     */
    private boolean isOTelMappedEvent(Object event) {
        boolean isOTelMappedEvent = false;
        String eventMsg = "";
        // Check if the event is a mapped OpenTelemetry log event.
        if (event instanceof LogTraceData) {
            eventMsg = ((LogTraceData) event).getMessage();
        } else if (event instanceof FFDCData) {
            eventMsg = ((FFDCData) event).getMessage();
        }
        isOTelMappedEvent = eventMsg.contains(MpTelemetryLogFieldConstants.OTEL_SCOPE_INFO);
        return isOTelMappedEvent;
    }

    /*
     * Validate the configured sources.
     */
    private List<String> validateSources(Map<String, Object> config) {
        List<String> validatedList = new ArrayList<String>();
        if (config.containsKey(SOURCE_LIST_KEY)) {
            String[] sources = (String[]) config.get(SOURCE_LIST_KEY);
            if (sources != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Original sources list: ", Arrays.toString(sources));
                }
                for (String source : sources) {
                    if (!source.isEmpty()) {
                        if (getSourceName(source.trim()).isEmpty()) {
                            Tr.warning(tc, "CWMOT5005.mptelemetry.unknown.log.source", source);
                            continue;
                        }
                        validatedList.add(source);
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Validated sources list: ", Arrays.toString(validatedList.toArray()));
        }
        return validatedList;
    }

    /*
     * Given the sourceList in 'config' form, return the sourceID
     * Proper sourceID format is <source> | <location>
     */
    private List<String> convertToSourceIDList(List<String> sourceList) {
        List<String> sourceIDList = new ArrayList<String>();
        for (String source : sourceList) {
            String sourceName = getSourceName(source);
            if (!sourceName.equals("")) {
                sourceIDList.add(getSourceName(source) + "|" + CollectorConstants.MEMORY);
            }
        }
        return sourceIDList;
    }

    /*
     * Get the fully qualified source string from the config value
     */
    private String getSourceName(String source) {
        if (source.equals(CollectorConstants.MESSAGES_CONFIG_VAL))
            return CollectorConstants.MESSAGES_SOURCE;
        else if (source.equals(CollectorConstants.TRACE_CONFIG_VAL))
            return CollectorConstants.TRACE_SOURCE;
        else if (source.equals(CollectorConstants.FFDC_CONFIG_VAL))
            return CollectorConstants.FFDC_SOURCE;

        return "";
    }

    /*
     * Get the source name of the received data event object.
     */
    private String getSourceNameFromDataObject(Object event) {
        GenericData genData = (GenericData) event;
        String sourceName = genData.getSourceName();

        if (sourceName.equals(CollectorConstants.MESSAGES_SOURCE)) {
            return CollectorConstants.MESSAGES_SOURCE;
        } else if (sourceName.equals(CollectorConstants.TRACE_SOURCE)) {
            return CollectorConstants.TRACE_SOURCE;
        } else if (sourceName.equals(CollectorConstants.FFDC_SOURCE)) {
            return CollectorConstants.FFDC_SOURCE;
        } else {
            return "";
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(String arg0, BufferManager arg1) {
        //Not needed in a Synchronized Handler
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(String arg0, BufferManager arg1) {
        //Not needed in a Synchronized Handler
    }
}