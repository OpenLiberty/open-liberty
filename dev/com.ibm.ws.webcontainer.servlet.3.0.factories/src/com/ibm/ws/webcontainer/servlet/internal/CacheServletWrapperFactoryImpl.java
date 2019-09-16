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
package com.ibm.ws.webcontainer.servlet.internal;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.servlet.CacheServletWrapper;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapperFactory;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

@Component(service=CacheServletWrapperFactory.class, property = { "service.vendor=IBM" })
public class CacheServletWrapperFactoryImpl implements CacheServletWrapperFactory {

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.servlet.CacheServletWrapperFactory#createCacheServlerWrapper(com.ibm.wsspi.webcontainer.servlet.IServletWrapper, javax.servlet.http.HttpServletRequest, java.lang.String, com.ibm.ws.webcontainer.webapp.WebApp)
     */
    @Override
    public CacheServletWrapper createCacheServletWrapper(IServletWrapper wrapper, HttpServletRequest req, String cacheKey, WebApp webapp) {
        return new CacheServletWrapper(wrapper, req, cacheKey, webapp);
    }

}
