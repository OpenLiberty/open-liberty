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
package com.ibm.wsspi.request.probe.bci;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.request.probe.RequestProbeService;
import com.ibm.ws.request.probe.bci.internal.RequestProbeBCIManagerImpl;
import com.ibm.wsspi.probeExtension.ContextInfoRequirement;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;

public class TransformDescriptorHelper {

	private static ThreadLocal<RequestContext> rcThreadLocalObj = new ThreadLocal<RequestContext>();
	private Event currentEvent;
	private boolean isCounter;

	// Used for debugging purpose only
	private String className;
	private String methodName;
	private String methodDesc;

	private static final TraceComponent tc = Tr
			.register(TransformDescriptorHelper.class);

	/**
	 * This method will check if context information is required or not by
	 * processing through each active ProbeExtensions available in PE List
	 * 
	 * @param eventType
	 * @return
	 */
	public boolean contextInfoRequired(String eventType, long requestNumber) {

		boolean needContextInfo = false;

		List<ProbeExtension> probeExtnList = RequestProbeService
				.getProbeExtensions();

		for (int i = 0; i < probeExtnList.size(); i++) {
			ProbeExtension probeExtension = probeExtnList.get(i);
			if (requestNumber % probeExtension.getRequestSampleRate() == 0) {
				if (probeExtension.getContextInfoRequirement() == ContextInfoRequirement.ALL_EVENTS) {
					needContextInfo = true;
					break;

				} else if (probeExtension.getContextInfoRequirement() == ContextInfoRequirement.EVENTS_MATCHING_SPECIFIED_EVENT_TYPES
						&& (probeExtension.invokeForEventTypes() == null || probeExtension
								.invokeForEventTypes().contains(eventType))) {
					needContextInfo = true;
					break;
				}
			}
		}
		return needContextInfo;
	}

	public void entryHelper(String className, String methodName,
			String methodDesc, String type, String td, Object thisInstance,
			Object objArrays) {

		RequestProbeTransformDescriptor requestProbeTransformDescriptor = getObjForInstrumentation(td);

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
			Tr.debug(
					tc,
					"entryHelper",
					"[className=" + this.className + ", methodName="
							+ this.methodName + ", methodDesc="
							+ this.methodDesc + "]",
					"[type= " + type + "]",
					(requestProbeTransformDescriptor != null) ? requestProbeTransformDescriptor
							+ " [isCounter="
							+ requestProbeTransformDescriptor.isCounter() + "]"
							: null);
		}

		// This check is required as this can be null during feature removal
		// process and can cause exceptions for in flight requests.
		if (requestProbeTransformDescriptor != null) {

			isCounter = requestProbeTransformDescriptor.isCounter();

			currentEvent = new Event(type); // Create new Event with type &
											// relevant context info
			Object contextInfo = null;

			// If this event is not a counter proceed as usual.
			if (!isCounter) {
				RequestContext rcVal = rcThreadLocalObj.get();
				// Set the start time for the Event
				currentEvent.setStartTime(System.nanoTime());
				// Check thread local if there is RC available? yes, return same
				if (rcVal == null) {
					// Else create one, push in ThreadLocal
					rcVal = new RequestContext();
					// Change the state of the request to running.
					rcVal.setRequestState(RequestContext.STATE_RUNNING);
					rcVal.setRootEvent(currentEvent);
					rcVal.setCurrentEvent(currentEvent);
					rcThreadLocalObj.set(rcVal);
					// Increment the event count.
					rcVal.incrementEventCount();
				} else {
					// Stop adding events to request context once we reach the
					// max value.
					if (rcVal.getEventCount() < RequestContext.MAX_EVENTS) {
						Event tempCurrentEvent = rcVal.getCurrentEvent();
						if (tempCurrentEvent != null) {
							tempCurrentEvent.addChild(currentEvent);
							currentEvent.setParentEvent(tempCurrentEvent);
							rcVal.setCurrentEvent(currentEvent);
							// Increment the event count.
							rcVal.incrementEventCount();
						}
					} else {
						// Event count has exceeded the allowed limit
						if (rcVal.getRequestState() == RequestContext.STATE_RUNNING) {
							rcVal.setRequestState(RequestContext.STATE_TOO_LONG);
						}
					}
				}

				if (contextInfoRequired(type, rcVal.getRequestId()
						.getSequenceNumber())) {
					contextInfo = requestProbeTransformDescriptor
							.getContextInfo(thisInstance, objArrays);
				}

				if (contextInfo != null) {
					currentEvent.setContextInfo(contextInfo);
				}
				// Process all the active PE available in the list
				RequestProbeService.processAllEntryProbeExtensions(
						currentEvent, rcVal);
			} else {
				// Handle counter event
				contextInfo = requestProbeTransformDescriptor.getContextInfo(
						thisInstance, objArrays);
				if (contextInfo != null) {
					currentEvent.setContextInfo(contextInfo);
				}
			}
		}
	}

	public void exitHelper() {

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "exitHelper", "[className=" + this.className
					+ ", methodName=" + this.methodName + ", methodDesc="
					+ this.methodDesc + "]");
		}

		// This check is required as this can be null during feature removal
		// process and can cause exceptions for in flight requests.
		if (currentEvent != null) {
			if (!isCounter) {
				RequestContext rcVal = rcThreadLocalObj.get();
				// Process all the active PE available in the list
				if (rcVal != null) {
					currentEvent.setEndTime(System.nanoTime()); // Set endTime
																// for event
					RequestProbeService.processAllExitProbeExtensions(
							currentEvent, rcVal);

					if (currentEvent.getParentEvent() != null) {
						rcVal.setCurrentEvent(currentEvent.getParentEvent());
					}

					// if end of request remove the thread local object..
					if (currentEvent == rcVal.getRootEvent()) {
						rcThreadLocalObj.remove();
						// Set state of the request to FINISHED
						rcVal.setRequestState(RequestContext.STATE_FINISHED);
					}
				}
			} else {
				RequestProbeService
						.processAllCounterProbeExtensions(currentEvent);
			}
		}
	}

	/**
	 * getObjForInstrumentation Returns TransformDescriptor with input
	 * parameters className, methodName and methodDescription
	 * 
	 * @param classname
	 * @param mInfo
	 * @return
	 */
	public static RequestProbeTransformDescriptor getObjForInstrumentation(
			String key) {
		RequestProbeTransformDescriptor requestProbeTransformDescriptor = RequestProbeBCIManagerImpl
				.getRequestProbeTransformDescriptors().get(key);
		return requestProbeTransformDescriptor;
	}

}
