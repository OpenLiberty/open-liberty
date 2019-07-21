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
package com.ibm.ws.jbatch.rest.internal.resources;

import java.net.HttpURLConnection;
import java.util.List;

import com.ibm.ws.jbatch.rest.internal.BatchRequestUtil;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Abstract base class that all RequestHandlers inherit from.
 * 
 * Subclasses must call the RequestHandler(path) CTOR, passing in the request
 * path they can handle.
 * 
 * The request path may contain wildcards to match whole path segments.
 * E.g. "/batch/jobinstances/*"
 * 
 * Note however you can't partially match a segment; e.g this won't work:
 * "/batch/job*"
 * 
 * Provides a canHandle() request that will match the current request's path
 * against the handler's path.
 * 
 * Provides default implementations for all the request methods (get/post/put/etc),
 * all of which send an HTTP_BAD_REQUEST error response.
 */
public abstract class RequestHandler {
	
	private static final TraceComponent tc = Tr.register(RequestHandler.class, "wsbatch", "com.ibm.ws.jbatch.rest.resources.RESTMessages");
	
	private long lastOptionsMsg = 0;
    
    /**
     * The request path for this handler.
     */
    private List<String> handlerPath ;
    
    /**
     * If using the default CTOR then the user MUST use setPath() to set the  
     * request path for this handler.
     */
    protected RequestHandler() {
    }
    
    /**
     * CTOR.
     * 
     * @param path The request path for this handler.
     */
    protected RequestHandler(String path) {
        setPath(path);
    }
    
    /**
     * Fluent API for setting the handler path.
     * 
     * @param path The request path for this handler.
     * 
     * @return this.
     */
    public RequestHandler setPath(String path) {
        handlerPath = BatchRequestUtil.splitPath(path);
        return this;
    }
    
    /**
     * @return true if this handler can handle the given request (based on the request path).
     */
    public boolean canHandle(RESTRequest request) {
        return canHandle( BatchRequestUtil.splitPath(request.getPath()) );
    }
    
    /**
     * @return true if this handler can handle the given request path.
     */
    public boolean canHandle(List<String> requestPath) {
        if (requestPath.size() != handlerPath.size()) {
            return false;
        }
        
        for (int i = 0; i < requestPath.size(); ++i) {
            if (requestPath.get(i).equals(handlerPath.get(i)) || handlerPath.get(i).equals("*")) {
                // matches... keep going
            } else {
                return false;
            }
        }
            
        return true;
    }
    
    public void get(RESTRequest request, RESTResponse response) throws Exception {
        response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "GET request not supported");
    }
    public void put(RESTRequest request, RESTResponse response) throws Exception {
        response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "PUT request not supported");
    }
    public void post(RESTRequest request, RESTResponse response) throws Exception {
        response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "POST request not supported");
    }
    public void delete(RESTRequest request, RESTResponse response) throws Exception {
        response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "DELETE request not supported");
    }
    public void options(RESTRequest request, RESTResponse response) throws Exception {
    	if ((System.currentTimeMillis() - lastOptionsMsg) > 60 * 1000) {
    		Tr.info(tc, "http.options.received", request.getRemoteHost() + ":" + request.getRemotePort());
    		lastOptionsMsg = System.currentTimeMillis();
    	}
        response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "OPTIONS request not supported");
    }
    public void trace(RESTRequest request, RESTResponse response) throws Exception {
        response.sendError(HttpURLConnection.HTTP_BAD_REQUEST, "TRACE request not supported");
    }
    
    /**
     * @return default Stringified representation of the handler
     */
    public String toString() {
        return super.toString() + ":path=" + String.valueOf(handlerPath);
    }
}