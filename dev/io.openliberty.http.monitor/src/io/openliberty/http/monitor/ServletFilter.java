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

import javax.servlet.UnavailableException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
//import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class ServletFilter implements Filter {
	
	
	private static final TraceComponent tc = Tr.register(ServletFilter.class);
	
	private static final String REST_HTTP_ROUTE_ATTR = "REST.HTTP.ROUTE";
	
	
    @Override
    public void init(FilterConfig config) {
    	
    }
	
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		
		String appName = servletRequest.getServletContext().getAttribute("com.ibm.websphere.servlet.enterprise.application.name").toString();

		Exception servletException = null;
		Exception exception = null;
		
		String httpRoute;
		String contextPath = servletRequest.getServletContext().getContextPath();
		contextPath = (contextPath == null ) ? null : contextPath.trim();
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
			
			// Wasn't a REST request, time to resolve if possible
			if (httpRoute == null) {
				
				if (servletRequest instanceof SRTServletRequest) {
					//SRTServletRequest allows us to get the WebAppDispatcher
					SRTServletRequest srtServletRequest = (SRTServletRequest) servletRequest;
					
					httpRoute = resolveHttpRoute(srtServletRequest, contextPath, httpStatsAttributesHolder);
				} else {
					//This shouldn't happen.
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
						Tr.debug(tc, String.format("Servlet request is not an instance of SRTServletRequest", servletRequest));
					}
				}
				
			}
			
			httpStatsAttributesHolder.setHttpRoute(httpRoute);//httpRoute

			/*
			 * Pass information onto HttpServerStatsMonitor.
			 */
			HttpServerStatsMonitor httpMetricsMonitor = HttpServerStatsMonitor.getInstance();
			if (httpMetricsMonitor != null) {
				httpMetricsMonitor.updateHttpStatDuration(httpStatsAttributesHolder, Duration.ofNanos(elapsednanos), appName);
			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "Could not acquire instance of HttpServerStatsMonitor. Can not proceed to create/update Mbean.");
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
	 * Remove unnecessary servlet 30 if statement for an undocumented property.
	 * 
	 * @param th The throwable
	 * @return The resolved stattus code
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
	
	private String resolveHttpRoute(SRTServletRequest srtServletRequest, String contextPath, HttpStatAttributes httpStatsAttributesHolder) {
		String httpRoute = null;		
		
		String pathInfo = srtServletRequest.getPathInfo();
		pathInfo = (pathInfo == null ) ? null : pathInfo.trim();
		String servletPath = srtServletRequest.getServletPath();
		servletPath = (servletPath == null ) ? null : servletPath.trim();
		String requestURI = srtServletRequest.getRequestURI();
		requestURI = (requestURI == null ) ? null : requestURI.trim();
	

		/*
		 * WebAppDispatcher used to get a "mapping" value
		 */
		IWebAppDispatcherContext iwadc = srtServletRequest.getWebAppDispatcherContext();
		WebAppDispatcherContext wadc = null;
		if (iwadc instanceof WebAppDispatcherContext) {
			wadc = (WebAppDispatcherContext) iwadc;
		}


		if (servletPath != null && !servletPath.isEmpty()) {
			/*
			 * path Info is null (no extra path info) and servlet path is not null
			 */
			if (pathInfo == null) {
				
				/*
				 * First lets deal with JSPs
				 * There are TWO scenarios:
				 * - Unconfigured JSP
				 * - "Configured" JSP where they configured the path to end with a .jsp (weird!)
				 * They technically could've configured the path of a normal servlet to end with .jsp too (more weird!)
				 * ^ This would typically be handled with "DIRECT MATCH WITH SERVLET PATH", but our JSP
				 * logic checks for things that end with ".jsp" so we'll need to explicitly handle that as well.
				 * 
				 */
				if (servletPath.endsWith(".jsp")) { //only works for EE8 up, unfortunately cannot resolve for EE7 without mapping or pattern information.
					if (wadc != null && wadc.getMappingValue() != null
							&& wadc.getMappingValue().endsWith(".jsp")) {
						/*
						 *  Direct match with a configured JSP that ends with ".jsp" in the path (weird!)
						 *  ^We use the mapping to see that the match is "name.jsp" and not just "name",
						 *  which would be the case if its an unconfigured jsp
						 *  
						 *  examples:
						 *  <url-pattern>/name.jsp</url-pattern>
						 *  <url-pattern>/sub/name.jsp</url-pattern>
						 */
						httpRoute = contextPath + servletPath;
					} else {
						/*
						 * unconfigured JSPs.
						 * File that's just "there" are just treated as /context/*
						 * 
						 * Problem: For JEE7 where weirdly configured resource that ends with *.jsp , because there is NO MAPPING
						 * It will fall in here. Even though it is a direct match.
						 * This is a constraint of JEE7 without mapping or pattern information to obtain.
						 * *** Overall, this is an unlikely scenario *****
						 */
						httpRoute = contextPath + "/*";
					}
				}
				
				else {
					/*
					 * PATH INFO NULL
					 * SERVLET NOT NULL
					 * We are dealing with a direct match.
					 * OR a wild card servlet , but right on the node
					 * ^ Can only resolve for EE8
					 * e.g.
					 * servlet path: /path/*
					 * request path: /path
					 *
					 */

					//Used by EE8 and above
					if (Servlet4Helper.isServlet4Up()) {

						String pattern = Servlet4Helper.getPattern(iwadc);
						//This resolves the /wild/* servlet path and the /wild is entered for EE8
						if (pattern != null && pattern.endsWith("/*")) {
							httpRoute = contextPath + pattern;
							//direct mtach for EE8
						} else {
							httpRoute = contextPath + servletPath;
						}
					}
					//EE7 specifically
					else {
						/*
						 * Simply a direct match.
						 * 
						 * Unfortunately, given a scenario where a servlet is set as /path/*
						 * we cannot properly resolve the http route of hits to /path as /path/*
						 * 
						 * Constraint of EE7 where we have no "pattern" information
						 */
						httpRoute = contextPath + servletPath;
						
					}
				}

			}
			/*
			 *  SERVLETS WITH WILD CARD 
			 */
			else if (pathInfo != null && !pathInfo.isEmpty() ){
				httpRoute = contextPath + servletPath + "/*";			
			} else {
				/*
				 * This block implies pathInfo is not null, but is empty whilst servlet path exists.
				 * We'll just use the servlet path then.
				 */
				httpRoute = contextPath + servletPath;
			}
		} 
		else if (pathInfo != null && servletPath != null && servletPath.isEmpty()) {
			/*
			 * Special case- like /metrics or /health where PathInfo is "/"
			 * but servletPath is empty. Related to how the servlet mapping is configured.
			 * 
			 * Also applies to a default HTML pages.
			 * 
			 * We append the pathInfo (which would be "/") to the context path
			 * This ends up with /metrics/ and /health/
			 * Similarly, when loading default html it's a /contextRoot/
			 */
			if (pathInfo.trim().equals("/")) {
				httpRoute = contextPath + pathInfo;
			} 
			/*
			 * If pathInfo is not just "/" and it isn't empty (whilst servlet path is empty)
			 * We'll just use /*
			 * 
			 * This also applies to if the servlet uses "/*" (i.e., /contxt/* ) for wild carding
			 * */
			else if(!pathInfo.isEmpty()) {
				httpRoute = contextPath  + "/*";
			} else {
				/*
				 * This implies servlet path is empty and pathInfo is empty?!
				 * use context path + "/*"
				 */
				httpRoute = contextPath  + "/*";
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, String.format("Defaulting to \"/*\" for request URI [%s] with the provided pathInfo is [%s] and servlet path [%s]" , requestURI, pathInfo, servletPath));
				}
			}			
		
		} else {
			/*
			 * Can't figure out what it is.
			 * SevletPath is, default to "/*"
			 */
			
			httpRoute = contextPath  + "/*";
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, String.format("Defaulting to \"/*\" for request URI [%s] with the provided pathInfo is [%s] and servlet path [%s]" , requestURI, pathInfo, servletPath));
			}
		}
		return httpRoute;
	}
	
	
    /** {@inheritDoc} */
    @Override
    public void destroy() {
    }
}
