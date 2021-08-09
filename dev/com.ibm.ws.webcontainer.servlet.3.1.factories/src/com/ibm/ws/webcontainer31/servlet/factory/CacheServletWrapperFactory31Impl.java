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
package com.ibm.ws.webcontainer31.servlet.factory;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.servlet.CacheServletWrapper;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapperFactory;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 *
 */
@Component(service = CacheServletWrapperFactory.class, property = { "service.vendor=IBM" })
public class CacheServletWrapperFactory31Impl implements CacheServletWrapperFactory {

    /** {@inheritDoc} */
    @Override
    public CacheServletWrapper createCacheServletWrapper(IServletWrapper wrapper, HttpServletRequest req, String cacheKey, WebApp webapp) {
        // Nothing specific for CacheServletWrapper in Servlet 3.1 so just return the base WC CacheServletWrapper
        return new CacheServletWrapper(wrapper, req, cacheKey, webapp);
    }

}
