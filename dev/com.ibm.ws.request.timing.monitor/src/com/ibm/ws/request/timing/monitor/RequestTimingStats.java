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

import com.ibm.websphere.request.timing.RequestTimingStatsMXBean;
import com.ibm.ws.request.timing.stats.RequestTiming;

/**
 * This is used to report request timing stats.
 * Each supported request type will have one instance of RequestTimingStatsMXBean.
 * Statistic reported :
 * 1) Total Requests
 * 2) Current Active Requests
 * 3) Slow Requests count
 * 4) Hung Requests count.
 * 
 * This class receives a RequestTiming OSGI object and calls the 
 * corresponding methods to get the counts then returned as 
 * a new com.ibm.websphere.monitor.jmx.Counter object with currentValue set.
 * 
 */
public class RequestTimingStats implements RequestTimingStatsMXBean {

	// Following is the stats we are reporting for request types
	private RequestTiming requestTimingobject = null;
	private String[] filterTypes;

	/**
	 * Constructor. Takes in the RequestTiming object and a type to filter the
	 * request counts by
	 */
	public RequestTimingStats(RequestTiming requestTiming, String type) {
		this.requestTimingobject = requestTiming;
		this.filterTypes = new String[] { type };
	}

	/**
	 * Constructor. Takes in the RequestTiming object and a array of types to filter
	 * the request counts by. This is added for the workaround mentioned in
	 * RequestTimingMonitor
	 */
	public RequestTimingStats(RequestTiming requestTiming, String[] type) {
		this.requestTimingobject = requestTiming;
		this.filterTypes = type;
	}

    /**
     * Method getRequestCount()
     * This is returning the total request counts and passes the type
     * Type = Long.
     * Data: count
     **/
	@Override
	public long getRequestCount() {
		long totalCount = 0L;
		for (String type : filterTypes) {
			totalCount += requestTimingobject.getRequestCount(type);
		}
		return totalCount;
	}

    /**
     * Method getActiveRequestCount()
     * This is returning the active request counts and passes the type
     * Type = Long.
     * Data: count
     **/
	@Override
	public long getActiveRequestCount() {
		long activeCount = 0L;
		for (String type : filterTypes) {
			activeCount += requestTimingobject.getActiveRequestCount(type);
		}
		return activeCount;
	}
	
    /**
     * Method getSlowRequestCount()
     * This is returning the slow request counts and passes the type
     * Type = Long.
     * Data: count
     **/
	@Override
	public long getSlowRequestCount() {
		long slowCount = 0L;
		for (String type : filterTypes) {
			slowCount += requestTimingobject.getSlowRequestCount(type);
		}
		return slowCount;
	}
	
    /**
     * Method getHungRequestCount()
     * This is returning the hung request counts and passes the type
     * Type = Long.
     * Data: count
     **/
	@Override
	public long getHungRequestCount() {
		long hungCount = 0L;
		for (String type : filterTypes) {
			hungCount += requestTimingobject.getHungRequestCount(type);
		}
		return hungCount;
	}

}
