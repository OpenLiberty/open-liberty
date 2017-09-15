/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.requestContext;

import java.util.Map;
import java.util.Map.Entry;

import com.ibm.ws.request.probe.RequestId;
import com.ibm.ws.request.probe.RequestIdGeneratorPUID;
import com.ibm.ws.request.probe.RequestProbeService;

/**
 * class RequestContext : This class is responsible for encapsulating the
 * details of the request i.e : thread id, request id, root event in the respect
 * of the current request context and the current event in the context of the
 * current request context. It allow the operations to know about the currently
 * running request it its detail and its respective events. Based on a
 * particular use case like slow request the whole event is dumped in tree
 * structure along with the request context enabling the diagnostics to capture
 * all the relevant information.
 */
public class RequestContext {

	private int requestContextIndex = -1;
	private final long threadId;
	private final RequestId requestId;
	private volatile Event rootEvent;
	private Event currentEvent;
	private volatile int state = -1;
	private int eventCount = -1;
	private static final RequestIdGeneratorPUID idgen = new RequestIdGeneratorPUID();
	
	/**
	 * Request states.
	 */
	public static final int STATE_RUNNING = 10;
	public static final int STATE_TOO_LONG = 11;
    public static final int STATE_FINISHED = 100;
    
    /**
     * Maximum number of events a request context can store.
     * When the event count exceeds this number, the
     * request context gets truncated.
     */
    public static final int MAX_EVENTS = 500;

	public RequestContext() {

		requestId = idgen.getNextRequestId();
		RequestProbeService.requestIDExtension.setValue(requestId.getId());
		threadId = Thread.currentThread().getId();
		eventCount = 0;
	}

	/**
	 * @return : request id of the current request context.
	 */
	public RequestId getRequestId() {
		return requestId;
	}

	/**
	 * @return : the root event in the respect of the current request context.
	 */
	public Event getRootEvent() {
		return rootEvent;
	}

	/**
	 * @param event
	 *            : Allow you to set the current Root Event
	 */
	public void setRootEvent(Event event) {
		this.rootEvent = event;
	}

	/**
	 * @return : get the current event for the current request context
	 */
	public Event getCurrentEvent() {
		return currentEvent;
	}

	/**
	 * @param currentEvent
	 */
	public void setCurrentEvent(Event currentEvent) {
		this.currentEvent = currentEvent;
	}

	/**
	 * @return : The current request context
	 */
	public int getRequestContextIndex() {
		return requestContextIndex;
	}

	public void setRequestContextIndex(int requestContextIndex) {
		this.requestContextIndex = requestContextIndex;
	}
	
	/**
	 * @return : Indicates state of the request this request context
	 * is associated with. The request can be in the following states
	 * 1) RUNNING
	 * 2) FINIHSED
	 */	
	public int getRequestState() {
		return state;
	}

	/**
	 * @return : The state of this request.
	 */
	public void setRequestState(int state) {
		this.state = state;
	}
		
	/**
	 * Increments by one the current event count.
	 */
	public void incrementEventCount() {
		eventCount++;
	}
	
	/**
	 * @return Current event count for the request.
	 */
	public int getEventCount(){
		return eventCount;
	}

	/**
	 * This method is used to dump the event in Tree structure. Currently used
	 * for Request Timing where it will keep a log of timing of request
	 * execution, in case it exceeds it will call this method and dump the tree
	 * of Request Dumping would be for log file or into Fight Data Recorder
	 * 
	 * @param event
	 */

	public static String dumpTree(Event event, boolean includeContextInfo) {
		return new EventStackFormatter().getStackFormat(event, includeContextInfo).toString();
	}

	public long getThreadId() {
		return threadId;
	}

	/**
	 * This method is used to get the stack trace for requested Thread ID. Using
	 * the Thread class getAllStackTraces() method we get the stack traces for
	 * all the active threads, then based on the requirement get the Stack
	 * thread for specific thread ID and return it.
	 * 
	 * @return
	 */
	public StringBuffer getStackTrace() {

		Map<Thread, StackTraceElement[]> activeThreadStackTraces = Thread
				.getAllStackTraces();
		StringBuffer requestTheadStackStr = new StringBuffer();

		for (Entry<Thread, StackTraceElement[]> entry : activeThreadStackTraces
				.entrySet()) {

			if (entry.getKey().getId() == threadId) { // If thread ID matches
														// the requestThreadId,
														// return the stackTrace

				for (StackTraceElement stackTraceElement : entry.getValue()) {
					requestTheadStackStr.append("\t at " + stackTraceElement
							+ "\n");
				}
				break;
			}

		}
		return requestTheadStackStr;
	}

	@Override
	public String toString() {
		return "RequestContext [threadId=" + threadId + ", requestId="
				+ requestId + ", state=" + state + "]";
	}
}
