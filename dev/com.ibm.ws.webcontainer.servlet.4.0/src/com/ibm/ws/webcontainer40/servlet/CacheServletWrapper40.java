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
package com.ibm.ws.webcontainer40.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.webcontainer.servlet.CacheServletWrapper;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.ws.webcontainer40.srt.SRTServletRequest40;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * A Servlet 4.0 specific CacheServletWrapper implementation that supports the Servle 4.0
 * ServletMapping API.
 */
public class CacheServletWrapper40 extends CacheServletWrapper {

    protected final static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer40.servlet");
    private static final String CLASS_NAME = CacheServletWrapper40.class.getName();

    private final HttpServletMapping mapping;

    /**
     * The CacheServletWrapper40 constructor will call the super class constructor and then
     * set the ServletMapping equal to the value returned from the HttpServletRequest getMapping method.
     *
     * @param wrapper
     * @param req
     * @param cacheKey
     * @param webapp
     */
    public CacheServletWrapper40(IServletWrapper wrapper, HttpServletRequest req, String cacheKey, WebApp webapp) {
        super(wrapper, req, cacheKey, webapp);

        String methodName = "CacheServletWrapper40";

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, methodName);
        }

        WebAppDispatcherContext40 dispatchContext = (WebAppDispatcherContext40) ((SRTServletRequest40) req).getWebAppDispatcherContext();
        mapping = dispatchContext.getServletMapping();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, methodName);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        String methodName = "handleRequest";

        // set the MappingMatch here as when the CacheServletWrapper is being used we will not go
        // through the path of URIMatcher.
        //reqData.setMappingMatch(this.mapping.getMappingMatch());
        WebAppDispatcherContext40 dispatchContext = (WebAppDispatcherContext40) ((SRTServletRequest40) req).getWebAppDispatcherContext();
        dispatchContext.setMappingMatch(mapping.getMappingMatch());

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "MappingMatch: " + mapping.getMappingMatch());
        }

        super.handleRequest(req, res);
    }
}
