/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
