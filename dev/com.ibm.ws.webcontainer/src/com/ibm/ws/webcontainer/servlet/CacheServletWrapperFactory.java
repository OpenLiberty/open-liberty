/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 *
 */
public interface CacheServletWrapperFactory {
    
    CacheServletWrapper createCacheServletWrapper(IServletWrapper wrapper, 
                                                  HttpServletRequest req, 
                                                  String cacheKey, 
                                                  WebApp webapp);

}
