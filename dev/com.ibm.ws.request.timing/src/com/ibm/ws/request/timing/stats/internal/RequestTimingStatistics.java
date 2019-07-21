/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.timing.stats.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.request.probe.RequestProbeService;
import com.ibm.ws.request.timing.probeExtensionImpl.HungRequestProbeExtension;
import com.ibm.ws.request.timing.probeExtensionImpl.SlowRequestProbeExtension;
import com.ibm.ws.request.timing.stats.RequestTiming;
import com.ibm.wsspi.requestContext.RequestContext;

/**
 * Implementation of the RequestTiming interface. This class reports the
 * following statistics:
 * 
 * 1) TotalRequestCount: Retrieves the requestCountMap from
 * HungRequestTimingConfig and filters based on type
 * 
 * 2) ActiveRequestCount: Retrieves the activeRequests map from
 * RequestProbeService and then filters based on type
 * 
 * 3) SlowRequestCount: Retrieves the slowRequestsCount from SlowRequest and
 * returns the size of the map indicating the number of slow requests
 * 
 * 4) HungRequestCount: Retrieves the hungRequestsCount from HungRequest and
 * returns the size of the map indicating the number of hung requests
 */

public class RequestTimingStatistics implements RequestTiming {

	/** Trace service */
	private static final TraceComponent tc = Tr.register(RequestTimingStatistics.class);

	/**
	 * Store a reference to SlowRequestProbeExtension. SlowRequestProbeExtension
	 * will be used to retrieve slow request count values for specific types.
	 */
	private volatile SlowRequestProbeExtension slowRequestProbeExt = null;

	/**
	 * Store a reference to HungRequestProbeExtension. HungRequestProbeExtension
	 * will be used to retrieve hung and total request count values for specific
	 * types.
	 */
	private volatile HungRequestProbeExtension hungRequestProbeExt = null;


	/**
	 * This returns the total number of requests reported by HungRequestTimingConfig
	 * in the HungRequestProbeExtension. It can filter the requests by type
	 */
	@Override
	public long getRequestCount(String type) {
		return hungRequestProbeExt.getTotalRequestCount(type);
	}

	/**
	 * This returns the total number of active requests reported by
	 * RequestProbeService It can filter the requests by type
	 */
	@Override
	public long getActiveRequestCount(String type) {
		long activeRequestCounts = 0;
		for (RequestContext requestcontext : RequestProbeService.getActiveRequests()) {
			if (requestcontext.getRootEvent().getType().equals(type)) {
				activeRequestCounts++;
			}
		}
		return activeRequestCounts;
	}

	/**
	 * This returns the total number of slow requests reported by SlowRequestManager
	 * It can filter the requests by type
	 */
	@Override
	public long getSlowRequestCount(String type) {
		return slowRequestProbeExt.getSlowRequestCount(type);
	}

	/**
	 * This returns the total number of hung requests reported by HungRequestManager
	 * in the HungRequestProbeExtension. It can filter the requests by type
	 */
	@Override
	public long getHungRequestCount(String type) {
		return hungRequestProbeExt.getHungRequestCount(type);
	}

	@Activate
	protected void activate() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Activating " + this);
		}
	}

	@Deactivate
	protected void deactivate() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, " Deactivating " + this);
		}
	}

	/**
	 * OSGI Setter
	 * 
	 * @param probeExt
	 */
	protected void setHungRequestProbeExt(HungRequestProbeExtension probeExt) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Setting probe extension : " + probeExt.getClass().getName());
		}
		hungRequestProbeExt = probeExt;
	}

	/**
	 * OSGI Unsetter
	 * 
	 * @param probeExt
	 */
	protected void unsetHungRequestProbeExt(HungRequestProbeExtension probeExt) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Unsetting probe extension : " + probeExt.getClass().getName());
		}
		hungRequestProbeExt = null;
	}

	/**
	 * OSGI Setter
	 * 
	 * @param probeExt
	 */
	protected void setSlowRequestProbeExt(SlowRequestProbeExtension probeExt) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Setting probe extension : " + probeExt.getClass().getName());
		}
		slowRequestProbeExt = probeExt;
	}

	/**
	 * OSGI Unsetter
	 * 
	 * @param probeExt
	 */
	protected void unsetSlowRequestProbeExt(SlowRequestProbeExtension probeExt) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
			Tr.event(tc, "Unsetting probe extension : " + probeExt.getClass().getName());
		}
		slowRequestProbeExt = null;
	}
}
