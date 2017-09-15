/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

/**
 * Interface to check if the request is a WebSocket request or not. Implementation of this interface is done by WebSocket component. 
 * This will invoked by WebContainer after all the filters are invoked and before invoking the servlet. 
 */
public interface WsocHandler {
    /*
     * Indicates whether this is a WebSocket request or not. Based on this WebContainer will call WebSocket servlet or vanilla html/servlet 
     * which are registered with the same URI.
     * 
     * @param ServletRequest  
     * @return true indicates that the request is a WebSocket request. False indicates that the request is not a WebSocket request
     */
    public boolean isWsocRequest(ServletRequest request) throws ServletException;
    
    public void handleRequest(HttpServletRequest request, HttpServletResponse response);
}
