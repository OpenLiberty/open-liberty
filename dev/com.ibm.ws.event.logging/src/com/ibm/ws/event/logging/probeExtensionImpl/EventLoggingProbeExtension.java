/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.logging.probeExtensionImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.logging.hpel.LogRecordContext;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.event.logging.internal.EventLoggingConstants;
import com.ibm.wsspi.probeExtension.ContextInfoRequirement;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;

public class EventLoggingProbeExtension implements ProbeExtension {

	private static final TraceComponent tc = Tr.register(
			EventLoggingProbeExtension.class, "eventLogging",
			"com.ibm.ws.event.logging.internal.resources.LoggingMessages");
	private static final Logger _logger = Logger.getLogger(
			"EventLogging",
			"com.ibm.ws.event.logging.internal.resources.LoggingMessages");

	/** Rate at which sampling should happen in Event Logging feature **/
	private static int sampleRate = 0;

	/** Log at entry or exit of events' **/
	private static boolean entryEnabled = false;
	private static boolean exitEnabled = false;

	/** comma separated list of event types to log for **/
	private static List<String> eventTypes = null;

	/** Minimum duration of a request, before we dump exit entries to log **/
	private static int minDuration = 0;

	/** indicates if context information details will be included in output **/
	private static boolean includeContextInfo = true;

	@Activate
	protected void activate(Map<String, Object> configuration) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Activating " + this);
		}
		configureService(configuration);
		if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
			Tr.info(tc, "EVENT_LOGGING_STARTUP_INFO", sampleRate, minDuration,
					eventTypes);
		}
	}

	@Modified
	protected void modified(Map<String, Object> configuration) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, " Modified");
		}
		configureService(configuration);

	}

	@Deactivate
	protected void deactivate(int reason) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Deactivating " + this, "reason=" + reason);
		}
	}

	@Override
	public void processEntryEvent(Event event, RequestContext requestContext) {
		
		if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) {
			Tr.debug(tc, "processEntryEvent " + event);
		}

		if (_logger.isLoggable(Level.INFO)) {
			try {

				LogRecordContext.addExtension("eventType", event.getType());
				if (includeContextInfo && event.getContextInfo() != null) {
					LogRecordContext.addExtension("contextInfo", event.getContextInfo()
							.toString());
				}
				if (includeContextInfo && event.getContextInfo() != null) {
					_logger.info("BEGIN " + "requestID="
							+ requestContext.getRequestId().getId() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR + "eventType="
							+ event.getType() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR + "contextInfo="
							+ event.getContextInfo());
				} else {
					_logger.info("BEGIN " + "requestID="
							+ requestContext.getRequestId().getId() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR + "eventType="
							+ event.getType());
				}
			} finally {

				LogRecordContext.removeExtension("eventType");
				if (includeContextInfo && event.getContextInfo() != null) {
					LogRecordContext.removeExtension("contextInfo");
				}
			}
		}
	}

	@Override
	public void processExitEvent(Event event, RequestContext requestContext) {
		
		if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) {
			Tr.debug(tc, "processExitEvent " + event);
		}
		
		if (_logger.isLoggable(Level.INFO)) {
			try {
				double duration = (event.getEndTime() - event.getStartTime()) / 1000000.0; // convert nanoseconds to milliseconds
				if (duration >= getMinDuration()) {
					LogRecordContext.addExtension("eventType", event.getType());
					if (includeContextInfo && event.getContextInfo() != null) {
						LogRecordContext.addExtension("contextInfo", event
								.getContextInfo().toString());
					}
					if (includeContextInfo && event.getContextInfo() != null) {
						_logger.info("END " + "requestID="
								+ requestContext.getRequestId().getId() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR
								+ "eventType=" + event.getType() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR
								+ "contextInfo=" + event.getContextInfo() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR
								+ "duration=" + String.format("%.3f", duration)
								+ "ms");
					} else {
						_logger.info("END " + "requestID="
								+ requestContext.getRequestId().getId() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR
								+ "eventType=" + event.getType() + EventLoggingConstants.EVENT_LOG_MESSAGE_SEPARATOR
								+ "duration=" + String.format("%.3f", duration)
								+ "ms");
					}
				}

			} finally {
				LogRecordContext.removeExtension("eventType");
				if (includeContextInfo && event.getContextInfo() != null) {
					LogRecordContext.removeExtension("contextInfo");
				}

			}
		}
	}
	
	@Override
	public void processCounter(Event event) {
		//Do nothing as invokeForCounter is false.
	}
	
	public static int getMinDuration() {
		return minDuration;
	}

	@Override
	public boolean invokeForRootEventsOnly() {
		return false;
	}

	@Override
	public int getRequestSampleRate() {
		return sampleRate;
	}

	@Override
	public int getContextInfoRequirement() {
		if (includeContextInfo)
			return ContextInfoRequirement.EVENTS_MATCHING_SPECIFIED_EVENT_TYPES;
		else
			return ContextInfoRequirement.NONE;
	}

	@Override
	public List<String> invokeForEventTypes() {
		return eventTypes;
	}

	@Override
	public boolean invokeForEventEntry() {
		return entryEnabled;

	}

	@Override
	public boolean invokeForEventExit() {
		return exitEnabled;
	}
	
	@Override
	public boolean invokeForCounter() {
		return false;
	}

	/**
	 * Configuration for activation and modification..
	 * 
	 * @param configuration
	 */
	private void configureService(Map<String, Object> configuration) {

		boolean sampleRateSet = false, logModeSet = false, minDurationSet = false, includeContextInfoSet = false, includeTypesSet = false;

		for (Map.Entry<String, Object> configElement : configuration.entrySet()) {
			String configKey = configElement.getKey();
			Object configValue = configElement.getValue();

			if (configKey.compareTo(EventLoggingConstants.EL_SAMPLING_RATE) == 0) {

				sampleRate = Integer.parseInt(configValue.toString());
				//Invalid input reset sampleRate to 1.
				if(sampleRate < 1)
					sampleRate = 1;
				sampleRateSet = true;

			} else if (configKey.compareTo(EventLoggingConstants.EL_LOG_MODE) == 0) {
				if (configValue.toString().equals("entry")) {
					entryEnabled = true;
					exitEnabled = false;
				} else if (configValue.toString().equals("exit")) {
					entryEnabled = false;
					exitEnabled = true;

				} else if (configValue.toString().equals("entryExit")) {
					entryEnabled = true;
					exitEnabled = true;
				}
				logModeSet = true;
			} else if (configKey
					.compareTo(EventLoggingConstants.EL_EVENT_TYPES) == 0) {

				String includeTypesStr = configValue.toString();
				if (!includeTypesStr.equals("all")) {
					eventTypes = new ArrayList<String>();
					for (String eventType : includeTypesStr.split(",")) {
						eventTypes.add(eventType.trim());
					}
				}else{
					//Bug fix
					eventTypes = null; 
				}

				includeTypesSet = true;
			} else if (configKey
					.compareTo(EventLoggingConstants.EL_MIN_DURATION) == 0) {
				minDuration = Integer.parseInt(configValue.toString());
				minDurationSet = true;

			} else if (configKey
					.compareTo(EventLoggingConstants.EL_INCLUDE_CONTEXT_INFO) == 0) {
				includeContextInfo = Boolean.parseBoolean(configValue.toString());
				includeContextInfoSet = true;
			}
		}

		if (!sampleRateSet) {
			sampleRate = 1;
		}
		if (!logModeSet) {
			entryEnabled = false;
			exitEnabled = true;
		}
		if (!minDurationSet) {
			minDuration = 500;
		}
		if (!includeTypesSet) {
			eventTypes = new ArrayList<String>() {
				{
					add("websphere.servlet.service");
				}
			};
		}
		if (!includeContextInfoSet) {
			includeContextInfo = true;
		}

		if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) {
			StringBuffer eventLoggingCfg = new StringBuffer();
			eventLoggingCfg.append(String.format("%n"));
			eventLoggingCfg.append("-------------------Event Logging Settings-------------------" + String.format("%n"));
			eventLoggingCfg.append("Sample rate: " + sampleRate + String.format("%n"));
			eventLoggingCfg.append("Context info requirement: " + ((includeContextInfo == true) ? ContextInfoRequirement.EVENTS_MATCHING_SPECIFIED_EVENT_TYPES
								     : ContextInfoRequirement.NONE) + String.format("%n"));
			eventLoggingCfg.append("Entry Enabled: " + entryEnabled + String.format("%n"));
			eventLoggingCfg.append("Exit Enabled: " + exitEnabled + String.format("%n"));
			eventLoggingCfg.append("Minimum duration of request (ms): " + minDuration + String.format("%n"));
			eventLoggingCfg.append("Event Types: " + eventTypes + String.format("%n"));
			eventLoggingCfg.append("-------------------------------------------------------------");
			
			Tr.debug(tc, "Event logging configuration", eventLoggingCfg);
		}
	}
}
