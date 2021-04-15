/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.monitor;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.ws.request.timing.stats.RequestTiming;

/**
 * This class is responsible for calculating and reporting of Performance Data for each Request by types.
 * Following data we collect from each Request Type.
 * 1) Returns the total number of requests since the server started
 * 2) Returns the number of currently active requests
 * 3) Returns the slow requests that are beyond the slowThreshold set in server.xml
 * 4) Returns the hung requests that are beyond the hungThreshold set in server.xml
 * 
 * This requires a RequestTiming object to be injected and the RequestTimingStatsMXBean is registered 
 * thru the initRequestTimingStats.
 * 
 * Currently only supports SERVLET types
 * 
 */
@Component(property = { "service.vendor = IBM" })
@Monitor(group = "RequestTiming")
public class RequestTimingMonitor {
	
	private RequestTiming requestTimingobject = null;

	@PublishedMetric
	public MeterCollection<RequestTimingStats> servletCountByName = new MeterCollection<RequestTimingStats>("Servlet",
			this);

	// Adding requestType and their RequestContext type
	private static final Map<String, String[]> requestTypeMap;
	static {
		requestTypeMap = new HashMap<String, String[]>();
		// The workaround below that adds the counts associated with more than one
		// request event types for a servlet request is temporary. Root events can
		// change for any request type at any time and without notice. We
		// cannot foresee what new event type might break request timing stats in the
		// future.
		requestTypeMap.put("Servlet", new String []{"websphere.servlet.service","websphere.http.wrapHandlerAndExecute"});
		//Background:
		// The request timing support is intrinsically dependent on the probe framework.
		// The probe framework is a generic mechanism that allows components to register
		// specific methods/events (along the execution path) with it such that those
		// components are called by the probe framework prior and post (registered)
		// method execution. The probe framework calls components through events.
		// Therefore, a registered method is associated to an event and event type.
		// The probe framework is synchronous in nature and treats events (method calls)
		// along the request execution path as parent(root)/children events. The request
		// timing support uses the root/parent event notion to count active, slow, hung,
		// etc requests only once. Each request is counted/tracked per root event type.
		// This means that there can only be one root event type per request type if the
		// counts are to be accurate. For example, the expectation is that request
		// timing tracks all servlet requests under a single root event type called
		// websphere.servlet.service. In this particular case, there is is a method in
		// the servlet request execution path called *service() that was registered with
		// the probe framework and was associated to event type websphere.servlet.service.
		// It is this event type that is supposed to represent all servlet requests.
		// Given that the probe support is a generic event framework, the root event and
		// associated event type can change. More precisely, if there is another
		// component that registered method x()/event-type-x with the probe framework,
		// and method x() is along the servlet execution path, which is called before
		// the method associated with event websphere.servlet.service, the request
		// timing counts would now be tracked under method x()'s event type. This
		// exposes a flaw in the way request timing keeps track of request counts. For
		// instance, If there were to be dynamic configuration updates that enabled
		// components that changed what is considered to be the root event, the counts
		// for a request type would be split among different root events. One known case
		// where this takes place is when zosRequestLogging-1.0 is also enabled. The
		// enablement of this feature causes the root event type for servlet request to
		// change from websphere.servlet.service to
		// websphere.http.wrapperHandleAndExecute.
		// The agreed temporary solution is to simply allow the notion that request
		// types can have more than one root event type. This means that when retrieving
		// statistic counts for servlet requests, callers must manually track and add
		// the counts kept under the two potential and currently know root events:
		// websphere.servlet.service and websphere.http.wrapperHandleAndExecute.
	}

	@Activate
	protected void activate() {
	}

	@Deactivate
	protected void deactivate() {
		clearStatsCollection();
	}

	@Reference(service = RequestTiming.class, policyOption = ReferencePolicyOption.GREEDY)
	protected void setrequestTiming(RequestTiming requestTiming) {
		requestTimingobject = requestTiming;
		initRequestTimingStats();
	}

	protected void unsetrequestTiming(RequestTiming requestTiming) {
		requestTimingobject = null;
	}

	/**
	 * Method : initRequestTimingStats()
	 * 
	 * This method will create RequestTimingStats object for servlet type. This
	 * method needs to be synchronised.
	 * 
	 * This method gets called only after the reference has been set
	 * 
	 */
	public synchronized RequestTimingStats initRequestTimingStats() {
		String key = "Servlet";
		RequestTimingStats nStats = servletCountByName.get(key);
		if (nStats == null) {
			nStats = new RequestTimingStats(requestTimingobject, requestTypeMap.get(key));
			servletCountByName.put(key, nStats);
		}
		return nStats;
	}

	/**
	 * Method : clearStatsCollection()
	 * 
	 * This method will clear the meterCollection and unregisters the mbean
	 * accordingly
	 * 
	 * 
	 */
	public synchronized void clearStatsCollection() {
		String key = "Servlet";
		servletCountByName.remove(key);
	}

}
