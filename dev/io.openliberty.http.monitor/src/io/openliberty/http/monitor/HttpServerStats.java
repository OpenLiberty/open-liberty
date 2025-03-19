/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import java.time.Duration;
import java.util.Optional;

import com.ibm.websphere.monitor.jmx.Meter;
import com.ibm.websphere.monitor.meters.StatisticsMeter;

import io.openliberty.http.monitor.mbean.HttpServerStatsMXBean;

/**
 *
 */
public class HttpServerStats extends Meter implements HttpServerStatsMXBean {

	private final StatisticsMeter responseTime;
	private String requestMethod, httpRoute;
	private int responseStatus;

	private String errorType = null;;


	private String scheme, serverName, networkProtocolName, networkProtocolVersion;
	private int serverPort;

	public HttpServerStats() {

		responseTime = new StatisticsMeter();
		responseTime.setDescription("Cumulative Response Time (NanoSeconds) for a HTTP connection");
		responseTime.setUnit("ns");
	}

	public HttpServerStats(HttpStatAttributes httpStatAttributes) {
		responseTime = new StatisticsMeter();
		responseTime.setDescription("Cumulative Response Time (NanoSeconds) for a HTTP connection");
		responseTime.setUnit("ns");

		requestMethod = httpStatAttributes.getRequestMethod();
		httpRoute = httpStatAttributes.getHttpRoute();
		if (httpRoute == null) {
			httpRoute = "";
		}
		
		responseStatus = httpStatAttributes.getResponseStatus();

		scheme = httpStatAttributes.getScheme();

		networkProtocolName = httpStatAttributes.getNetworkProtocolName();

		networkProtocolVersion = httpStatAttributes.getNetworkProtocolVersion();

		serverName = httpStatAttributes.getServerName();

		serverPort = httpStatAttributes.getServerPort();

		errorType = httpStatAttributes.getErrorType();
		if (errorType == null) {
			errorType = "";
		}
	}

	/**
	 * 
	 * @param durationNanos in nanoseconds
	 */
	public void updateDuration(long durationNanos) {
		responseTime.addDataPoint(durationNanos);
	}

	public void updateDuration(Duration duration) {
		responseTime.addDataPoint(duration.toNanos());
	}

	@Override
	public double getDuration() {
		return responseTime.getTotal();
	}

	@Override
	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	@Override
	public int getResponseStatus() {
		return responseStatus;
	}

	@Override
	public String getHttpRoute() {
		return httpRoute;
	}

	@Override
	public long getCount() {
		return responseTime.getCount();
	}

	@Override
	public String getScheme() {

		return scheme;
	}

	@Override
	public String getNetworkProtocolName() {

		return networkProtocolName;
	}

	@Override
	public String getNetworkProtocolVersion() {

		return networkProtocolVersion;
	}

	@Override
	public String getServerName() {

		return serverName;
	}

	@Override
	public int getServerPort() {

		return serverPort;
	}

	@Override
	public String getErrorType() {
		return errorType;
	}

}
