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

package com.ibm.ws.request.probe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.logging.hpel.LogRecordContext;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.probeExtension.FastList;
import com.ibm.wsspi.probeExtension.ProbeExtension;
import com.ibm.wsspi.requestContext.Event;
import com.ibm.wsspi.requestContext.RequestContext;

/**
 * <p>
 * The class RequestProbeService is used to process all the registered entry and
 * exit probe extensions and based on the available probe extensions impl and
 * call their respective overridden methods.Since its provide a framework to use
 * the underlying BCI framework hence whoever component like JDBC, request
 * timing or timed operation uses it will load this feature. and by enabling the
 * component who have used it allow it to be activated and registered with the
 * OSGI framework. For example :
 * 
 * <pre>
 * {@code
 * <server>
 * 
 *     <featureManager>
 *          <feature>timedOperations-1.0</feature>
 *     </featureManager>
 * 
 * </server> }
 * </pre>
 * 
 * Allow the feature timedOperations to be enabled and allow the request probe
 * feature to be enabled/activated as the BCI timed operation uses the BCI
 * framework from the request probe.
 * </p>
 */
public class RequestProbeService {

	private static final TraceComponent tc = Tr.register(RequestProbeService.class);

	/** Registered Probe Extensions **/
	private static volatile List<ProbeExtension> probeExtensions = 
			Collections.unmodifiableList(new ArrayList<ProbeExtension>());

	/** Active running requests **/
	private static FastList<RequestContext> activeRequests = new FastList<RequestContext>();

	public final static ThreadLocalStringExtension requestIDExtension;
	private final static String REQUEST_ID_EXTENSION_NAME = "requestID";

	static {
		requestIDExtension = new ThreadLocalStringExtension();
	}

	protected void activate(Map<String, Object> configuration) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Activating " + this);
		}
		LogRecordContext.registerExtension(REQUEST_ID_EXTENSION_NAME, requestIDExtension);
	}

	protected void modified(Map<String, Object> configuration) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
			Tr.event(tc, " Modified");
	}

	public synchronized void setRequestProbe(ProbeExtension probeExtension) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Setting the probe extension into list", probeExtension.getClass());
		}
		List<ProbeExtension> extensions = new ArrayList<ProbeExtension>(probeExtensions);
		extensions.add(probeExtension);
		probeExtensions = Collections.unmodifiableList(extensions);
	}

	public synchronized void unsetRequestProbe(ProbeExtension probeExtension) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Removing probe extension from list", probeExtension.getClass());
		}
		List<ProbeExtension> extensions = new ArrayList<ProbeExtension>(probeExtensions);
		extensions.remove(probeExtension);
		probeExtensions = Collections.unmodifiableList(extensions);
	}

	@Deactivate
	protected void deactivate(int reason) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, " Deactivating " + this, " reason = " + reason);
		}
		LogRecordContext.unregisterExtension(REQUEST_ID_EXTENSION_NAME);
		requestIDExtension.remove(); // Removing the extension from threadlocal
		probeExtensions = Collections.unmodifiableList(new ArrayList<ProbeExtension>());
		activeRequests.clear();
	}

	/**
	 * Iterate through all the probe extensions and process all the entry method
	 * of the available probe extension
	 * 
	 * @param event
	 *            : Event for which the probe extensions to be processed.
	 * @param requestContext
	 *            : Request context for the active request.
	 */
	public static void processAllEntryProbeExtensions(Event event, RequestContext requestContext) {
		if (event == requestContext.getRootEvent()) {
			// Add the request to Active Request list
			requestContext.setRequestContextIndex(activeRequests.add(requestContext)); 
		}

		List<ProbeExtension> probeExtnList = RequestProbeService.getProbeExtensions();
		for (int i = 0; i < probeExtnList.size(); i ++) {
			ProbeExtension probeExtension = probeExtnList.get(i);
			try{
				// Entry enabled??
				if (probeExtension.invokeForEventEntry()) { 
					// To be sampled ??
					if (requestContext.getRequestId().getSequenceNumber() % probeExtension.getRequestSampleRate() == 0) { 
						if (event == requestContext.getRootEvent() && probeExtension.invokeForRootEventsOnly() == true
								&& (probeExtension.invokeForEventTypes() == null || probeExtension.invokeForEventTypes().contains(event.getType()))) {
							probeExtension.processEntryEvent(event, requestContext);
						}
						if (probeExtension.invokeForRootEventsOnly() == false
								&& (probeExtension.invokeForEventTypes() == null || probeExtension.invokeForEventTypes().contains(event.getType()))) {
							probeExtension.processEntryEvent(event, requestContext);
						}
					}
				}
			}catch(Exception e){
				if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "----------------Probe extension invocation failure---------------");
					Tr.debug(tc, probeExtension.getClass().getName() + ".processEntryEvent failed because of the following reason:" );
					Tr.debug(tc, e.getMessage());
				}
				FFDCFilter.processException(e, RequestProbeService.class.getName() + ".processAllEntryProbeExtensions", "148");
			}
		}
	}

	/**
	 * Iterate through all the probe extensions and process all the exit method
	 * of the available probe extension
	 * 
	 * @param event
	 *            : Event for which the probe extensions to be processed.
	 * @param requestContext
	 *            : Request context for the active request.
	 */
	public static void processAllExitProbeExtensions(Event event, RequestContext requestContext) {

		List<ProbeExtension> probeExtnList = RequestProbeService.getProbeExtensions();
		for (int i = 0; i < probeExtnList.size(); i ++) {
			ProbeExtension probeExtension = probeExtnList.get(i);
			try{
				// Exit enabled??
				if (probeExtension.invokeForEventExit()) {
					// To be sampled ??
					if (requestContext.getRequestId().getSequenceNumber() % probeExtension.getRequestSampleRate() == 0) {
						if (event == requestContext.getRootEvent() && probeExtension.invokeForRootEventsOnly() == true
								&& (probeExtension.invokeForEventTypes() == null || probeExtension.invokeForEventTypes().contains(event.getType()))) {
							probeExtension.processExitEvent(event, requestContext);
						}
						if (probeExtension.invokeForRootEventsOnly() == false
								&& (probeExtension.invokeForEventTypes() == null || probeExtension.invokeForEventTypes().contains(event.getType()))) {
							probeExtension.processExitEvent(event, requestContext);
						}
					}
				}
			}catch(Exception e){
				if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "----------------Probe extension invocation failure---------------");
					Tr.debug(tc, probeExtension.getClass().getName() + ".processExitEvent failed because of the following reason:" );
					Tr.debug(tc, e.getMessage());
				}
				FFDCFilter.processException(e, RequestProbeService.class.getName() + ".processAllExitProbeExtensions", "185");
			}
		}
		if (event == requestContext.getRootEvent()) {
			// Remove the request from active request list
			try{
				RequestContext storedRequestContext = activeRequests.get(requestContext.getRequestContextIndex());
				// 1) Check to handle stale requests.
				// A long running stale request from the last time the feature was enabled could potentially 
				// end up evicting a valid request that is occupying the same slot in the list.
				// 2) Also check if the returned request context is null, this can happen when we remove the feature while
				// a request is executing as we clean up the active requests list and the slot no longer holds a request context.
				if(storedRequestContext != null && (storedRequestContext.getRequestId() == requestContext.getRequestId()))
					activeRequests.remove(requestContext.getRequestContextIndex());
			}catch(ArrayIndexOutOfBoundsException e){
				//Do nothing as this can fail for an in-flight request when the feature is disabled
				//Rational being, the active request list gets reset and this index can no longer be valid.
			}
		}
	}	

	/**
	 * Iterate through all the probe extensions and process the counter methods
	 * of interested probe extensions
	 * 
	 * @param event
	 *            : Event for which the probe extensions to be processed.
	 */
	public static void processAllCounterProbeExtensions(Event event){

		List<ProbeExtension> probeExtnList = RequestProbeService.getProbeExtensions();
		for (int i = 0; i < probeExtnList.size(); i ++) {
			ProbeExtension probeExtension = probeExtnList.get(i);
			try{
				//Check if this probe extension is interested in 
				//counter events
				if(probeExtension.invokeForCounter()){
					probeExtension.processCounter(event);
				}
			}catch(Exception e){
				if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "----------------Probe extension invocation failure---------------");
					Tr.debug(tc, probeExtension.getClass().getName() + ".processCounterEvent failed because of the following reason:" );
					Tr.debug(tc, e.getMessage());
				}
				FFDCFilter.processException(e, RequestProbeService.class.getName() + ".processAllCounterProbeExtensions", "215");
			}
		}
	}

	public static List<RequestContext> getActiveRequests() {
		return activeRequests.getAll();
	}

	public static List<ProbeExtension> getProbeExtensions() {
		//return new ArrayList<ProbeExtension>(probeExtensions);
		return probeExtensions;
	}

}
