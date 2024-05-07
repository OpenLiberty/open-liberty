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

import java.io.IOException;
import java.time.Duration;

import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
public class ServletFilter implements Filter {
	
	
	
	private void resolveNetworkProtocolInfo(String protocolInfo, HttpStatAttributes httpStat) {
		String[] networkInfo = protocolInfo.trim().split("/");
		String networkProtocolName = null;
		String networkVersion = "";
		if (networkInfo.length == 1) {
			networkProtocolName = networkInfo[0].toLowerCase();
		} else if( networkInfo.length == 2) {
			networkProtocolName = networkInfo[0];
			networkVersion = networkInfo[1];
		} else {
			//there shouldn't be more than two values.
		}
		
		httpStat.setNetworkProtocolName(networkProtocolName);
		httpStat.setNetworkProtocolVersion(networkVersion);
	}
	
	private void resolveRequestAttributes(ServletRequest servletRequest, HttpStatAttributes httpStat) {
		
		// Retrieve the HTTP request attributes
		if (HttpServletRequest.class.isInstance(servletRequest)) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

			httpStat.setRequestMethod(httpServletRequest.getMethod());

			httpStat.setScheme(httpServletRequest.getScheme());

			resolveNetworkProtocolInfo(httpServletRequest.getProtocol(), httpStat);

			httpStat.setServerName(httpServletRequest.getServerName());
			httpStat.setServerPort(httpServletRequest.getServerPort());
		} else {
			// uh oh
		}
	}

	// Only need to get response status
	private void resolveResponseAttributes(ServletResponse servletResponse, HttpStatAttributes httpStat) {

		if (servletResponse instanceof HttpServletResponse) {
			HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
			httpStat.setResponseStatus(httpServletResponse.getStatus());
		} else {
			// uh oh
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		
		System.out.println("ENTERING ZE FILTER");

		String httpRoute;
		String contextPath = servletRequest.getServletContext().getContextPath();
		long nanosStart = System.nanoTime();
		try {
			filterChain.doFilter(servletRequest, servletResponse);
		} catch (IOException ioe) {
			throw ioe;
		} catch (ServletException se) {
			throw se;
//		} catch (Exception e) { //Place Holder - can catch exception thrown by sevlet for error.type
//			System.out.println("something happened here");
		} finally {
			long elapsednanos = System.nanoTime()-nanosStart;
			HttpStatAttributes httpStat = new HttpStatAttributes();

			// Retrieve the HTTP request attributes
			resolveRequestAttributes(servletRequest, httpStat);

			// Retrieve the HTTP response attribute (i.e. the response status)
			resolveResponseAttributes(servletResponse, httpStat);

			// attempt to retrieve the `httpRoute` from the RESTful filter
			httpRoute = (String) servletRequest.getAttribute("RESTFUL.HTTP.ROUTE");
			
			/*
			 * If it does not exist.
			 * Either dealing with:
			 * 1. Non-existent context-root
			 * 2. Non-existent path in a Restful configured application
			 * 3. Servlets
			 */
			if (httpRoute == null) {
				if (servletRequest instanceof SRTServletRequest) {
					SRTServletRequest srtServletRequest = (SRTServletRequest) servletRequest;

					IWebAppDispatcherContext webAppdispatcherContext = srtServletRequest.getWebAppDispatcherContext();

					// Cast to WebAppDispatcherContext40 to obtain HttpServletMapping
					if (webAppdispatcherContext instanceof WebAppDispatcherContext40) {
						WebAppDispatcherContext40 webAppDispatcherContext40 = (WebAppDispatcherContext40) webAppdispatcherContext;
						
						//Can be null for direct match with servlet URL pattern with no wildcard
						String pathInfo = webAppDispatcherContext40.getPathInfo();
						//System.out.println("path Info: " + pathInfo); // Deal with null....
						

						HttpServletMapping httpServletMapping = webAppDispatcherContext40.getServletMapping();
						if (httpServletMapping != null && pathInfo != null) {

							/*
							 * Mapping exists:
							 * 
							 * 1. Could be an archive with JAX-RS/Restful resources in which case a
							 * non-existent URL can still produce a HttpServletMapping with matchValue=""
							 * and Pattern="/" but the _pathInfo of wadc40 would be a non empty path
							 * 
							 * 2. Wild card servlet match
							 */
							String pattern = httpServletMapping.getPattern();
							String matchValue = httpServletMapping.getMatchValue();

							/*
							 * Pattern is "/", match value is empty "" and path info is NOT "/"
							 * 
							 * This occurs if we hit a RESTful/JAX-RS configured application
							 * and a non-existent path was encountered.
							 * 
							 * (i.e. if /RestfulApp/nonExistent
							 * 
							 * This produces a pattern of "/", no match and pathInfo of "/"
							 */
							if (pattern.equals("/") && matchValue.isEmpty() && !pathInfo.equals("/")) {
								httpRoute = contextPath + "/*";
							}
							/*
							 * Pattern is "/*" but matchValue is ""
							 * Instead of just using "/*", we set it as context-path only
							 * 
							 * e.g. Servlet deployed in applcation "MyApp" with pattern /*
							 * and the request is sent to "/MyApp/" or "/MyApp"
							 * We will just produce the httpRoute as "/MyApp"
							 * 
							 * This also applies to the MP Metrics "/metrics" endpoint
							 */
							else if (pattern.equals("/*") && matchValue.isEmpty()) {
								httpRoute = contextPath;
							}
							/*
							 * Special case for MP Health's "/health" pattern is empty and match value is empty, but the pathInfo is "/"
							 * This is different with a direct match with a servlet url pattern where pathInfo is empty
							 *  pattern contains the url pattern and matchValue is not empty (should match the url pattern)
							 * /health
							 */
							else if (pathInfo.equals("/") && pattern.isEmpty() && matchValue.isEmpty()) {
								httpRoute = contextPath;
							}
							/*
							 *  A Match!  -- match for Servlet with a WILD CARD!
							 */
							else if (pattern != null && pattern.endsWith("/*") && !matchValue.isEmpty()) {
								httpRoute = contextPath + pattern;
							} else {
								// unknown scenario
							}
							//System.out.println("Servlet Mapping pattern: " + pattern);
						} else if (pathInfo != null){

							/*
							 * Non Restful bound application
							 * 
							 * 1. Non-existent Path
							 * 2. Welcome page?
							 */

							// Non existent path -> /context-path/*
							if (!pathInfo.equals("/")) {
								httpRoute = contextPath + "/*";
								httpStat.setHttpRoute(httpRoute);
							}
							// Default page of some sort -> /context-path/ or file not found
							else if (pathInfo.equals("/")) {
								httpRoute = contextPath + "/";
								httpStat.setHttpRoute(httpRoute);
								//System.out.println(" DEFAULT PAGE : ");
							} else {
								// something really weird has happened?!
							}
						} else if (httpServletMapping != null && pathInfo == null){ // A SERVLET!!
							String pattern = httpServletMapping.getPattern();
							String matchValue = httpServletMapping.getMatchValue();
							if (pattern != null && !pattern.equals("/") && !matchValue.isEmpty()) {
								httpRoute = contextPath + pattern;
							} else {
								//completely non-existent
								httpRoute = null;
							}
						}

					} // cast to WebAppDispatcherContext40
				} // cast to SRTServletRequest
			} // if httpRoute null

			httpStat.setHttpRoute(httpRoute);
			//System.out.println(httpStat);

			//Transfer information to HttpMetricsMonitor

			HttpStatsMonitor httpMetricsMonitor = HttpStatsMonitor.getInstance();
			if (httpMetricsMonitor != null) {
				httpMetricsMonitor.updateHttpStatDuration(httpStat, Duration.ofNanos(elapsednanos));
			} else {
				//TODO: log error - did not get instance of httpMetricsMonitor
			}

		}

	}

}
