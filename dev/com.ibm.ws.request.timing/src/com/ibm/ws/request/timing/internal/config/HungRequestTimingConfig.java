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
package com.ibm.ws.request.timing.internal.config;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.ws.request.timing.RequestTimingConstants;
import com.ibm.ws.request.timing.config.Timing;
import com.ibm.wsspi.requestContext.ContextInfoArray;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;

public class HungRequestTimingConfig extends RequestTimingConfig {
	
	private final boolean keepStatistics;
	private final boolean interruptHungRequest;
	
	public HungRequestTimingConfig(int contextInfoRequirement, Map<String, List<Timing>> hungRequestTiming, boolean interruptHungRequest){
		//Sample is always 1 for hung request detection
		super(RequestTimingConstants.SAMPLE_RATE, contextInfoRequirement, hungRequestTiming);
		
		// We want to keep the interrupt part of this turned off unless it was explicitly asked for.
		boolean isInterruptHungRequest = interruptHungRequest;
		if (hungRequestTiming != null) {
			for (List<Timing> typeList : hungRequestTiming.values()) {
				if (typeList != null) {
					for (Timing t : typeList) {
						if (t.interruptHungRequest() == true) {
							isInterruptHungRequest = true;
						}
					}
				}
			}
		}
		this.interruptHungRequest = isInterruptHungRequest; 
		
		// See if the configuration contains more than just the default thresholds.  If it does, we'll
		// be keeping statistics as to which embedded timing configurations are used.
		keepStatistics = (countTimingConfigs() > 1);
	}
	
	public HungRequestTimingConfig() {
		super();
		keepStatistics = false;
		interruptHungRequest = false;
	}

	/**
	 * Keep track of which timing elements are used to process which requests.  This
	 * information is printed by the dump introspector to help operations determine which
	 * of their timing configurations are being used.
	 */
	public void incrementTimingConfigForRequest(RequestContext requestContext,
			ConcurrentHashMap<String, AtomicLong> requestCounts) {

		// To keep track of request count regardless of keepStatistics is enabled
		Event rootEvent = requestContext.getRootEvent();
		String requestType = rootEvent.getType();
		
		// Adding the request type and keeping a count of requests in
		// the requestCounts map for RequestTimingStatistics
		// {#link:RequestTimingStatistics}
		AtomicLong count = requestCounts.putIfAbsent(requestType, new AtomicLong());
		if (count == null) {
			count = requestCounts.get(requestType);
		}
		count.getAndIncrement();
		
		if (keepStatistics) {
			Object contextInfoObj = rootEvent.getContextInfo();
			ContextInfoArray cia = ((contextInfoObj != null) && (contextInfoObj instanceof ContextInfoArray)) ? ((ContextInfoArray)contextInfoObj) : null;

			// There is an opportunity here to cache the result and use it later when the
			// probationary request manager tries to figure out if this request threshold
			// has been reached.  It's not clear though how to save this value for later.
			// The code does not maintain any request state until after the probationary 
			// request manager has determined the threshold has been reached.  This seems
			// to be on purpose, to save the overhead of looking it up until the request
			// has been running for some time.  I suppose it depends on how long it takes
			// us to look it up, whether it's beneficial to cache it or not.
			Timing bestMatch = this.getTiming(requestType, (cia != null) ? cia.getContextInfoArray() : null);
			bestMatch.incrementCount((cia != null) ? cia.toString() : null);
		}
	}
	
	public void writeIntrospectionData(PrintWriter pw) {
		if (keepStatistics) {
			Map<String, List<Timing>> timingConfig = getRequestTiming();
			if (timingConfig != null) {
				for (String type : timingConfig.keySet()) {
					pw.println("Type " + type + ":");
					List<Timing> typeConfig = timingConfig.get(type);
					for (Timing t : typeConfig) {
						t.writeIntrospectionData(pw);
					}
				}
				pw.println("End of requestTiming-1.0 output");
			} else {
				pw.println("requestTiming-1.0 cannot print statistics because the timing config was null.");
			}
		} else {
			pw.println("requestTiming-1.0 is not keeping timing statistics since there are no <timing/> elements in the configuration");
		}

		/* TODO: Write the interrupt stuff here rather than in its own dump introspector? */
	}

	public boolean isRequestInterruptEnabled() {
		return interruptHungRequest;
	}
	
	@Override
	public boolean getInterruptRequest(String type, String[] contextInfo) {
		return getTiming(type, contextInfo).interruptHungRequest();
	}
	
	private int countTimingConfigs() {
		int configCount = 0;
		
		Map<String, List<Timing>> config = getRequestTiming();
		if (config != null) {
			for (List<Timing> typeConfig : config.values()) {
				configCount += typeConfig.size();
			}
		}
		
		return configCount;
	}
	
	@Override
	public String toString() {
		StringBuffer hungReqTimingCfg = new StringBuffer();
		hungReqTimingCfg.append(String.format("%n"));
		hungReqTimingCfg.append("-------------------Hung Request Timing Settings-------------------" + String.format("%n"));
		hungReqTimingCfg.append("Sample rate: " + getSampleRate() + String.format("%n"));
		hungReqTimingCfg.append("Context info requirement: " + getContextInfoRequirement() + String.format("%n"));
		hungReqTimingCfg.append("-------------------Type Settings-------------------" + String.format("%n"));
		for(List<Timing> typeList : getRequestTiming().values()) {
			for (Timing t : typeList) {
				hungReqTimingCfg.append(t.getType() + ": " + t.getContextInfoString() + ": " + "Request threshold (ms) - " +  t.getRequestThreshold() + String.format("%n"));
			}
		}
		hungReqTimingCfg.append("-------------------------------------------------------------");
		return hungReqTimingCfg.toString();
	}
}
