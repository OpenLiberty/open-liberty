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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;

import jakarta.servlet.UnavailableException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.error.ServletErrorReport;
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
	
	private static final TraceComponent tc = Tr.register(ServletFilter.class);
	
	private static final String REST_HTTP_ROUTE_ATTR = "REST.HTTP.ROUTE";
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {

		String appName = servletRequest.getServletContext().getAttribute("com.ibm.websphere.servlet.enterprise.application.name").toString();

		Exception servletException = null;
		Exception exception = null;
		
		String httpRoute;
		String contextPath = servletRequest.getServletContext().getContextPath();
		long nanosStart = System.nanoTime();
		try {
			filterChain.doFilter(servletRequest, servletResponse);
		} catch (IOException ioe) {
			throw ioe;
		} catch (ServletException se) {
			servletException = se;
			throw se;
		} catch (Exception e) {
			exception = e;
			throw e;
		} finally {
			long elapsednanos = System.nanoTime()-nanosStart;
			
			//holder for http attributes
			HttpStatAttributes httpStatsAttributesHolder = new HttpStatAttributes();

			// Retrieve the HTTP request attributes
			resolveRequestAttributes(servletRequest, httpStatsAttributesHolder);

			/*
			 *  Retrieve the HTTP response attribute (i.e. the response status)
			 *  If servlet exception occurs, we can get the ServletErrorReport
			 *  and retrieve the error.
			 *  
			 *  If it isn't, then we'll set it to a 500.
			 */
			if (servletException != null ) {
				if (servletException instanceof ServletErrorReport) {
					ServletErrorReport ser = (ServletErrorReport) servletException;
					httpStatsAttributesHolder.setResponseStatus(ser.getErrorCode());
				} else {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
						Tr.debug(tc, String.format("Servlet Exception occured, but could not obtain a ServletErrorReport. Default to a 500 response. The exception [%s].",servletException));
					}
					httpStatsAttributesHolder.setResponseStatus(500);
				}
				
			} else if (exception != null) {
				httpStatsAttributesHolder.setResponseStatus(resolveStatusForException(exception));
			} else {
				resolveResponseAttributes(servletResponse, httpStatsAttributesHolder);
			}

			// attempt to retrieve the `httpRoute` from the RESTful filter
			httpRoute = (String) servletRequest.getAttribute(REST_HTTP_ROUTE_ATTR);
			
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
						//OR 
						String pathInfo = webAppDispatcherContext40.getPathInfo();

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
								if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
									Tr.debug(tc, String.format("Can not resolve HTTP route. Pattern:[%s] matchValue:[%s] pathInfo:[%s] ", pattern,matchValue,pathInfo));
								}
							}
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
								httpStatsAttributesHolder.setHttpRoute(httpRoute);
							}
							// Default page of some sort -> /context-path/ or file not found
							else if (pathInfo.equals("/")) {
								httpRoute = contextPath + "/";
								httpStatsAttributesHolder.setHttpRoute(httpRoute);
							} else {
								if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
									Tr.debug(tc, String.format("Can not resolve HTTP route.  pathInfo:[%s] ",pathInfo));
								}
							}
						} else if (httpServletMapping != null && pathInfo == null){ // A SERVLET!!
							String pattern = httpServletMapping.getPattern();
							String matchValue = httpServletMapping.getMatchValue();
							if (pattern != null && !pattern.equals("/") && !matchValue.isEmpty()) {
								
								pattern = (pattern.startsWith("/")) ? pattern : "/" + pattern;
								httpRoute = contextPath + pattern;
							} else {
								if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
									Tr.debug(tc, String.format("Can not resolve HTTP route.  matchValue:[%s] pathInfo:[%s] Pattern:[%s]", matchValue,pathInfo, pattern));
								}
								httpRoute = null;
							}
						} else if ((pathInfo == null) && 
								(webAppDispatcherContext40.getServletPathForMapping() != null) 
								&& (!webAppDispatcherContext40.getServletPathForMapping().isEmpty())){
							
							/*
							 * PathInfo is null, httpServlet Mapping is null.
							 * If we can get a servletPathForMapping value.
							 * We'll use that.
							 * 
							 * Example: JSP that wasn't configured in web.xml
							 */
							String path = webAppDispatcherContext40.getServletPathForMapping();
							path = (path.startsWith("/")) ? path : "/" + path ;
							httpRoute = contextPath + webAppDispatcherContext40.getServletPathForMapping();
							
						}

					} // cast to WebAppDispatcherContext40
				} // cast to SRTServletRequest
			} // if httpRoute null

			httpStatsAttributesHolder.setHttpRoute(httpRoute);

			/*
			 * Pass information onto HttpStatsMonitor.
			 */
			HttpStatsMonitor httpMetricsMonitor = HttpStatsMonitor.getInstance();
			if (httpMetricsMonitor != null) {
				httpMetricsMonitor.updateHttpStatDuration(httpStatsAttributesHolder, Duration.ofNanos(elapsednanos), appName);
			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Could not acquire instance of HTTpStatsMonitor. Can not proceed to create/update Mbean.");
				}
			}

		}

	}
	
	/**
	 * Resolve HTTP attributes related to request
	 * @param servletRequest
	 * @param httpStat
	 */
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
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, String.format("Expected an HttpServletRequest, instead got [%s].",servletRequest.getClass().toString()));
			}
		}
	}

	/**
	 * Resolve HTTP attributes related to response
	 * 
	 * @param servletResponse
	 * @param httpStat
	 */
	private void resolveResponseAttributes(ServletResponse servletResponse, HttpStatAttributes httpStat) {

		if (servletResponse instanceof HttpServletResponse) {
			HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
			httpStat.setResponseStatus(httpServletResponse.getStatus());
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, String.format("Expected an HttpServletResponse, instead got [%s].",servletResponse.getClass().toString()));
			}
		}
	}
	
	/**
	 * Resolve Network Protocol Info  - move to common utility package
	 * @param protocolInfo
	 * @param httpStat
	 */
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
	
	/**
	 * Taken from WebApprrorReport.constructErrorReport()
	 * Trimmed to remove unecessary servlet 30 if statement.
	 * 
	 * @param th
	 * @return
	 */
	private int resolveStatusForException(Throwable th) {
        Throwable rootCause = th;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        
        int status;
        
        if (rootCause instanceof FileNotFoundException) {
        	status = HttpServletResponse.SC_NOT_FOUND;
        }
        else if (rootCause instanceof UnavailableException) {
            UnavailableException ue = (UnavailableException) rootCause;
            if (ue.isPermanent()) {
            	status = HttpServletResponse.SC_NOT_FOUND;
            } else {
            	status = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            }
        }
        else if (rootCause instanceof IOException
                        && th.getMessage() != null
                        && th.getMessage().contains("CWWWC0005I")) {     //Servlet 6.0 added
        	status = HttpServletResponse.SC_BAD_REQUEST;
        }
        else {
        	status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        
        return status;


	}

}
