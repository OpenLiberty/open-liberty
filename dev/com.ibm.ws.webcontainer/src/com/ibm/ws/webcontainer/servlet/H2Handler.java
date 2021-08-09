/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.wsspi.http.HttpInboundConnection;

/**
 * Interface to check if the request is an HTTP2 request, and to upgrade the request.
 * This will invoked by WebContainer after all the filters are invoked and before invoking the servlet. 
 */
public interface H2Handler {
    /*
     * @param ServletRequest  
     * @return true indicates that the request is an http2 request. False indicates that the request is not an http2 request
     */
    
    /**
     * @param hic
     * @param request
     * @return true indicates that the request is an http2 request. False indicates that the request is not an http2 request
     */
    public boolean isH2Request(HttpInboundConnection hic, ServletRequest request) throws ServletException;
    
    /**
     * @param hic
     * @param request
     */
    public void handleRequest(HttpInboundConnection hic, HttpServletRequest request, HttpServletResponse response);
}
