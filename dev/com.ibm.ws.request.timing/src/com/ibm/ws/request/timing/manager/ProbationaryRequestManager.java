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

package com.ibm.ws.request.timing.manager;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.request.probe.RequestProbeService;
import com.ibm.ws.request.timing.RequestTimingService;
import com.ibm.ws.request.timing.internal.config.RequestTimingConfig;
import com.ibm.ws.request.timing.queue.DelayedRequestQueue;
import com.ibm.ws.request.timing.queue.QueueableRequest;
import com.ibm.wsspi.probeExtension.ContextInfoRequirement;
import com.ibm.wsspi.requestContext.ContextInfoArray;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;


public class ProbationaryRequestManager<C extends RequestTimingConfig, R extends QueueableRequest> {
	
	private static final TraceComponent tc = Tr.register (ProbationaryRequestManager.class, "requestTiming", "com.ibm.ws.request.timing.internal.resources.LoggingMessages");
	
	/** Reference to configuration **/
	private volatile C config;
	
	/** Queue for holding requests **/
	private final DelayedRequestQueue<R> requestQueue;
		
	/** Last request time, used for stopping the timer **/
	private volatile long lastRequestTime;
	
	/** 
	 * This object will be used for synchronizing the following activities
	 * 1) Starting the timer of probationary request manager
	 * 2) Stopping the timer of probationary request manager
	 * 3) Re-setting the timer on configuration update
	 */
	private final Object syncTimerObject = new Object() {};
	
	/** 
	 * Holds a reference to the timer that will check for probationary requests. 
	 * This will be used for cancelling the timer when we reach a idle state or
	 * when we are shutting things down.  
	 */
	private volatile ScheduledFuture<?> timeKeeper = null;
	
	/** 
	 * Required for instantiating instances of generic type R.
	 */
	private final Class<R> clazz;
	
	private volatile java.lang.reflect.Constructor<R> rConstructor;
		
	public ProbationaryRequestManager(C config, DelayedRequestQueue<R> queue, Class<R> clazz){
		requestQueue = queue;
		this.config = config;
		this.clazz = clazz;
		try{
			this.rConstructor = this.clazz.getConstructor(RequestContext.class, long.class, long.class, boolean.class, boolean.class);
		}catch(Exception e){
			throw new RuntimeException("Probationary request manager instantiation failed.");
		}
	}
	
	/** Starts probationary request detector task **/
	public void startTimer(){
		//Start probationary request detection task if not already started
		//Only a single instance of this task should be running at any point of time
		if(timeKeeper == null){
			long reqThresholdMin = config.getRequestThresholdMin();
			//Check if request detection is enabled
			if( reqThresholdMin > 0 ){
				boolean startedTimer = false;
				long delay = reqThresholdMin/2;
				synchronized (syncTimerObject) {
					if(timeKeeper == null){
						timeKeeper = RequestTimingService.getScheduledExecutorService().scheduleAtFixedRate(probationaryReqDectector, delay, delay, TimeUnit.MILLISECONDS);
						startedTimer = true;
					}
				}
				if(startedTimer && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "Starting probationary request (" + clazz.getSimpleName() + ") detector with initial delay (ms) : " + delay + " and period (ms) : " + delay);
				}
			}
		}
	}
	
	/** Stops probationary request detector task **/
	public void stopTimer(){
		boolean stoppedTimer = false;
		synchronized (syncTimerObject) {
			if(timeKeeper != null){
				timeKeeper.cancel(false);
				timeKeeper = null;
				stoppedTimer = true;
			}
		}
		if(stoppedTimer && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "Stopping probationary request (" + clazz.getSimpleName() + ") detector.");
		}
	}
	
	/** Resets probationary request detector task, used on configuration update **/
	public void resetTimer(C config){
		if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "Re-setting probationary request (" + clazz.getSimpleName() + ") detector.");
		}
		synchronized(syncTimerObject){
			//Stop probationary request detector, update the configuration & start it.
			stopTimer();
			this.config = config;
			startTimer();
		}
	}
	
	/** Used during start up to set the default configuration **/
	public void setConfig(C config){
		this.config = config;
	}
	
	public void setLastRequestTime(long lastRequestTime) {
		this.lastRequestTime = lastRequestTime;
	}

	/**
	 * A scheduled executor which will invoke this runnable to queue probationary requests
	 * A request is a probationary request if the request has been active for more than half the threshold value.
	 */
	private final Runnable probationaryReqDectector = new Runnable(){
		@Trivial
		@Override
		public void run() {
			try{
				/**
				 * Other conditions that should be met to queue a probationary request.
				 * 1)The request is already not queued.
				 * 2)Type specific setting is available and the setting has request detection enabled or
				 * 3)Type specific setting is not available and globally request detection is enabled.
				 */
				for(RequestContext activeReqContext: RequestProbeService.getActiveRequests()){
					long requestThreshold = 0, requestThresholdMean = 0;
					boolean interruptRequest = false;
					Event rootEvent = activeReqContext.getRootEvent();
					String requestType = rootEvent.getType();
					Object contextInfoObj = rootEvent.getContextInfo();
					String[] contextInfo = ((contextInfoObj != null) && (contextInfoObj instanceof ContextInfoArray)) ? ((ContextInfoArray)contextInfoObj).getContextInfoArray() : null;
					requestThreshold = config.getRequestThreshold(requestType, contextInfo);
					interruptRequest = config.getInterruptRequest(requestType, contextInfo);
					// TODO: Need to lookup twice here ^--- by context info, any way to optimize?
					requestThresholdMean = requestThreshold/2;
					if(requestThresholdMean > 0){
						long activeTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - activeReqContext.getRootEvent().getStartTime(), TimeUnit.NANOSECONDS);
						if(activeTime >= requestThresholdMean){
							boolean includeContextInfo = (config.getContextInfoRequirement() == ContextInfoRequirement.ALL_EVENTS) ? true : false;
							R request = rConstructor.newInstance(activeReqContext, (requestThreshold - activeTime), requestThreshold, includeContextInfo, interruptRequest);
							if(requestQueue.addRequest(request)){
								if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
									Tr.debug(tc, "(" + clazz.getSimpleName() + ") Active time (ms) : " + activeTime 
											+ " and request threshold mean (ms) : " + requestThresholdMean
											+ String.format("%n") + "Adding new request to queue ...", request.toString());
								}
							}
						}
					}
				}
				long timeElapsedSinceLastReq = TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastRequestTime, TimeUnit.NANOSECONDS);
				long stopDuration = config.getRequestThresholdMax()/2;
				if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "(" + clazz.getSimpleName() + ") Time elapsed since last request (ms) : " + timeElapsedSinceLastReq 
							+ " and largest threshold window (ms) : " + stopDuration);
				}
				if(timeElapsedSinceLastReq > stopDuration){
					//No new request was seen in the biggest threshold window or
					//User has disabled request detection so stop probationary request detector.
					stopTimer();
				}
			}catch(Exception e){
				//Clean up.
				stopTimer();
				FFDCFilter.processException(e, this.getClass().getName(), "188", this);
			}
		}
	};
}
