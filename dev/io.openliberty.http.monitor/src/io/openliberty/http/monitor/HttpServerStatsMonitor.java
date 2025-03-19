/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.pmi.factory.StatisticActions;

import io.openliberty.http.monitor.metrics.MetricsManager;

import com.ibm.wsspi.http.HttpRequest;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.GenericServlet;

/**
 *
 */
@Monitor(group = "HTTP")
public class HttpServerStatsMonitor extends StatisticActions {

	private static final TraceComponent tc = Tr.register(HttpServerStatsMonitor.class);

	private static final ThreadLocal<HttpStatAttributes.Builder> tl_httpStatsBuilder = new ThreadLocal<HttpStatAttributes.Builder>();
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

		long elapsedNanos = System.nanoTime() - tl_startNanos.get();
		HttpStatAttributes.Builder retrievedHttpStatAttributesBuilder = tl_httpStatsBuilder.get();

		if (retrievedHttpStatAttributesBuilder == null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, "Unable to retrieve HttpStatAttributes. Unable to record time.");
			}
			return;
		}

		updateHttpStatDuration(retrievedHttpStatAttributesBuilder, Duration.ofNanos(elapsedNanos), null);

	}
	
	/**
	 * Resolve Network Protocol Info  - move to common utility package
	 * @param protocolInfo
	 * @param httpStatsBuilder
	 */
	private void resolveNetworkProtocolInfo(String protocolInfo, HttpStatAttributes.Builder httpStatsBuilder) {
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

		httpStatsBuilder.withNetworkProtocolName(networkProtocolName);
		httpStatsBuilder.withNetworkProtocolVersion(networkVersion);
	}
	
	@ProbeAtEntry
	@ProbeSite(clazz = "com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink", method = "sendResponse", args = "com.ibm.wsspi.http.channel.values.StatusCodes,java.lang.String,java.lang.Exception,boolean")
	public void atSendResponse(@This Object probedHttpDispatcherLinkObj, @Args Object[] myargs) {

		tl_httpStatsBuilder.set(null);; //reset just in case
		
		tl_startNanos.set(System.nanoTime());
		HttpStatAttributes.Builder builder = HttpStatAttributes.builder();

		
		boolean is4xxCode = false;
		
		/*
		 *  Get Status Code (and Exception if it exists)
		 */
		if (myargs.length > 0) {
			if (myargs[0] != null && myargs[0] instanceof StatusCodes) {
				StatusCodes statCode = (StatusCodes) myargs[0];
				
				builder.withResponseStatus(statCode.getIntCode());
				is4xxCode = (statCode.getIntCode() % 400 <= 99 );
			}

			if (myargs[2] != null && myargs[2] instanceof Exception) {
				Exception exception = (Exception) myargs[2];
				builder.withException(exception);
			}
		} else {
			// uh oh can't resolve status code and/or exceptions
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Could not resolve response code");
			}
		}

		if (probedHttpDispatcherLinkObj != null) {
			HttpDispatcherLink httpDispatcherLink = (HttpDispatcherLink)probedHttpDispatcherLinkObj;
			builder.withServerName(httpDispatcherLink.getRequestedHost());
			builder.withServerPort(httpDispatcherLink.getRequestedPort());
			
			 try {

					HttpRequest httpRequest = httpDispatcherLink.getRequest();
					if (!is4xxCode) {
						builder.withHttpRoute(httpRequest.getURI());
					} else {
						/*
						 * For 4xx response codes,
						 *  set HTTP route to "/"
						 */
						builder.withHttpRoute("/");
					}
					
					builder.withRequestMethod(httpRequest.getMethod());
					builder.withScheme(httpRequest.getScheme());
					resolveNetworkProtocolInfo(httpRequest.getVersion(), builder);

			 } catch(Exception e) {
				 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					 Tr.debug(tc, String.format("Exception occured %s" +  e));
				 }
			 }
			
			tl_httpStatsBuilder.set(builder);

		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Failed to obtain HttpDispatcherLink Object from probe");
			}
		}
	}

    /**
     * 
     * @param builder
     * @param duration
     * @param appName Can be null (would mean its from these probes -- ergo server, don't have to worry about unloading)
     */
	public void updateHttpStatDuration(HttpStatAttributes.Builder builder, Duration duration, String appName) {

		HttpStatAttributes httpStatsAttributes;
		
		httpStatsAttributes = builder.build();
		if (httpStatsAttributes == null) return;
		
		/*
		 * Create and/or update MBean
		 */
		String keyID = httpStatsAttributes.getHttpStatID();
				

		HttpServerStats httpServerStats = HttpConnByRoute.get(keyID);
		if (httpServerStats == null) {
			httpServerStats = initializeHttpStat(keyID, httpStatsAttributes, appName);
			//Shutdown by the monitor-1.0 filter - shows over
			if (httpServerStats == null) {
				return;
			}
		}

		//Monitor bundle when updating statistics will do synchronization
		httpServerStats.updateDuration(duration);
		
		
		if (MetricsManager.getInstance() != null ) {
			MetricsManager.getInstance().updateHttpMetrics(httpStatsAttributes, duration);
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
		
		//Shut down by monitor-1.0 filter attribute
		if (HttpConnByRoute.get(key) == null) {
			return null;
		}
		
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
	
    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ws.webcontainer.servlet.ServletWrapper", method = "destroy")
    public void atServletDestroy(@This GenericServlet s) {
    	
        String appName = (String) s.getServletContext().getAttribute("com.ibm.websphere.servlet.enterprise.application.name");
        removeStat(appName);
    	
    }

}
