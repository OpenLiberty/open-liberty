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
package com.ibm.ws.webcontainer.osgi.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A RequestInterceptor which will be called during request processing and given an opportunity 
 * to process the request. The point in request processing the interceptor is called depends
 * on a property set by the implementing service:
 * 
 *  InterceptPoint=AfterFilters - the interceptor will be called immediately after app filters have
 *                                been called and immediately before the target of the request. These
 *                                interceptors are called in reverse service ranking order so highest ranked
 *                                is called last.
 *  InterceptPoint=OnFileNotFound - the interceptor will be called if the request for a static file is
 *                                 for a file which does not exist. These interceptors are called in
 *                                 service ranking order so highes ranked is called first. 
 *                                                              
 * Each interceptor must provide an implementation of the handleRequest method.The method should return true 
 * if the request is fully processed and should not continue as normal. The method should 
 * return false if the request is not fully processed and should continue as normal.
 */
public interface RequestInterceptor {
    
    
    public static final String INTERCEPT_POINTS_PROPERTY = "InterceptPoint";
    public static final String INTERCEPT_POINT_AFTER_FILTERS = "AfterFilters";
    public static final String INTERCEPT_POINT_FNF = "OnFileNotFound";

    
    // If the returned value is false the request was not handled and should continue normally.
    boolean handleRequest(HttpServletRequest req, HttpServletResponse resp);

}
