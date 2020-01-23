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
import com.ibm.ws.request.interrupt.InterruptibleRequestLifecycle;
import com.ibm.ws.request.timing.RequestTimingConstants;
import com.ibm.ws.request.timing.RequestTimingService;
import com.ibm.ws.request.timing.queue.DelayedRequestQueue;
import com.ibm.ws.request.timing.queue.HungRequest;
import com.ibm.wsspi.requestContext.RequestContext;

public class HungRequestManager {

	private static final TraceComponent tc = Tr.register (HungRequestManager.class, "requestTiming", "com.ibm.ws.request.timing.internal.resources.LoggingMessages");
	
	private static final TraceNLS nls = TraceNLS.getTraceNLS(HungRequestManager.class, "com.ibm.ws.request.timing.internal.resources.LoggingMessages");

	/** Map of all hung requests **/
	private final ConcurrentHashMap<String, RequestContext> hungRequests;

	/** Queue for holding hung requests **/
	private final DelayedRequestQueue<HungRequest> requestQueue;

	/** Instance of thread dump scheduler for generating javacore **/
	private final ThreadDumpManager threadDumpScheduler;

	/** Interruptible thread infrastructure lifecycle reference **/
	private InterruptibleRequestLifecycle interruptibleRequestLifecycle = null;
	
	/** 
	 * This object will be used for synchronizing the following activities
	 * 1) Starting hung request manager
	 * 2) Stopping hung request manager
	 */
	private final Object syncHandlerObject = new Object() {};

	/** 
	 * Holds a reference to task that will handle hung requests. 
	 * This will be used for cancelling the task when we are shutting down.
	 */
	private volatile Future<?> future = null;

	public HungRequestManager(DelayedRequestQueue<HungRequest> queue){
		requestQueue = queue;
		hungRequests = new ConcurrentHashMap<String, RequestContext>();
		threadDumpScheduler= new ThreadDumpManager(RequestTimingConstants.THREAD_DUMPS_REQUIRED, 
						RequestTimingConstants.THREAD_DUMP_DURATION);
	}

	/** Starts the hung request handler task **/
	public void startHandler(){
		//Start hung request handler task if not already started
		//Only a single instance of this task should be running at any point of time
		if(future == null){
			boolean startedHandler = false;
			synchronized (syncHandlerObject) {
				if(future == null){
					future = RequestTimingService.getExecutorService().submit(hungRequestHandler);
					startedHandler = true;
				}
			}
			if(startedHandler && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, "Starting hung request handler");
			}
		}
	}

	/**
	 *  Stops the hung request handler task.
	 *  This will also stop thread dump scheduler if it is running. 
	 **/
	public void stopHandler(){
		boolean stoppedHandler = false;
		synchronized (syncHandlerObject) {
			if(future != null){
				future.cancel(true);
				future = null;
				stoppedHandler = true;
				threadDumpScheduler.stopTimer();
			}
		}
		if(stoppedHandler && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "Stopping hung request handler.");
		}
	}
	
	/** Utility method for printing warning message when a hung request completes **/
	public void logMessageIfHung(RequestContext requestContext){
		if(hungRequests.containsKey(requestContext.getRequestId().getId())) { 
			double totalHungDuration =  (System.nanoTime() - requestContext.getRootEvent().getStartTime())/1000000.0;
			if(tc.isWarningEnabled()) {
				Tr.warning(tc, "HUNG_REQUEST_COMPLETED_INFO", requestContext.getRequestId().getId(),DataFormatHelper.padHexString((int) requestContext.getThreadId(), 8), String.format("%.3f", totalHungDuration));
			}
		}
	}

	/**
	 * A executor will invoke this runnable to handle hung requests
	 * A request is hung if the request has been active for more than the threshold value.
	 * Hung requests are handled using tasks created by this runnable 
	 */
	private final Runnable hungRequestHandler = new Runnable(){
		@Trivial
		@Override
		public void run() {
			while(true) {
				try {
					//Get the hung request.
					//This is a blocking call, will block till the next hung request is available
					final HungRequest hungRequest = requestQueue.processNext();

					if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
						Tr.debug(tc, "Removing request from the queue...", hungRequest.toString());
					}

					//Create a new instance of the hung request task
					Runnable hungRequestTask = new Runnable(){
						@Trivial
						@Override
						public void run() {
							try{
								HungRequest request = hungRequest;
								boolean includeContextInfo = request.includeContextInfo();
								RequestContext requestContext = request.getRequestContext();
								long hungReqThreshold = request.getHungRequestThreshold();

								if(requestContext.getRequestState() != RequestContext.STATE_FINISHED){
									if(tc.isWarningEnabled()) {
										double activeTime = (System.nanoTime() - requestContext.getRootEvent().getStartTime())/1000000.0; //Calculate the active time for the request
										// Log summary of the hung thread.
										Tr.warning(tc,"HUNG_REQUEST_WARNING", requestContext.getRequestId().getId(), 
												DataFormatHelper.padHexString((int) requestContext.getThreadId(), 8),
												String.format("%.3f", activeTime), 
												(requestContext.getRequestState() == RequestContext.STATE_TOO_LONG ?
														RequestContext.dumpTree(requestContext.getRootEvent(), includeContextInfo) + nls.getString("TRUNCATED_REQUEST_MESSAGE", null)
														: RequestContext.dumpTree(requestContext.getRootEvent(), includeContextInfo)));
									}
									String theId = requestContext.getRequestId().getId();
									if(hungRequests.putIfAbsent(theId, requestContext) == null){
										// A new hung thread was found!
										// Start the task for creating javacore if its not already running.
										threadDumpScheduler.startTimer();
										// Notify the interrupt code, and then any other registered listeners.
										long threadId = requestContext.getThreadId();
										if ((interruptibleRequestLifecycle != null) && (request.interruptRequest())) {
											interruptibleRequestLifecycle.hungRequestDetected(theId, threadId);
										}
										RequestTimingService.processAllHungRequestNotifications(theId, threadId);
									}
									//Re-queue the request as it is still hung.
									request.resetDelay(hungReqThreshold);
									if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
										Tr.debug(tc, "Re-queuing request ...", request.toString());
									}
									requestQueue.requeueRequest(request);
								}else{
									//Means request has completed
									//Remove this request from the hung request list and discard this request
									hungRequests.remove(requestContext.getRequestId().getId());
									requestQueue.removeRequest(request);
									if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
										Tr.debug(tc, "Discarding request..", request.toString());
									}
								}
							}catch(Exception e){
								FFDCFilter.processException(e, this.getClass().getName(), "157", this);
							}
						}
					};

					//Submit the task
					RequestTimingService.getExecutorService().submit(hungRequestTask);
				} catch (InterruptedException exit) {
					break; //Break out of the loop; ending thread
				} catch (Exception e){
					stopHandler();
					FFDCFilter.processException(e, this.getClass().getName(), "167", this);
				}
			}
		}
	};
	
	/* This is not an OSGi method */
	public void setInterruptibleRequestLifecycle(InterruptibleRequestLifecycle lifecycle) {
		this.interruptibleRequestLifecycle = lifecycle;
	}

	/**
	 * hungRequests is a map of hung requests. In order to get the count, need to
	 * check if the request is still running and then increment count by type
	 * 
	 * @param type:
	 *            the request to be filtered by
	 * @return the number of requests that are currently hung for the type
	 */
	public long countHungRequests(String type) {
		long count = 0;
		for (RequestContext requestContext : hungRequests.values()) {
			if (requestContext.getRequestState() == RequestContext.STATE_RUNNING) {
				if (requestContext.getRootEvent().getType().equals(type)) {
					count++;
				}
			}
		}
		return count;
	}
}
