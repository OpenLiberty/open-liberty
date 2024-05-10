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

import com.ibm.websphere.monitor.annotation.Args;
import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.pmi.factory.StatisticActions;

import io.openliberty.http.monitor.metrics.RestMetricManager;

import com.ibm.wsspi.http.HttpRequest;

import java.time.Duration;
import java.util.Optional;

/**
 *
 */
@Monitor(group = "HttpStats")
public class HttpStatsMonitor extends StatisticActions {

	private static final TraceComponent tc = Tr.register(HttpStatsMonitor.class);

	private static final ThreadLocal<HttpStatAttributes> tl_httpStats = new ThreadLocal<HttpStatAttributes>();
	private static final ThreadLocal<Long> tl_startNanos = new ThreadLocal<Long>();
	
	public static HttpStatsMonitor instance;

	/*
	 * Instance block to create singleton.
	 * The "Liberty-Monitoring-Components" in the bnd.bnd
	 * specifies the monitor runtime to create an instance
	 * of this class. We'll leverage that to
	 * create the singleton.
	 * 
	 * Unconventional as we well set "this" particular instance.
	 */
	{
		System.out.println("init HttpStatMonitor singleton");
		if (instance == null) {
			System.out.println(" set ins tance of httpStatMonitor");
			instance = this;
		} else {
			Tr.debug(tc, String.format("Multiple attempts to create %s. We already have an instance", HttpStatsMonitor.class.getName()));
		}

	}

	public static HttpStatsMonitor getInstance() {
		if (instance != null) {
			return instance;
		} else {
			System.err.println("no instance found");
			Tr.debug(tc, String.format("No instance of %s found", HttpStatsMonitor.class.getName()));
		}
		return null;
	}

	@PublishedMetric
	public MeterCollection<HttpStats> HttpConnByRoute = new MeterCollection<HttpStats>("HttpMetrics", this);

	@ProbeAtReturn
	@ProbeSite(clazz = "com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink", method = "sendResponse", args = "com.ibm.wsspi.http.channel.values.StatusCodes,java.lang.String,java.lang.Exception,boolean")
	public void atSendResponseReturn(@This Object probedHttpDispatcherLinkObj) {

		System.out.println("probe out " + Thread.currentThread());
		
		long elapsedNanos = System.nanoTime() - tl_startNanos.get();
		HttpStatAttributes retrievedHttpStatAttr = tl_httpStats.get();

		if (retrievedHttpStatAttr == null) {
			Tr.debug(tc, "probing out - Unable to retrieve HttpStatAttributes");
			System.err.println("probing out - Unable to retrieve HttpStatAttributes");
			return;
		}

		updateHttpStatDuration(retrievedHttpStatAttr, Duration.ofNanos(elapsedNanos));

	}
	
	/**
	 * Resolve Network Protocol Info  - move to common utility package
	 * @param protocolInfo
	 * @param httpStat
	 */
	private void resolveNetwortProtocolInfo(String protocolInfo, HttpStatAttributes httpStat) {
		String[] networkInfo = protocolInfo.trim().split("/");
		
	
		String networkProtocolName = null;
		String networkVersion = "";
		if (networkInfo.length == 1) {
			networkProtocolName = networkInfo[0].toLowerCase();
		} else if (networkInfo.length == 2) {
			networkProtocolName = networkInfo[0];
			networkVersion = networkInfo[1];
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, String.format("More values than expected when parsing protocol information: [%s]", protocolInfo) );
			}
		}

		httpStat.setNetworkProtocolName(networkProtocolName);
		httpStat.setNetworkProtocolVersion(networkVersion);
	}
	
	@ProbeAtEntry
	@ProbeSite(clazz = "com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink", method = "sendResponse", args = "com.ibm.wsspi.http.channel.values.StatusCodes,java.lang.String,java.lang.Exception,boolean")
	public void atSendResponse(@This Object probedHttpDispatcherLinkObj, @Args Object[] myargs) {
		
		tl_httpStats.set(null);; //reset just in case
		
		System.out.println("probe in " + Thread.currentThread());
		
		tl_startNanos.set(System.nanoTime());
		HttpStatAttributes httpStatAttributes = new HttpStatAttributes();

		/*
		 *  Get Status Code (and Exception if it exists)
		 */
		if (myargs.length > 0) {
			if (myargs[0] != null && myargs[0] instanceof StatusCodes) {
				StatusCodes statCode = (StatusCodes) myargs[0];
				httpStatAttributes.setResponseStatus(statCode.getIntCode());
			}

			if (myargs[2] != null && myargs[2] instanceof Exception) {
				Exception exception = (Exception) myargs[2];
				httpStatAttributes.setException(exception);
			}
		} else {
			// uh oh can't resolve status code and/or exceptions
			System.err.println("Could not resolve response code");
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Could not resolve response code");
			}
		}

		if (probedHttpDispatcherLinkObj != null) {
			HttpDispatcherLink httpDispatcherLink = (HttpDispatcherLink)probedHttpDispatcherLinkObj;
			httpStatAttributes.setServerName(httpDispatcherLink.getRequestedHost());
			httpStatAttributes.setServerPort(httpDispatcherLink.getRequestedPort());
			
			 try {

					HttpRequest httpRequest = httpDispatcherLink.getRequest();
					
					httpStatAttributes.setHttpRoute(httpRequest.getURI());
					httpStatAttributes.setRequestMethod(httpRequest.getMethod());
					httpStatAttributes.setScheme(httpRequest.getScheme());
					resolveNetwortProtocolInfo(httpRequest.getVersion(), httpStatAttributes);

			 } catch(Exception e) {
				 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					 Tr.debug(tc, String.format("Exception occured %s" +  e));
				 }
			 }
			
			tl_httpStats.set(httpStatAttributes);

		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Failed to obtain HttpDispatcherLink Object from probe");
			}
		}
	}


	public void updateHttpStatDuration(HttpStatAttributes httpStatAttributes, Duration duration) {

		/*
		 * First validate that we got all properties.
		 */
		if (!httpStatAttributes.validate()) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, String.format("Invalid HTTP Stats attributes : \n %s", httpStatAttributes.toString()));
			}
		}
		
		/*
		 * Create and/or update MBean
		 */
		String key = resolveStatsKey(httpStatAttributes);

		HttpStats hms = HttpConnByRoute.get(key);
		if (hms == null) {
			hms = initializeHttpStat(key, httpStatAttributes);
		}

		//Monitor bundle when updating statistics will do synchronization
		hms.updateDuration(duration);

		
		
		if (RestMetricManager.getInstance() != null ) {
			RestMetricManager.getInstance().updateHttpMetrics(httpStatAttributes, duration);
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No Available Metric runtimes to forward HTTP stats to.");
			}
		}
	}

	private synchronized HttpStats initializeHttpStat(String key, HttpStatAttributes statAttri) {
		/*
		 * Check again it was added, thread that was blocking may have been adding it
		 */
		if (HttpConnByRoute.get(key) != null) {
			return HttpConnByRoute.get(key);
		}

		HttpStats httpMetricStats = new HttpStats(statAttri);
		HttpConnByRoute.put(key, httpMetricStats);
		return httpMetricStats;
	}

	/**
	 * Resolve the object name (specifically the name property)
	 * <code> domain:type=type,name="this"</code>
	 * 
	 * @param httpStatAttributes
	 * @return
	 */
	private String resolveStatsKey(HttpStatAttributes httpStatAttributes) {
		
		Optional<String> httpRoute = httpStatAttributes.getHttpRoute();
		Optional<String> errorType = httpStatAttributes.getErrorType();
		String requestMethod = httpStatAttributes.getRequestMethod();
		Optional<Integer> responseStatus = httpStatAttributes.getResponseStatus();

		StringBuilder sb = new StringBuilder();
		sb.append("\""); // starting quote
		sb.append("method:" + requestMethod);
		
		/*
		 * Status, Route  and errorType may be null.
		 * In which cas we will not append it to the name property
		 */
		responseStatus.ifPresent(status -> sb.append(";status:" + status));

		
		httpRoute.ifPresent(route -> {
			sb.append(";httpRoute:" + route.replace("*", "\\*"));
		});

		httpRoute.ifPresent(route -> {
			sb.append(";errorType:" + errorType);
		});

		sb.append("\""); // ending quote
		return sb.toString();
	}

}
