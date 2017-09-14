/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.servlet.factory;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.servlet.CacheServletWrapper;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapperFactory;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer40.servlet.CacheServletWrapper40;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

@Component(service = CacheServletWrapperFactory.class, property = { "service.vendor=IBM" })
public class CacheServletWrapperFactory40Impl implements CacheServletWrapperFactory {

    @Override
    public CacheServletWrapper createCacheServletWrapper(IServletWrapper wrapper, HttpServletRequest req,
                                                         String cacheKey, WebApp webapp) {

        return new CacheServletWrapper40(wrapper, req, cacheKey, webapp);
    }

}
