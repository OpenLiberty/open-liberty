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
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.http.HttpRequest;

import com.ibm.wsspi.genericbnf.HeaderField;

import java.lang.reflect.Field;
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
			Tr.debug(tc, String.format("More values than expected when parsing protocol information: [%s]", protocolInfo) );
			System.err.println("Something has gone awry");
		}

		httpStat.setNetworkProtocolName(networkProtocolName);
		httpStat.setNetworkProtocolVersion(networkVersion);
	}

	//TODO: Use this instead. - related to the SendResponse
	private void resolveAtributesFromHttpDispatcherLink(HttpDispatcherLink hdl ) {
		
		System.out.println("Testing it out - START");
		//will be able to get status
		System.out.println();
		System.out.println("hdl host " + hdl.getRequestedHost());
		System.out.println("hdl port " + hdl.getRequestedPort());
		
		System.out.println("asdf");
		 hdl.getRequest();
		 System.out.println("zzd");
		
		 try {
			 
				HttpRequest httpRequest = hdl.getRequest();
				System.out.println(" class of httpRequest " + httpRequest);
				System.out.println("httpRequest URI " + httpRequest.getURI());
				System.out.println("httpRequest method " + httpRequest.getMethod());
				System.out.println("httpRequest scheme " + httpRequest.getScheme());
				System.out.println("httpRequest ver" + httpRequest.getVersion());
				
//				System.out.println("httpRequest virHost " + httpRequest.getVirtualHost());
//				System.out.println("httpRequest virPort " + httpRequest.getVirtualPort());
				
				
				System.out.println("Testing it out - STOP");
		 } catch(Exception e) {
			 System.out.println("oof" + e);
		 }
		
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
		}

		if (probedHttpDispatcherLinkObj != null) {

			//resolveAtributesFromHttpDispatcherLink((HttpDispatcherLink)probedHttpDispatcherLinkObj);
			
			HttpDispatcherLink httpDispatcherLink = (HttpDispatcherLink)probedHttpDispatcherLinkObj;
			httpStatAttributes.setServerName(httpDispatcherLink.getRequestedHost());
			httpStatAttributes.setServerPort(httpDispatcherLink.getRemotePort());
			
			
			 try {
				 
					HttpRequest httpRequest = httpDispatcherLink.getRequest();
					
//					System.out.println(" class of httpRequest " + httpRequest);
//					System.out.println("httpRequest URI " + httpRequest.getURI());
//					System.out.println("httpRequest method " + httpRequest.getScheme());
//					System.out.println("httpRequest scheme " + httpRequest.getScheme());
//					System.out.println("httpRequest ver" + httpRequest.getVersion());
					
					httpStatAttributes.setHttpRoute(httpRequest.getURI());
					httpStatAttributes.setRequestMethod(httpRequest.getMethod());
					httpStatAttributes.setScheme(httpRequest.getScheme());
					resolveNetwortProtocolInfo(httpRequest.getVersion(), httpStatAttributes);
					


			 } catch(Exception e) {
				 System.err.println("Something bad happened" + e);
			 }
			

			tl_httpStats.set(httpStatAttributes);

		} else {
			System.err.println("Failed to obtain HttpDispatcherLink Object from probe");
		}

	}

	// sync static methods for adding/retrieving

	@FFDCIgnore({ClassNotFoundException.class, NoClassDefFoundError.class})
	public void updateHttpStatDuration(HttpStatAttributes httpStatAttributes, Duration duration) {

		
		
		/*
		 * First validate that we got all properties.
		 */
		if (!httpStatAttributes.validate()) {
			System.err.println("Something isn't quite right");
			System.err.println(httpStatAttributes.toString());
		}
		
		/*
		 * Create and/or update MBean
		 */
		String key = resolveStatsKey(httpStatAttributes);

		HttpStats hms = HttpConnByRoute.get(key);
		if (hms == null) {
			hms = initializeHttpStat(key, httpStatAttributes);
		}

		hms.updateDuration(duration);

		/*
		 * Handle metrics
		 */
		try {
			/*
			 * Need to use String explicitly.
			 * Otherwise, if you started w/o metrics and load metrics feature,
			 * SharedMetricRegistries.class will throw NoClassDefFoundDError even
			 * if it is on bundle's class path.
			 */
			if (Class.forName("io.openliberty.microprofile.metrics50.SharedMetricRegistries") != null) {
				RestMetricManager.updateHttpMetrics(httpStatAttributes, duration);
			}
		} catch (ClassNotFoundException e) {
			Tr.debug(tc, "Class not found");
			System.out.println("SharedMetricRegistires ---- Class not found");
			//e.printStackTrace();
		} catch (NoClassDefFoundError e) {
			Tr.debug(tc, "NoClassDefFoundError - because metrics wasn't dynamically imported");
			System.out.println("SharedMetricRegistires ---- because metrics wasn't dynamically imported");
			//e.printStackTrace();
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

	/*
	 * Calculate the key to put in - unique one for METHOD / RESPONSE / ROUTE
	 * 
	 * Minimal produceable key is : METHOD IF Server encounters error , can create :
	 * METHOD / RESPONSE
	 * 
	 */
	private String resolveStatsKey(HttpStatAttributes httpStatAttributes) {
		
		

		Optional<String> httpRoute = httpStatAttributes.getHttpRoute();
		String requestMethod = httpStatAttributes.getRequestMethod();
		Optional<Integer> responseStatus = httpStatAttributes.getResponseStatus();

		StringBuilder sb = new StringBuilder();
		sb.append("\""); // starting quote
		sb.append("method:" + requestMethod);
		
		
		/*
		 * For the response status and route, we'll put "nothing" (i.e., "")
		 * in place for the Mbean object name 
		 */
		//responseStatus.ifPresent(status -> sb.append(";status:" + status));
		String respStatusString = (responseStatus.isPresent())? responseStatus.get().toString() : ""; 
		sb.append(";status:" + respStatusString);
		
//		httpRoute.ifPresent(route -> {
//			sb.append(";httpRoute:" + route.replace("*", "\\*"));
//		});
		sb.append(";httpRoute:"+httpRoute.orElseGet( () -> "").replace("*", "\\*"));
		

		String errorType = httpStatAttributes.getErrorType().orElse("");
		sb.append(";error:" + errorType);
		

		sb.append("\""); // ending quote
		return sb.toString();
	}
	
	private void saveOldCodeForResolvingHTTPAttributes() {
		
//		Method getRequestMethod = probedHttpDispatcherLinkObj.getClass().getMethod("getRequest", null);
//
//		Object httpRequestObj = getRequestMethod.invoke(probedHttpDispatcherLinkObj, null);


//		try {
//			Field iscField;
//
//			iscField = probedHttpDispatcherLinkObj.getClass().getDeclaredField("isc");
//
//			iscField.setAccessible(true);
//
//			Object httpInboundServiceContextImplObj = iscField.get(probedHttpDispatcherLinkObj);
//
//			// TODO: ^^^^what if "httpInboundServiceContextImplObj" is null?! -- or it blows up due to modifying access
//			// (i.e. the catches below)
//			// Maybe use a method to encapsulate , if failure or null or excepton use
//			// alternative method -> see testJustHttpDispatcherLink()
//
//			HttpInboundServiceContext httpInboundServiceContextImplInstance = (HttpInboundServiceContext) httpInboundServiceContextImplObj;
//			
//			//TODO: vv can this be null? handle that?
//			HttpRequestMessage httpRequestMessage = httpInboundServiceContextImplInstance.getRequest();
//			
//			
//			//DC: did i even need this? could've just used HDL -> getRequest -> getInfo
//			httpStatAttributes.setRequestMethod(httpRequestMessage.getMethod());
//			httpStatAttributes.setScheme(httpRequestMessage.getScheme());
//
//			resolveNetwortProtocolInfo(httpRequestMessage.getVersion(), httpStatAttributes);
//
//			String serverName = null;
//
//			/*
//			 * Calculate the server-name First try to take it from the Host header
//			 * 
//			 * failing that, use InetAddress and get the IP
//			 */
//			HeaderField hostHeaderField = httpRequestMessage.getHeader("Host");
//			String sHostHeader = hostHeaderField.asString();
//			if (sHostHeader.contains(":")) {
//				serverName = sHostHeader.split(":")[0];
//			} else {
//				serverName = sHostHeader;
//			}
//
//			if (serverName == null) {
//				serverName = httpInboundServiceContextImplInstance.getLocalAddr().getHostAddress();
//			}
//
//			//System.out.println("servername " + serverName);
//			httpStatAttributes.setServerName(serverName);
//
//			//System.out.println("port " + hisc.getLocalPort());
//			httpStatAttributes.setServerPort(httpInboundServiceContextImplInstance.getLocalPort());
//
//		} catch (NoSuchFieldException e) { // getDeclaredField() call
//			e.printStackTrace();
//		} catch (SecurityException e) { // getDeclaredField() call
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) { // iscField.get call
//			e.printStackTrace();
//		} catch (IllegalAccessException e) { // iscField.get call
//			e.printStackTrace();
//		}
	}

}
