/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.http.HttpRequest;


import io.openliberty.microprofile.metrics50.SharedMetricRegistries;

import com.ibm.wsspi.genericbnf.HeaderField;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 *
 */
@Monitor(group = "HttpStats")
public class HttpStatsMonitor extends StatisticActions {

	private static final TraceComponent tc = Tr.register(HttpStatsMonitor.class);

	private final ThreadLocal<HttpStatAttributes> tl_httpStats = new ThreadLocal<HttpStatAttributes>();
	private final ThreadLocal<Long> tl_startNanos = new ThreadLocal<Long>();

	/*
	 * Set singleton
	 */
	{
		if (instance == null) {
			System.out.println("Registering singleton actual");
			instance = this;
		} else {
			Tr.debug(tc, "singleton already registered " + instance);
		}

	}

	public static HttpStatsMonitor getInstance() {
		if (instance != null) {
			return instance;
		} else {
			System.out.println("DC: no instance found");
		}
		return null;
	}

	public static HttpStatsMonitor instance;

	@PublishedMetric
	public MeterCollection<HttpStats> HttpConnByRoute = new MeterCollection<HttpStats>("HttpMetrics", this);

	@ProbeAtReturn
	@ProbeSite(clazz = "com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink", method = "sendResponse", args = "com.ibm.wsspi.http.channel.values.StatusCodes,java.lang.String,java.lang.Exception,boolean")
	public void atSendResponseReturn(@This Object probedHttpDispatcherLinkObj) {

		long elapsedNanos = System.nanoTime() - tl_startNanos.get();
		HttpStatAttributes retrievedHttpStatAttr = tl_httpStats.get();

		System.out.println(" welcome home ");

		System.out.println(retrievedHttpStatAttr);

		if (retrievedHttpStatAttr == null) {
			// Pretty important to get those httpAttributes
			return;
		}

		updateHttpStatDuration(retrievedHttpStatAttr, Duration.ofNanos(elapsedNanos));

	}

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
			// there shouldn't be more than two values.
		}

		httpStat.setNetworkProtocolName(networkProtocolName);
		httpStat.setNetworkProtocolVersion(networkVersion);
	}

	private void testJustHttpDispatcherLink(HttpDispatcherLink hdl ) {
		
		System.out.println("Testing it out - START");
		//will be able to get status
		System.out.println();
		System.out.println("hdl host " + hdl.getRequestedHost());
		System.out.println("hdl port " + hdl.getRequestedPort());
		
		
		HttpRequest httpRequest = hdl.getRequest();
		

		System.out.println(" class of httpRequest " + httpRequest);
		System.out.println("httpRequest URI " + httpRequest.getURI());
		System.out.println("httpRequest method " + httpRequest.getMethod());
		System.out.println("httpRequest scheme " + httpRequest.getScheme());
		System.out.println("httpRequest ver" + httpRequest.getVersion());
		System.out.println("httpRequest virHost " + httpRequest.getVirtualHost());
		System.out.println("httpRequest virPort " + httpRequest.getVirtualPort());
		//System.out.println("httpRequest " + httpRequest.);
		
		
		System.out.println("Testing it out - STOP");
		
	}
	
	@ProbeAtEntry
	@ProbeSite(clazz = "com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink", method = "sendResponse", args = "com.ibm.wsspi.http.channel.values.StatusCodes,java.lang.String,java.lang.Exception,boolean")
	public void atSendResponse(@This Object probedHttpDispatcherLinkObj, @Args Object[] myargs) {

		tl_startNanos.set(System.nanoTime());
		HttpStatAttributes httpStatAttributes = new HttpStatAttributes();

		// Get Status Code and Exception if it exists
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
		}

		if (probedHttpDispatcherLinkObj != null) {

			testJustHttpDispatcherLink((HttpDispatcherLink)probedHttpDispatcherLinkObj);
			
//				Method getRequestMethod = probedHttpDispatcherLinkObj.getClass().getMethod("getRequest", null);
//
//				Object httpRequestObj = getRequestMethod.invoke(probedHttpDispatcherLinkObj, null);

			try {
				Field iscField;

				iscField = probedHttpDispatcherLinkObj.getClass().getDeclaredField("isc");

				iscField.setAccessible(true);

				Object httpInboundServiceContextImplObj = iscField.get(probedHttpDispatcherLinkObj);

				// TODO: ^^^^what if "httpInboundServiceContextImplObj" is null?! -- or it blows up due to modifying access
				// (i.e. the catches below)
				// Maybe use a method to encapsulate , if failure or null or excepton use
				// alternative method

				HttpInboundServiceContext hisc = (HttpInboundServiceContext) httpInboundServiceContextImplObj;
				
				//TODO: vv can this be null? handle that?
				HttpRequestMessage httpRequestMessage = hisc.getRequest();
				
				
				//DC: did i even need this? could've just used HDL -> getRequest -> getInfo
				httpStatAttributes.setRequestMethod(httpRequestMessage.getMethod());
				httpStatAttributes.setScheme(httpRequestMessage.getScheme());

				resolveNetwortProtocolInfo(httpRequestMessage.getVersion(), httpStatAttributes);

				String serverName = null;

				/*
				 * Calculate the server-name First try to take it from the Host header
				 * 
				 * failing that, use InetAddress and get the IP
				 */
				HeaderField hostHeaderField = httpRequestMessage.getHeader("Host");
				String sHostHeader = hostHeaderField.asString();
				if (sHostHeader.contains(":")) {
					serverName = sHostHeader.split(":")[0];
				} else {
					serverName = sHostHeader;
				}

				if (serverName == null) {
					serverName = hisc.getLocalAddr().getHostAddress();
				}

				System.out.println("servername " + serverName);
				httpStatAttributes.setServerName(serverName);

				System.out.println("port " + hisc.getLocalPort());
				httpStatAttributes.setServerPort(hisc.getLocalPort());

			} catch (NoSuchFieldException e) { // getDeclaredField() call
				e.printStackTrace();
			} catch (SecurityException e) { // getDeclaredField() call
				e.printStackTrace();
			} catch (IllegalArgumentException e) { // iscField.get call
				e.printStackTrace();
			} catch (IllegalAccessException e) { // iscField.get call
				e.printStackTrace();
			}
			tl_httpStats.set(httpStatAttributes);

		} else {
			System.out.println("DC: failure of acquiring obj when probed fired");
		}

	}

	// sync static methods for adding/retrieving

	public void updateHttpStatDuration(HttpStatAttributes statAttri, Duration duration) {

		/*
		 * Deal with Mbean
		 */
		String key = resovleKey(statAttri);

		HttpStats hms = HttpConnByRoute.get(key);
		if (hms == null) {
			hms = iniitializeHttpStat(key, statAttri);
		}

		hms.updateDuration(duration);

		/*
		 * Deal with Metrics
		 */
		try {
			if (Class.forName(SharedMetricRegistries.class.getName()) != null) {
				RestMetricManager.updateHttpMetrics(statAttri, duration);
			}
		} catch (ClassNotFoundException e) {

			System.out.println("SharedMetricRegistries does not exist");
			e.printStackTrace();
		}

//		nothing for now
	}

	private synchronized HttpStats iniitializeHttpStat(String key, HttpStatAttributes statAttri) {
		/*
		 * Check again it was added, thread that was blocking may have been adding it
		 */
		if (HttpConnByRoute.get(key) != null) {
			return HttpConnByRoute.get(key);
		}

		HttpStats httpMetricStats = new HttpStats();
		HttpConnByRoute.put(key, httpMetricStats);
		return httpMetricStats;

	}

	/*
	 * Calculate the key to put in - unique one for METHOD / RESPONSE / ROUTE
	 * 
	 * Minimal produceable key is : METHOD IF Server encounters error , can create :
	 * METHOD / RESPONSE
	 * 
	 */
	private String resovleKey(HttpStatAttributes statAttri) {
//		Each key is a nonempty string of characters which may not contain any of the characters comma (,), equals (=), colon, asterisk, or question mark. The same key may not occur twice in a given ObjectName.
//
//		Each value associated with a key is a string of characters that is either unquoted or quoted.
		Optional<String> httpRoute = statAttri.getHttpRoute();
		String requestMethod = statAttri.getRequestMethod();
		Optional<Integer> responseStatus = statAttri.getResponseStatus();
		Optional<Exception> exception = statAttri.getException();

		StringBuilder sb = new StringBuilder();
		sb.append("\""); // starting quote
		sb.append("method:" + requestMethod);
		responseStatus.ifPresent(status -> sb.append(";status:" + status));
		httpRoute.ifPresent(route -> {
			sb.append(";httpRoute:" + route.replace("*", "\\*"));
		});
		exception.ifPresent(throwable -> sb.append(";error:" + exception.getClass().getName()));

		// double check if responseStatus is 4xx or 5xx
		responseStatus.ifPresent(status -> {
			if (status > 400) {
				sb.append(";error:" + status);
			}
		});
		sb.append("\""); // ending quote
		return sb.toString();
	}

}
