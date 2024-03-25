/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet.internal.servlet.factory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.webcontainer.servlet.CacheServletWrapper;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapperFactory;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer40.servlet.CacheServletWrapper40;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

import jakarta.servlet.http.HttpServletRequest;

@Component(service = CacheServletWrapperFactory.class, property = { "service.vendor=IBM" })
public class CacheServletWrapperFactory61Impl implements CacheServletWrapperFactory {

    @Override
    public CacheServletWrapper createCacheServletWrapper(IServletWrapper wrapper, HttpServletRequest req,
                                                         String cacheKey, WebApp webapp) {

        return new CacheServletWrapper40(wrapper, req, cacheKey, webapp);
    }

}
