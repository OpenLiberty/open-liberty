/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.ws.jbatch.utility.utils.StringUtils;

/**
 * Very simple http client featuring a fluent API.
 * 
 * Example usage: send a "GET" request to "http://myhost:8080/my/uri/path" and return
 * the response as a String.
 * 
 *  String response = new SimpleHttpClient()
 *                                   .setTarget( "http://myhost:8080" )
 *                                   .path("my/uri")
 *                                   .path("path")
 *                                   .header( "Accept", "text/plain" )
 *                                   .get()
 *                                   .readEntity( new StringEntityReader() );
 *
 */
public class SimpleHttpClient {
	
	protected static final String batchRestContext = "/ibm/api/batch";

    /**
     * The URL path.
     */
    private String path = "";
    
    /**
     * The URL target (<protocol>://<host>:<port>)
     */
    private String target;

    /**
     * Map of HTTP headers.
     */
    private Map<String, String> headers = new HashMap<String, String>();

    /**
     * Map of query params.
     */
    private Map<String, String> queryParams = new HashMap<String, String>();
    
    /**
     * The connect and read timeout. The same timeout value is applied to both
     * the connect and read operations. 
     */
    private int timeout_ms = 0;
    
    /**
     * Set the target (<protocol>://<host>:<port>)
     */
    public SimpleHttpClient setTarget(String target) {
        this.target = target;
        return this;
    }

    /**
     * @return the target (<protocol>://<host>:<port>)
     */
    protected String getTarget() {
        return target;
    }

    /**
     * @param the timeout value, in ms, to be applied to both the connect and read operations.
     * 
     * @return this
     */
    public SimpleHttpClient setTimeout(int timeout_ms) {
        this.timeout_ms = timeout_ms;
        return this;
    }
    
    /**
     * Append the given appendPath to the path.
     *
     * Note: a "/" is automatically inserted in front of appendPath.
     */
    public SimpleHttpClient path(String appendPath) {
        path += "/" + appendPath;
        return this;
    }

    /**
     * @return the constructed path
     */
    protected String getPath() {
        return path;
    }

    /**
     * @return the query string (including the leading "?"), or "" if none.
     */
    protected String getQueryString() {
        
        StringBuilder retMe = new StringBuilder();
        String delim = "";

        for (Map.Entry<String, String> queryParam : getQueryParams().entrySet()) {
            retMe.append(delim).append(queryParam.getKey() + "=" + queryParam.getValue());
            delim = "&";
        }

        return (retMe.length() == 0) ? "" : "?" + retMe.toString();
    }

    /**
     * @return the target URL
     */
    protected URL getURL() throws IOException {
        return new URL( getTarget() + getPath() + getQueryString() );  
    }

    /**
     * Set the given header.
     *
     * @return this.
     */
    public SimpleHttpClient header(String key, String value) {
        if ( key != null && value != null ) {
            headers.put(key, value);
        }
        return this;
    }

    /**
     * @return the header map
     */
    protected Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * TODO: handle multiple values.
     */
    public SimpleHttpClient queryParam(String key, String value) {
        queryParams.put(key, value);
      
        return this;
    }
    
    /**
     * Only inserts the parameter if it is non-null and not empty
     * @param key
     * @param value
     * @return
     */
    public SimpleHttpClient queryParamNotNullOrEmpty(String key, String value) {
        if(value != null && value.trim().length() > 0) {
            queryParams.put(key, value);
        }
        
        return this;
    }

    /**
     * @return the queryParams map.
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    // TODO: pathParam
    // TODO: close the httpurlcon, inputstreams and whatnot?
    

    /**
     * Execute a GET request.
     *
     * @return a Response object for reading the response
     */
    public Response get() throws IOException {
        HttpURLConnection con = setHeaders( getConnection("GET") );
        con.setInstanceFollowRedirects(false);
        con.connect();

        if(con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
        	con = processRedirect(con);
        }
        
        return new Response(con);
    }

    /**
     * Execute a POST request.
     *
     * @return a Response object for reading the response
     * @throws IOException 
     */
    public Response post( EntityWriter entityWriter ) throws IOException {
        
        return withPayload("POST", entityWriter);
    }
    
    /**
     * Execute a PUT request.
     *
     * @return a Response object for reading the response
     * @throws IOException 
     */
    public Response put( EntityWriter entityWriter ) throws IOException {
        
        return withPayload("PUT", entityWriter);
    }
    
    /**
     * Execute a DELETE request.
     *
     * @return a Response object for reading the response
     */
    public Response delete() throws IOException {
        HttpURLConnection con = setHeaders( getConnection("DELETE") );
        con.setInstanceFollowRedirects(false);
        con.connect();
        
        if(con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
        	con = processRedirect(con);
        }
        
        return new Response(con);
    }
    
    /**
     * Both PUTs and POSTs route to here.
     * 
     * @return a Response object for reading the response
     * 
     * @throws IOException 
     */
    protected Response withPayload(String requestMethod, EntityWriter entityWriter) throws IOException {
    
        HttpURLConnection con = setHeaders( getConnection(requestMethod) );
        if (entityWriter != null) {
            entityWriter.writeEntity( con.getOutputStream() );
        }
        con.setInstanceFollowRedirects(false);
        con.connect();
        
        if(con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
        	con = processRedirect(con);
        }
        
        return new Response(con);
    }

    /**
     * @return the given con, with all headers set.
     */
    protected HttpURLConnection setHeaders(HttpURLConnection con) {

        for (Map.Entry<String, String> header : getHeaders().entrySet()) {
            con.setRequestProperty( header.getKey(), header.getValue() );
        }

        return con;
    }

    /**
     * @return an HttpUrlConnection using the given requestMethod (e.g. "GET", "POST", etc).
     */
    protected HttpURLConnection getConnection(String requestMethod ) throws IOException {
        HttpURLConnection con = (HttpURLConnection) getURL().openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);  // should i bother setting this to false for GETs?
        con.setUseCaches(false);
        con.setRequestMethod( requestMethod );
        
        if (Boolean.getBoolean("com.ibm.ws.jbatch.utility.https.disableHostnameVerification")) {
            disableHostnameVerification((HttpsURLConnection) con);
        }
        
        // If -Djavax.net.ssl.keyStore is specified, then we assume the user wants to use
        // certificate-based authentication.
        if ( !StringUtils.isEmpty(System.getProperty("javax.net.ssl.keyStore")) && con instanceof HttpsURLConnection) {
            ((HttpsURLConnection)con).setSSLSocketFactory( (SSLSocketFactory) SSLSocketFactory.getDefault() );
        }
        
        con.setConnectTimeout(timeout_ms);
        con.setReadTimeout(timeout_ms);
        
        return con;
    }
    
    /**
     * @return con, with hostname verification disabled.
     */
    protected HttpsURLConnection disableHostnameVerification(HttpsURLConnection con) {
        con.setHostnameVerifier(HttpUtils.getTrustAllHostnames());
        return con;
    }
    
    /**
     * Process an HTTP 302 redirect by opening a new connection to the indicated host.
     * 
     * @param con
     * @return
     * @throws IOException
     */
    private HttpURLConnection processRedirect(HttpURLConnection con) throws IOException {    	
    	String requestMethod = con.getRequestMethod();
    	
    	// Our "target" is just the host/port, but the Location field includes the full path, so we have to trim it down.
    	String target = con.getHeaderField("Location");
    	int index = target.indexOf(batchRestContext);
    	target = target.substring(0, index);
    	this.setTarget(target);
    	
    	con = setHeaders( getConnection(requestMethod) );
        con.setInstanceFollowRedirects(false);
        con.connect();
        
        return con;
    }

}
