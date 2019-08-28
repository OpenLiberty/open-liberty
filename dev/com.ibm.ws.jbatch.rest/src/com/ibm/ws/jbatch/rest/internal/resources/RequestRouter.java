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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.jbatch.rest.internal.BatchRequestUtil;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * Routes REST requests to the appropriate handler and method.
 * 
 * The appropriate handler is chosen by scanning the list and selecting
 * the first one whose canHandle() method returns true.
 * 
 * The handler's get/post/put/delete/trace/options method is called based
 * on the HTTP request method.
 */
public class RequestRouter {
    
    /**
     * Registered request handlers.
     */
    List<RequestHandler> requestHandlers = new ArrayList<RequestHandler>();
    
    /**
     * Register a RequestHandler.
     */
    public RequestRouter addHandler(RequestHandler requestHandler) {
        requestHandlers.add(requestHandler);
        return this;
    }
 
    /**
     * @return the first handler in the list whose canHandle() method returns true.
     * 
     * @throws IOException if no handler is found.
     */
    protected RequestHandler getHandler(RESTRequest request) throws IOException {
        List<String> requestPath = BatchRequestUtil.splitPath(request.getPath()) ;
        
        for (RequestHandler requestHandler : requestHandlers) {
            if (requestHandler.canHandle(requestPath)) {
                return requestHandler;
            }
        }
        throw new IOException("No handler for request " + request.getPath());
    }
        
    /**
     * Route the request to the appropriate handler.
     * 
     * If this is a "GET" request, handler.get() is called; 
     * if it's a "POST", handler.post(), and so on.
     */
    public void routeRequest(RESTRequest request, RESTResponse response) throws Exception {

         try {
             routeRequestInternal(request, response, getHandler(request));
         } catch (RequestException re) {
             response.sendError(re.getHttpResponseCode(), re.getMessage());
         }
    }

    /**
     * Called by routeRequest.
     */
    protected void routeRequestInternal(RESTRequest request, 
                                        RESTResponse response,
                                        RequestHandler handler) throws Exception {
        
        if ("GET".equals(request.getMethod())) {
            handler.get(request, response);

        } else if ("POST".equals(request.getMethod())) {
            handler.post(request, response);

        } else if ("PUT".equals(request.getMethod())) {
            handler.put(request, response);

        } else if ("DELETE".equals(request.getMethod())) {
            handler.delete(request, response);
            
        } else if ("OPTIONS".equals(request.getMethod())) {
            handler.options(request, response);

        } else if ("TRACE".equals(request.getMethod())) {
            handler.trace(request, response);
            
        } else {
            throw new IOException("Unrecognized HTTP request method: " + request.getMethod() );
        }
    }
}
