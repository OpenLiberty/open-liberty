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
package com.ibm.ws.request.timing.probeExtensionImpl;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.request.interrupt.InterruptibleRequestLifecycle;
import com.ibm.ws.request.timing.internal.config.HungRequestTimingConfig;
import com.ibm.ws.request.timing.manager.HungRequestManager;
import com.ibm.ws.request.timing.manager.ProbationaryRequestManager;
import com.ibm.ws.request.timing.queue.DelayedRequestQueue;
import com.ibm.ws.request.timing.queue.HungRequest;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;

public class HungRequestProbeExtension implements ProbeExtension, Introspector {

	private static final TraceComponent tc = Tr.register (HungRequestProbeExtension.class, "requestTiming", "com.ibm.ws.request.timing.internal.resources.LoggingMessages");

	/** Reference to hung request detection configuration **/
	private volatile HungRequestTimingConfig config = new HungRequestTimingConfig();
	
	/** Map for total request count with request type (key) and count (value) **/
	public final ConcurrentHashMap<String, AtomicLong> requestCounts = new ConcurrentHashMap<String, AtomicLong>();

	/** Delay queue for holding requests **/
	private final DelayedRequestQueue<HungRequest> requestQueue = new DelayedRequestQueue<HungRequest>();
		
	/** Hung request probe extension uses this to detect half hung requests **/
	private final ProbationaryRequestManager<HungRequestTimingConfig, HungRequest> probHungReqMgr = 
			new ProbationaryRequestManager<HungRequestTimingConfig, HungRequest>(config, requestQueue, HungRequest.class);
	
	/** Hung request probe extension uses this to handle hung requests **/
	private final HungRequestManager hungReqMgr = new HungRequestManager(requestQueue);
	
	/** Link between the interrupt code and the timing code */
	private InterruptibleRequestLifecycle interruptLifecycle = null;
	
	/** 
	 * Used for indicating that shutdown has been initiated for this probe extension
	 * and no new requests should be processed 
	 */
	private volatile boolean hasStopped = false;	
	
	@Override
	public void processEntryEvent(Event event, RequestContext requestContext) {
		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "processEntryEvent " + event);
		}
		if(!hasStopped){
			probHungReqMgr.setLastRequestTime(System.nanoTime());
			//This will only start the timer if it has not already started.
			//If it has started will exit immediately.
			probHungReqMgr.startTimer();
			//This will only start the handler if it has not already started.
			//If it has started will exit immediately.
			hungReqMgr.startHandler();
			//If we're keeping statistics, do that now.  The hung request probe
			//extension is responsible for both probe extensions.
			config.incrementTimingConfigForRequest(requestContext, requestCounts);
			// Tell the interrupt code about this request.
			if (config.isRequestInterruptEnabled()) {
				interruptLifecycle.newRequestEntry(requestContext.getRequestId().getId());
			}
		}
	}

	@Override
	public void processExitEvent(Event event, RequestContext requestContext) {
		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "processExitEvent " + event);
		}		
		if(!hasStopped){
			//If this request was marked as hung, log a warning that it completed.
			hungReqMgr.logMessageIfHung(requestContext);
			// Tell the interrupt code about this request.
			if (config.isRequestInterruptEnabled()) {
				interruptLifecycle.completedRequestExit(requestContext.getRequestId().getId());
			}
		}
	}
	
	/** 
	 * Used during start up to set the default configuration
	 * Request timing service will call this method on activate
	 **/
	public void setConfig(HungRequestTimingConfig config){
		this.config = config;
		probHungReqMgr.setConfig(config);
		hungReqMgr.setInterruptibleRequestLifecycle((config.isRequestInterruptEnabled()) ? interruptLifecycle : null);
	}
	
	/** On configuration update request timing service will call this method **/
	public void updateConfig(HungRequestTimingConfig config){
		this.config = config;
		probHungReqMgr.resetTimer(this.config);
		hungReqMgr.setInterruptibleRequestLifecycle((config.isRequestInterruptEnabled()) ? interruptLifecycle : null);
	}

	/** On feature removal request timing service will call this method **/
	public void stop(){
		if (!hasStopped) {
			hasStopped = true;
			//Stop all managers and clear the queue.
			probHungReqMgr.stopTimer();
			hungReqMgr.stopHandler();
			requestQueue.clear();
			requestCounts.clear();
		}
	}
	
	@Override
	public void processCounter(Event event) {
		//Do nothing as invokeForCounter is false.
	}
	
	@Override
	public int getRequestSampleRate() {
		return config.getSampleRate(); // sampleRate == 1: call for every request
	}

	@Override
	public boolean invokeForRootEventsOnly() {
		return true; // ROOT_EVENTS = invoke this ProbeExtension for root events that have accepted event type
	}

	@Override
	public boolean invokeForEventEntry() {
		return true;
	}

	@Override
	public boolean invokeForEventExit() {
		return true;
	}

	@Override
	public List<String> invokeForEventTypes() {
		return null;
	}

	@Override
	public boolean invokeForCounter() {
		return false;
	}

	@Override
	public int getContextInfoRequirement() {
		return config.getContextInfoRequirement();
	}

	@Override
	public String getIntrospectorName() {
		return "HungRequestProbeExtensionIntrospector";
	}

	@Override
	public String getIntrospectorDescription() {
		return "Provides information on how requestTiming-1.0 is assigning timing thresholds to requests";
	}

	@Override
	public void introspect(PrintWriter out) throws Exception {
		config.writeIntrospectionData(out);
	}
	
	/**
	 * OSGi setter
	 */
	protected void setInterruptibleRequestLifecycle(InterruptibleRequestLifecycle lifecycle) {
		 this.interruptLifecycle = lifecycle;
	}
	
	/**
	 * OSGi unsetter
	 */
	protected void unsetInterruptibleRequestLifecycle(InterruptibleRequestLifecycle lifecycle) {
		this.interruptLifecycle = null;
	}

	/**
	 * config (HungRequestTimingConfig) keeps a map of request type and the
	 * appropriate counts. Need to retrieve the map and get the count specified by
	 * the type param.
	 * 
	 * @param type:
	 *            the request to be filtered by
	 * @return The total number of requests for the type
	 */
	public long getTotalRequestCount(String type) {
		AtomicLong requestCount = requestCounts.get(type);
		if (requestCount != null) 
			return requestCount.get();
		return 0L;
	}

	/**
	 * @param type:
	 *            the request to be filtered by
	 * @return number of requests that are currently hung for the type
	 */
	public long getHungRequestCount(String type) {
		return hungReqMgr.countHungRequests(type);
	}

}
