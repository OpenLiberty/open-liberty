/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.internal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.batch.runtime.JobExecution;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import org.apache.commons.io.IOUtils;

import com.ibm.jbatch.container.ws.WSJobExecution;
import com.ibm.ws.jbatch.rest.utils.StringUtils;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;

public class BatchRequestUtil {
	
	private static final TraceComponent tc = Tr.register(BatchRequestUtil.class, "wsbatch", "com.ibm.ws.jbatch.rest.resources.RESTMessages");
	
	public static final String NEWLINE = "%n"; //Platform independent new line character
	
	// Regular expression for matching on /ibm/api/batch/<version> in the incoming
	// request URL.
	//
	// <version> is null, or v1-v4.  This string will change if
	// new versions are added.
	public static final String REST_URL_REGEX = "(.*\\/ibm\\/api\\/batch\\/(v[1-4]\\/)?)";
	
	private static boolean isSSLAvailable = true;

    /**
     * Strips trailing slashes and removes redundant slashes.
     * 
     * @param path
     * @return
     */
    public static String normalizeURLPath(String path) {
        path = path.replaceAll("//*", "/");
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
    
	/**
	 * Extract the api version from the REST url
	 * 
	 * @param url
	 * @return The version from the batch rest url (v1, v2 or v2) or null if there is no version
	 */
	public static String getUrlVersion(String url) {
	    
	    if (url == null)
	    	return "";
		
	    Matcher matcher = matchPattern(url);
	    matcher.find();
		
	    if (matcher.group(2) == null) {
		return "";
	    } else {
		return matcher.group(2);
	    }
	}
    
    /**
	 * Return the batch url root without the rest of the resources in the path.
	 * 
	 * @param request
	 * @return Will always return something like:
	 * 
	 * http://<domain>/ibm/api/batch/
	 * 
	 * or
	 * 
	 * http://<domain>/ibm/api/batch/v1
	 */
	public static String getUrlRoot(RESTRequest request) {
	    Matcher matcher = matchPattern(request.getURL());
	    matcher.find();
	    return matcher.group(1);
	}
	
	// Utility method for matching rest url pattern
	private static Matcher matchPattern(String url) {
		Pattern pattern = Pattern.compile(REST_URL_REGEX);
		return pattern.matcher(url);
	}
	
    /**
     * @return the content-length header value for the given request.
     */
    public static int getContentLength(RESTRequest request) {
        String contentLengthString = request.getHeader("Content-Length");
        return (StringUtils.isEmpty(contentLengthString)) ? 0 : Integer.parseInt(contentLengthString);
    }
	
	/**
	 * @return the path as a List<String>, split on "/"
	 * 
	 * NOTE: don't use String.split("/") - it's very expensive (it compiles
	 *       the split char "/" as a regex every time it's called).
	 */
    public static List<String> splitPath(String path) {
        path = BatchRequestUtil.normalizeURLPath(path);
        
        List<String> retMe = new ArrayList<String>();
        
        int idx = -1;
        while ( (idx = path.indexOf("/")) >= 0 ) {
            if (idx > 0) {
                // only include non-empty segments
                retMe.add( path.substring(0, idx) );     
            }
            path = path.substring(idx+1);    // Move past the "/" and continue parsing
        }
        
        // Add the final segment (if non-empty)
        if (path.length() > 0) {
            retMe.add(path);
        }
        
        return retMe;
    }
    
    /**
     * Send an HTTP-302 redirect response with the given redirectUrl
     */
    public static void sendRedirect(RESTResponse response, String redirectUrl) {
        response.setResponseHeader("Location", redirectUrl);
        response.setStatus(HttpURLConnection.HTTP_MOVED_TEMP);
    }
    
    /**
     * @return {batchRestUrlRoot}/jobexecutions/{executionId}?action=stop
     */
    public static String buildStopUrl(String batchRestUrlRoot, long executionId) {
        return StringUtils.trimSuffix(batchRestUrlRoot, "/") 
                    + "/jobexecutions/" 
                    + executionId 
                    + "?action=stop";
    }
    
    /**
     * Convenience method - calls buildStopUrl(WSJobExecution) if jobExecution instanceof WSJobExecution.
     * Otherwise uses the localUrlRoot to build the url.
     */
    public static String buildStopUrl(JobExecution jobExecution, String localUrlRoot) {
    	
        return (jobExecution instanceof WSJobExecution)
        			? buildStopUrl(StringUtils.trimSuffix(((WSJobExecution) jobExecution).getRestUrl(), "/") + "/" + BatchRequestUtil.getUrlVersion(localUrlRoot), jobExecution.getExecutionId())
                    : buildStopUrl( localUrlRoot, jobExecution.getExecutionId()) ;
    }
    
    /**
     * @return {jobExecution.restUrl}/jobexecutions/{executionId}/joblogs
     */
    public static String buildJoblogsUrl(String batchRestUrlRoot, long executionId) {
        return StringUtils.trimSuffix(batchRestUrlRoot, "/") 
                    + "/jobexecutions/" 
                    + executionId
                    + "/joblogs";
    }
    
    /**
     * Convenience method - calls buildJoblogsUrl(WSJobExecution) if jobExecution instanceof WSJobExecution.
     * Otherwise uses the localUrlRoot to build the url.
     */
    public static String buildJoblogsUrl(JobExecution jobExecution, String localUrlRoot) {
        return (jobExecution instanceof WSJobExecution)
        		 ? buildJoblogsUrl( StringUtils.trimSuffix(((WSJobExecution) jobExecution).getRestUrl(), "/") + "/" + BatchRequestUtil.getUrlVersion(localUrlRoot), jobExecution.getExecutionId())
                : buildJoblogsUrl( localUrlRoot, jobExecution.getExecutionId()) ;
    }
    
    /**
     * @return a joblogs url for the given jobexecution, including the query parms.
     */
    public static String buildJoblogsUrl(JobExecution jobExecution,
                                         String localUrlRoot, 
                                         String queryString) {
        return buildJoblogsUrl(jobExecution, localUrlRoot) 
                + (StringUtils.isEmpty(queryString) ? "" : "?" + queryString);
    }
    
    /**
     * @return a query string made up of the parameters in the given map.
     *         The returned query string begins with "?".
     *         If no parameters are in the map, then "" is returned (no "?").
     */
    public static String buildQueryString(Map<String, String[]> parameterMap) {
        if (parameterMap == null) {
            return "";
        }
        
        List<String> parms = new ArrayList<String>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            for (String val : entry.getValue()) {
                parms.add(entry.getKey() + "=" + val);
            }
        }
        return ( parms.isEmpty() ) ? "" : "?" + StringUtils.join(parms, "&");
    }

    /**
     * @return a joblogs url for the given jobinstanceid, including the query parms.
     */
    public static String buildJoblogsUrlForJobInstance(long jobInstanceId,
                                                       String restUrl, 
                                                       String queryString) {
        
        return StringUtils.trimSuffix(restUrl, "/") 
                + "/jobinstances/" 
                + jobInstanceId
                + "/joblogs"
                + (StringUtils.isEmpty(queryString) ? "" : "?" + queryString);
    }
    
    /**
     * @return a stop url for the given jobinstanceid
     */
    public static String buildStopUrlForJobInstance(long jobInstanceId, String restUrl) {
    	return StringUtils.trimSuffix(restUrl, "/")
    			+ "/jobinstances/"
    			+ jobInstanceId
    			+ "?action=stop";
    }
    
    /**
     * @return a job instance url for the given jobinstanceid
     */
    public static String buildPurgeUrlForJobInstance(long jobInstanceId,
                                                       String restUrl) {
        
        return StringUtils.trimSuffix(restUrl, "/") 
                + "/jobinstances/" 
                + jobInstanceId;
                
    }
    
    public static HttpsURLConnection getConnection(URL url) {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            JSSEHelper jsseHelper = JSSEHelper.getInstance();
            connection.setSSLSocketFactory(jsseHelper.getSSLSocketFactory(null, null, null));
            
            return connection;
            
        } catch (MalformedURLException mfe) { //FFDC
        } catch (SSLException e) { //FFDC
        } catch (IOException e1) { //FFDC
        } 
        return null;
    }
    
    /**
     * Have a request be processed at a non-local endpoint via a new HTTP request or HTTP redirect
     *  
     * @param restUrl
     * @param requestMethod
     * @param originalContentType
     * @param authHeader
     */
    public static void handleNonLocalRequest(String restUrl, String requestMethod, 
    		RESTRequest request, RESTResponse response) throws ProtocolException, MalformedURLException, IOException {

    	// If the SSL connection is available, use it unless the request says otherwise. 
    	if (isSSLAvailable && !"true".equalsIgnoreCase(request.getParameter("permitRedirect"))) {
    		sendRESTRequest(restUrl, requestMethod, request, response);
    	}

    	// If SSL is not available, or the request specifies redirect, do a redirect.
    	// Note that this is not an if-else block because if the initial attempt to use SSL fails, we want to fall back to this path.
    	if (!isSSLAvailable || "true".equalsIgnoreCase(request.getParameter("permitRedirect"))) {    		
    		sendRedirect(response, restUrl);
    	}
    }
    
    /**
     * Create a new connection to the specified REST URL
     *  
     * @param restUrl
     * @param requestMethod
     * @param originalContentType
     * @param authHeader
     * @return
     * @throws ProtocolException
     * @throws MalformedURLException
     */
    public static HttpsURLConnection sendRESTRequest(String restUrl, String requestMethod, 
    		RESTRequest request, RESTResponse response) throws ProtocolException, MalformedURLException, IOException {
    	
    	if (isSSLAvailable) {
    		URL url = new URL(restUrl);
    		HttpsURLConnection connection = BatchRequestUtil.getConnection(url);
    		connection.setRequestMethod(requestMethod);
    		connection.setDoInput(true);
    		connection.setUseCaches(false);
    		if (request.getHeader("Authorization") != null)
    			connection.setRequestProperty("Authorization", request.getHeader("Authorization").trim());
    		if (request.getHeader("Content-Type") != null)
    			connection.setRequestProperty("Content-Type", request.getHeader("Content-Type").trim());
    		if (request.getHeader("Cookie") != null)
    			connection.setRequestProperty("Cookie", request.getHeader("Cookie").trim());

    		try {
    			int rc = connection.getResponseCode();
    			if (response != null) { // aka if this is a single-endpoint request
    				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
    					// Note: headers must be set *before* writing to the output stream
    					if ("zip".equals(request.getParameter("type"))) {
    						response.setContentType("application/zip");
    						response.setResponseHeader("Content-Disposition", connection.getHeaderField("Content-Disposition"));
    					} else if ("text".equals(request.getParameter("type"))) {
    						response.setContentType("text/plain; charset=UTF-8");
    					}

    					IOUtils.copy(connection.getInputStream(), response.getOutputStream());
    				} else {
    					String errorMsg = IOUtils.toString(connection.getInputStream());

    					response.sendError(HttpURLConnection.HTTP_INTERNAL_ERROR, 
    							"An internal request to " + connection.getURL() + " failed. The response code was " + rc + " " + connection.getResponseMessage() +
    							" and the error message was " + errorMsg);

    				}
    			}
    		} catch (SSLHandshakeException sslEx) {
    			// Log that SSL failed, and set the flag so we don't try it again.
    			Tr.info(tc, "ssl.connection.unavailable", connection.getURL().getHost());
    			isSSLAvailable = false;
    		}

    		return connection;

    	} else {
    		Tr.debug(tc, "A request to " + restUrl + " could not be completed. " +
    				"An SSL connection to the endpoint was not available.");
    		return null;
    	}
    }

    // Getter for isSSLAvailable
    public static boolean getSSLAvailable() {
    	return isSSLAvailable;
    }
}
