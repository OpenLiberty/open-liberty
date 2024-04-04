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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.collector.Collector;
import com.ibm.ws.collector.Target;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.CollectorJsonUtils;
import com.ibm.ws.logging.data.LogTraceData;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfo;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.semconv.SemanticAttributes;

@Component(name = OpenTelemetryLogHandler.COMPONENT_NAME, service = {
	Handler.class, ServerQuiesceListener.class }, configurationPolicy = ConfigurationPolicy.OPTIONAL, property = { "service.vendor=IBM" })
public class OpenTelemetryLogHandler extends Collector implements ServerQuiesceListener {

	private static final TraceComponent tc = Tr.register(OpenTelemetryLogHandler.class);

	public static final String COMPONENT_NAME = "io.openliberty.microprofile.telemetry20.logging.internal.OpenTelemetryLogHandler";
	
	private TelemetryLogEmitter tle = null;
	
	private OpenTelemetry openTelemetry;
	
	@Override
	@Reference(name = EXECUTOR_SERVICE, service = ExecutorService.class)
	protected void setExecutorService(ServiceReference<ExecutorService> executorService) {
	    executorServiceRef.setReference(executorService);
	}

	@Override
	protected void unsetExecutorService(ServiceReference<ExecutorService> executorService) {
	    executorServiceRef.unsetReference(executorService);
	}

	@Override
	@Activate
	protected void activate(ComponentContext cc, Map<String, Object> configuration) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	    	    Tr.debug(tc, "In activate()");
	    }

	    this.openTelemetry = OpenTelemetryAccessor.getOpenTelemetryInfo("io.openliberty.microprofile.telemetry.runtime").getOpenTelemetry();

	    // Configure message as the only source.
	    Map<String, Object> config = setSourceListToConfig(configuration);
	    super.activate(cc, config);
	}	
	
	@Override
	@Deactivate
	protected void deactivate(ComponentContext cc, int reason) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	    	    Tr.debug(tc, "In deactivate()");
	    }
	    super.deactivate(cc, reason);
	}

	@Override
	@Modified
	protected void modified(Map<String, Object> configuration) {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "In modified()");
	    }
	    // Configure message as the only source.
	    Map<String, Object> config = setSourceListToConfig(configuration);
	    super.modified(config);

	}
    
	@Override
	public String getHandlerName() {
	    return COMPONENT_NAME;
	}

	@Override
	public Object formatEvent(String source, String location, Object event, String[] tags, int maxFieldLength) {
	    LogRecordBuilder builder = null;
	    String eventType = CollectorJsonUtils.getEventType(source, location);
	    if (eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE)) {
	        LogTraceData logData = (LogTraceData) event;
	        if (logData.getMessage().contains("scopeInfo:")) {
	            // Log a warning - This should only be the case when the logs exporter is set to logging.
	            // To prevent a loop (OL Logs -> OTel Logs -> OL Logs -> OTel Logs).
	            return null;
	        }
	        if (openTelemetry != null)
	            builder = openTelemetry.getLogsBridge().loggerBuilder(OpenTelemetryConstants.INSTRUMENTATION_NAME).build().logRecordBuilder();
	        
	        mapLibertyLogRecordToOTelLogRecord(builder, logData, eventType);
	    }
	    return builder;
	}
	

	private void mapLibertyLogRecordToOTelLogRecord(LogRecordBuilder builder, LogTraceData logData, String eventType, AttributesBuilder attributes) {
        boolean isMessageEvent = eventType.equals(CollectorConstants.MESSAGES_LOG_EVENT_TYPE);
	    
	    // Get message from LogData and set it in the LogRecordBuilder
	    String message = logData.getMessage();
	    builder.setBody(message);
	    
	    // Get Timestamp from LogData and set it in the LogRecordBuilder
	    builder.setTimestamp(logData.getDatetime(), TimeUnit.MILLISECONDS);
	    
	    // Get Log Level from LogData and set it in the LogRecordBuilder
	    String loglevel = logData.getLoglevel();
	    builder.setSeverity(wsLevelToSeverity(loglevel));
	    
	    // Get Log Severity from LogData and set it in the LogRecordBuilder
	    String logSeverity = logData.getSeverity();
	    builder.setSeverityText(logSeverity);

	    // Add Thread information to Attributes Builder
	    attributes.put(SemanticAttributes.THREAD_NAME, logData.getThreadName());
	    attributes.put(SemanticAttributes.THREAD_ID, logData.getThreadId());
	    
	    // Add Throwable information to Attribute Builder
	    String exceptionName = logData.getExceptionName();
	    String throwable = logData.getThrowable();
	    if (throwable != null) {
	    	    attributes.put(SemanticAttributes.EXCEPTION_MESSAGE, exceptionName);
	    	    attributes.put(SemanticAttributes.EXCEPTION_STACKTRACE, throwable);
	    }
	    
	    // Add additional log information from LogData to Attributes Builder
	    attributes.put(LogTraceData.getModuleKey(0, isMessageEvent), logData.getModule())
	              .put(LogTraceData.getMessageIdKey(0, isMessageEvent), logData.getMessageId())
	              .put(LogTraceData.getMethodNameKey(0, isMessageEvent), logData.getMethodName())
	              .put(LogTraceData.getClassNameKey(0, isMessageEvent), logData.getClassName())
	              .put(LogTraceData.getSequenceKey(0, isMessageEvent), logData.getSequence());
	    
	    // Set the Attributes to the builder.
	    builder.setAllAttributes(attributes.build());
	    
	    // span context
	    //builder.setContext(Context.current());
	}
	
	private static Severity wsLevelToSeverity(String level) {
	    if (level.equals(WsLevel.FATAL.toString())) {
              return Severity.FATAL;
	    }
	    else if (level.equals(WsLevel.SEVERE.toString()) || level.equals("SystemErr")) {
              return Severity.ERROR;
            }
	    else if (level.equals(WsLevel.WARNING.toString())) {
              return Severity.WARN;
            }
	    else if (level.equals(WsLevel.AUDIT.toString())) {
              return Severity.INFO2;
            }
	    else if (level.equals(WsLevel.INFO.toString())) {
              return Severity.INFO;
            }
	    else if (level.equals(WsLevel.CONFIG.toString()) || level.equals("SystemOut")) {
              return Severity.DEBUG4;
            }
            else if (level.equals(WsLevel.DETAIL.toString())) {
              return Severity.DEBUG3;
            }
            else if (level.equals(WsLevel.FINE.toString())) {
              return Severity.DEBUG2;
            }
            else if (level.equals(WsLevel.FINER.toString())) {
              return Severity.DEBUG;
            }
            else if (level.equals(WsLevel.FINEST.toString())) {
              return Severity.TRACE;
            }
            else {
              return Severity.FATAL;
            }
	}
	
	@Override
	public Target getTarget() {
	    if (tle == null) {
	        tle = new TelemetryLogEmitter();
	    }
	    return tle;
	}
	
	public OpenTelemetry getOpenTelemetry() {
	    OpenTelemetry openTelemetry = null;
	    ClassLoader newClassLoader = OpenTelemetry.noop().getClass().getClassLoader();     
         
	    OpenTelemetryInfo openTelemetryInfo = OpenTelemetryAccessor.getServerOpenTelemetryInfo(newClassLoader);
	    openTelemetry = openTelemetryInfo.getOpenTelemetry();
	    return openTelemetry;
	}
	
	private Map<String, Object> setSourceListToConfig(Map<String, Object> configuration) {
	    Map<String, Object> config = new HashMap<>(configuration);
	    String[] sourceList = new String[]{"message"};
	    config.put(SOURCE_LIST_KEY, sourceList);
	    return config;
	}

	@Override
	public void serverStopping() {
	    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	        Tr.debug(tc, "Server stop initiated - stopping all tasks.");
	    }
	    stopAllTasks();
	}
}