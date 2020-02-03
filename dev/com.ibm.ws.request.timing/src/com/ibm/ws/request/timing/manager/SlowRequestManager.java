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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.request.timing.RequestTimingService;
import com.ibm.ws.request.timing.queue.DelayedRequestQueue;
import com.ibm.ws.request.timing.queue.SlowRequest;
import com.ibm.wsspi.requestContext.RequestContext;

public class SlowRequestManager {
	
	private static final TraceComponent tc = Tr.register (SlowRequestManager.class, "requestTiming", "com.ibm.ws.request.timing.internal.resources.LoggingMessages");
	
	private static final TraceNLS nls = TraceNLS.getTraceNLS(SlowRequestManager.class, "com.ibm.ws.request.timing.internal.resources.LoggingMessages");

	/** Map of all slow requests **/
	private final ConcurrentHashMap<String, RequestContext> slowRequests;

	/** Queue for holding slow requests **/
	private final DelayedRequestQueue<SlowRequest> requestQueue;

	/** 
	 * This object will be used for synchronizing the following activities
	 * 1) Starting slow request manager
	 * 2) Stopping slow request manager
	 */
	private final Object syncHandlerObject = new Object() {};

	/** 
	 * Holds a reference to task that will handle slow requests. 
	 * This will be used for cancelling the task when we are shutting down.
	 */
	private volatile Future<?> future = null;

	public SlowRequestManager(DelayedRequestQueue<SlowRequest> queue){
		requestQueue = queue;
		slowRequests = new ConcurrentHashMap<String, RequestContext>();
	}

	/** Starts the slow request handler task **/
	public void startHandler(){
		//Start slow request handler task if not already started
		//Only a single instance of this task should be running at any point of time
		if(future == null){
			boolean startedHandler = false;
			synchronized (syncHandlerObject) {
				if(future == null){
					future = RequestTimingService.getExecutorService().submit(slowRequestHandler);
					startedHandler = true;
				}
			}
			if(startedHandler && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, "Starting slow request handler");
			}
		}
	}

	/** Stops the hung request handler task. **/
	public void stopHandler(){
		boolean stoppedHandler = false;
		synchronized (syncHandlerObject) {
			if(future != null){
				future.cancel(true);
				future = null;
				stoppedHandler = true;
			}
		}
		if(stoppedHandler && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "Stopping slow request handler.");
		}
	}

	/**
	 * A executor will invoke this runnable to handle slow requests
	 * A request is slow if the request has been active for more than the threshold value.
	 * Slow requests are handled using tasks created by this runnable 
	 */
	private final Runnable slowRequestHandler = new Runnable(){
		@Trivial
		@Override
		public void run() {
			while(true) {
				try {
					//Get the slow request.
					//This is a blocking call, will block till the next slow request is available
					final SlowRequest slowRequest = requestQueue.processNext();

					if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
						Tr.debug(tc, "Removing request from the queue...", slowRequest.toString());
					}

					//Create a new instance of the slow request task
					Runnable slowRequestTask = new Runnable(){
						@Trivial
						@Override
						public void run() {
							try{
								SlowRequest request = slowRequest;
								boolean includeContextInfo = request.includeContextInfo();
								RequestContext requestContext = request.getRequestContext();
								long slowReqThreshold = request.getSlowRequestThreshold();

								
								if(requestContext.getRequestState() != RequestContext.STATE_FINISHED){
									if(tc.isWarningEnabled() && request.incIterationCount() <= request.getSlowRequestIterationsReq()) {
										double activeTime = (System.nanoTime() - requestContext.getRootEvent().getStartTime())/1000000.0; //Calculate the active time for the request

										String threadId = DataFormatHelper.padHexString((int) requestContext.getThreadId(), 8);
										String requestDuration = String.format("%.3f", activeTime);
										String stackTrace = requestContext.getStackTrace().toString();
										String dumpTree = RequestContext.dumpTree(requestContext.getRootEvent(), includeContextInfo);
										if(requestContext.getRequestState() == RequestContext.STATE_TOO_LONG){
											//Add truncated message to dump tree
											dumpTree = dumpTree + nls.getString("TRUNCATED_REQUEST_MESSAGE", null);
										}
										//Re-check the state of the request, if it has completed don't log it.
										if(requestContext.getRootEvent().getEndTime() == 0){
											Tr.warning(tc, "REQUEST_TIMER_WARNING",	requestContext.getRequestId().getId(), threadId, requestDuration, stackTrace,	dumpTree);
											requestContext.setSlow(true);
										}
									}
									// This enables to count new slow threads and not duplicates
									// RequestTimingStatistics {#link:RequestTimingStatistics
									String requestId = requestContext.getRequestId().getId();
									slowRequests.putIfAbsent(requestId, requestContext);
									request.resetDelay(slowReqThreshold);
									if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
										Tr.debug(tc, "Re-queuing request ...", request.toString());
									}
									requestQueue.requeueRequest(request);
								}else{
									//Request has completed, discard this request
									requestQueue.removeRequest(request);
									// Remove this request from the slow request list
									slowRequests.remove(requestContext.getRequestId().getId());
									if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
										Tr.debug(tc, "Discarding request..", request.toString());
									}
								}
							}catch(Exception e){
								FFDCFilter.processException(e, this.getClass().getName(), "131", this);
							}
						}
					};

					//Submit the task
					RequestTimingService.getExecutorService().submit(slowRequestTask);
				} catch (InterruptedException exit) {
					break; //Break out of the loop; ending thread
				} catch (Exception e){
					stopHandler();
					FFDCFilter.processException(e, this.getClass().getName(), "142", this);
				}
			}
		}
	};
	
	/**
	 * slowRequests is a map of slow requests. In order to get the count, need to
	 * check if the request is still running and then increment count by type
	 * 
	 * @param type:
	 *            the request to be filtered by
	 * @return the number of requests that are currently slow for the type
	 */
	public long countSlowRequests(String type) {
		long count = 0;
		for (RequestContext requestContext : slowRequests.values()) {
			if (requestContext.getRequestState() == RequestContext.STATE_RUNNING) {
				if (requestContext.getRootEvent().getType().equals(type)) {
					count++;
				}
			}
		}
		return count;
	}
}
