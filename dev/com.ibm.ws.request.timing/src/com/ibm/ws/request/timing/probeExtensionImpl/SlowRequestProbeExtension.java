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

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.request.timing.internal.config.SlowRequestTimingConfig;
import com.ibm.ws.request.timing.manager.ProbationaryRequestManager;
import com.ibm.ws.request.timing.manager.SlowRequestManager;
import com.ibm.ws.request.timing.queue.DelayedRequestQueue;
import com.ibm.ws.request.timing.queue.SlowRequest;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;

/**
 *
 */
public class SlowRequestProbeExtension implements ProbeExtension {

	private static final TraceComponent tc = Tr.register(SlowRequestProbeExtension.class);
	
	
	/** Reference to slow request detection configuration **/
	private volatile SlowRequestTimingConfig config = new SlowRequestTimingConfig();
	
	/** Delay queue for holding requests **/
	private final DelayedRequestQueue<SlowRequest> requestQueue = new DelayedRequestQueue<SlowRequest>();

	/** Slow request probe extension uses this to detect half hung requests **/
	private final ProbationaryRequestManager<SlowRequestTimingConfig, SlowRequest> probSlowReqMgr = 
			new ProbationaryRequestManager<SlowRequestTimingConfig, SlowRequest>(config, requestQueue, SlowRequest.class);

	/** Slow request probe extension uses this to handle slow requests **/
	private final SlowRequestManager slowReqMgr = new SlowRequestManager(requestQueue);
	
	/** 
	 * Used for indicating that shutdown has been initiated for this probe extension
	 * and no new requests should be processed 
	 */
	private volatile boolean hasStopped = false;
	
	@Override
	public void processEntryEvent(Event event, RequestContext requestContext) {
		if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) {
			Tr.debug(tc, "processEntryEvent " + event);
		}
		if(!hasStopped){
			probSlowReqMgr.setLastRequestTime(System.nanoTime());
			//This will only start the timer if it has not already started.
			//If it has started will exit immediately.
			probSlowReqMgr.startTimer();
			//This will only start the handler if it has not already started.
			//If it has started will exit immediately.
			slowReqMgr.startHandler();
		}
	}

	@Override
	public void processExitEvent(Event event, RequestContext requestContext) {
		if (TraceComponent.isAnyTracingEnabled() &&  tc.isDebugEnabled()) {
			Tr.debug(tc, "processExitEvent " + event);
		}
		if( !hasStopped && requestContext.isSlow() ){
			
			//if requestContext is a slowRequestContext
			double activeTime = (System.nanoTime() - event.getStartTime())/1000000.0; //Calculate the active time for the request
			
			String threadId = DataFormatHelper.padHexString((int) requestContext.getThreadId(), 8);
			String requestDuration = String.format("%.3f", activeTime);
			
			Tr.info(tc, "REQUEST_TIMER_FINISH_SLOW", requestContext.getRequestId().getId(), threadId, requestDuration);
			
			//Do nothing as no action is required on exit event.
			//Clean up activity for this request will be performed in the corresponding slow request timer task.

			/** Instead of this should we just return false from invokeForEventExit? */
		} 
	}
	
	/** 
	 * Used during start up to set the default configuration
	 * Request timing service will call this method on activate
	 **/
	public void setConfig(SlowRequestTimingConfig config){
		this.config = config;
		probSlowReqMgr.setConfig(config);
	}
	
	/** On configuration update request timing service will call this method **/
	public void updateConfig(SlowRequestTimingConfig config){
		this.config = config;
		probSlowReqMgr.resetTimer(this.config);
	}

	/** On feature removal request timing service will call this method **/
	public void stop(){
		if ( !hasStopped ) {
			hasStopped = true;
			//Stop all managers and clear the queue.
			probSlowReqMgr.stopTimer();
			slowReqMgr.stopHandler();
			requestQueue.clear();
		}
	}

	@Override
	public void processCounter(Event event) {
		//Do nothing as invokeForCounter is false.
	}
	
	@Override
	public boolean invokeForRootEventsOnly() {
		return true; // ROOT_EVENTS = invoke this ProbeExtension for root events that have accepted event type
	}

	@Override
	public List<String> invokeForEventTypes() {
		return null;
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
	public boolean invokeForCounter() {
		return false;
	}

	@Override
	public int getRequestSampleRate() {
		return config.getSampleRate();
	}

	@Override
	public int getContextInfoRequirement() {
		return config.getContextInfoRequirement();
	}

	/**
	 * @param type:
	 *            the request to be filtered by
	 * @return number of requests that are currently slow for the type
	 */
	public long getSlowRequestCount(String type) {
		return slowReqMgr.countSlowRequests(type);
	}

}
