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
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.webcontainer.servlet.ServletWrapper;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.pmi.factory.StatisticActions;

import io.openliberty.http.monitor.metrics.MetricsManager;

import com.ibm.wsspi.http.HttpRequest;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.GenericServlet;

/**
 *
 */
@Monitor(group = "HTTP")
public class HttpServerStatsMonitor extends StatisticActions {

	private static final TraceComponent tc = Tr.register(HttpServerStatsMonitor.class);

	private static final ThreadLocal<HttpStatAttributes> tl_httpStats = new ThreadLocal<HttpStatAttributes>();
	private static final ThreadLocal<Long> tl_startNanos = new ThreadLocal<Long>();
	
	private static final ConcurrentHashMap<String,Set<String>> appNameToStat = new ConcurrentHashMap<String,Set<String>>();
	
	private static HttpServerStatsMonitor instance;

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
		if (instance == null) {
			instance = this;
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, String.format("Multiple attempts to create %s. We already have an instance", HttpServerStatsMonitor.class.getName()));
			}
		}

	}

	public static HttpServerStatsMonitor getInstance() {
		if (instance != null) {
			return instance;
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, String.format("No instance of %s found", HttpServerStatsMonitor.class.getName()));
			}

		}
		return null;
	}

	@PublishedMetric
	public MeterCollection<HttpServerStats> HttpConnByRoute = new MeterCollection<HttpServerStats>("HttpMetrics", this);

	@ProbeAtReturn
	@ProbeSite(clazz = "com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink", method = "sendResponse", args = "com.ibm.wsspi.http.channel.values.StatusCodes,java.lang.String,java.lang.Exception,boolean")
	public void atSendResponseReturn(@This Object probedHttpDispatcherLinkObj) {

		/*
		 * Just prevent probed code execution. This bundle starts from an auto-feature.
		 * User's are not explicitly enabling this so lets not throw an exception. We'll
		 * quietly get out of the way.
		 */
		if (!ProductInfo.getBetaEdition()) {
			return;
		}

		long elapsedNanos = System.nanoTime() - tl_startNanos.get();
		HttpStatAttributes retrievedHttpStatAttr = tl_httpStats.get();

		if (retrievedHttpStatAttr == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, "Unable to retrieve HttpStatAttributes. Unable to record time.");
			}
			return;
		}

		updateHttpStatDuration(retrievedHttpStatAttr, Duration.ofNanos(elapsedNanos), null);

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

		/*
		 * Just prevent probed code execution. This bundle starts from an auto-feature.
		 * User's are not explicitly enabling this so lets not throw an exception. We'll
		 * quietly get out of the way.
		 */
		if (!ProductInfo.getBetaEdition()) {
			return;
		}

		tl_httpStats.set(null);; //reset just in case
		
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

    /**
     * 
     * @param httpStatAttributes
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
	public void updateHttpStatDuration(HttpStatAttributes httpStatAttributes, Duration duration, String appName) {

		/*
		 * Just prevent this from happening. This bundle starts from an auto-feature.
		 * User's are not explicitly enabling this so lets not throw an exception. We'll
		 * quietly get out of the way.
		 * 
		 * Other beta blocks should prevent this from ever happening. But regardless,
		 * this method is the lynch-pin. This is where MBean is registered and Metrics
		 * are registered to Metric/Meter registries.
		 */
		if (!ProductInfo.getBetaEdition()) {
			return;
		}

		/*
		 * First validate that we got all properties.
		 */
		if (!httpStatAttributes.validate()) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, String.format("Invalid HTTP Stats attributes : \n %s", httpStatAttributes.toString()));
			}
			return;
		}
		
		/*
		 * Create and/or update MBean
		 */
		String key = resolveStatsKey(httpStatAttributes);

		HttpServerStats hss = HttpConnByRoute.get(key);
		if (hss == null) {
			hss = initializeHttpStat(key, httpStatAttributes, appName);
		}

		//Monitor bundle when updating statistics will do synchronization
		hss.updateDuration(duration);

		
		
		if (MetricsManager.getInstance() != null ) {
			MetricsManager.getInstance().updateHttpMetrics(httpStatAttributes, duration);
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "No Available Metric runtimes to forward HTTP stats to.");
			}
		}
	}

	private synchronized HttpServerStats initializeHttpStat(String key, HttpStatAttributes statAttri, String appName) {
		/*
		 * Check again it was added, thread that was blocking may have been adding it
		 */
		if (HttpConnByRoute.get(key) != null) {
			return HttpConnByRoute.get(key);
		}

		HttpServerStats httpMetricStats = new HttpServerStats(statAttri);
		HttpConnByRoute.put(key, httpMetricStats);
		
		/*
		 * null means from server.
		 * Specifically splash page.
		 * 
		 * Add to appName -> stat cache
		 */
		if (appName != null) {
			appNameToStat.compute(appName, (appNameKey, currValSet) -> {
				if (currValSet == null) {
					HashSet<String> hs = new HashSet<String>();
					hs.add(key);
					return hs;
				} else {
					currValSet.add(key);
					return currValSet;
				}
			});
		}
		
		return httpMetricStats;
	}
	
	
	public void removeStat(String appName) {
		Set<String> retSet = appNameToStat.get(appName);
		if (retSet != null) {
			for (String statName : retSet) {
				HttpConnByRoute.remove(statName);
			}
		}
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

		errorType.ifPresent(route -> {
			sb.append(";errorType:" + errorType);
		});

		sb.append("\""); // ending quote
		return sb.toString();
	}
	
    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ws.webcontainer.servlet.ServletWrapper", method = "destroy")
    public void atServletDestroy(@This GenericServlet s) {
    	
        String appName = (String) s.getServletContext().getAttribute("com.ibm.websphere.servlet.enterprise.application.name");
        removeStat(appName);
    	
    }

}
