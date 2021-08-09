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
package com.ibm.ws.wsat.utils;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.jaxws.wsat.components.WSATConfigService;
import com.ibm.ws.jaxws.wsat.components.WSATSSLService;
import com.ibm.ws.wsat.service.Handler;
import com.ibm.ws.wsat.service.Protocol;
import com.ibm.wsspi.classloading.ClassLoadingService;

/**
 *
 */
public class WSATOSGIService {

    private WSATOSGIService() {

    }

    private static WSATOSGIService instance = null;

    public static WSATOSGIService getInstance() {
        if (instance == null) {
            instance = new WSATOSGIService();
        }
        return instance;
    }

//    private static final TraceComponent tc = Tr.register(WSATOSGIService.class, WSTXConstants.TRACE_GROUP, null);

    private Protocol p_service;
    private Handler h_service;
    private ClassLoadingService cl_service;

    public Protocol getProtocolService() {
        if (p_service == null) {
            BundleContext context = FrameworkUtil.getBundle(Protocol.class)
                            .getBundleContext();
            ServiceReference<Protocol> serviceRef = context
                            .getServiceReference(Protocol.class);
            if (serviceRef != null)
                p_service = context.getService(serviceRef);
        }
        return p_service;

    }

    public Handler getHandlerService() {
        if (h_service == null) {
            BundleContext context = FrameworkUtil.getBundle(Handler.class)
                            .getBundleContext();
            ServiceReference<Handler> serviceRef = context
                            .getServiceReference(Handler.class);
            if (serviceRef != null)
                h_service = context.getService(serviceRef);
        }
        return h_service;
    }

    public WSATConfigService getConfigService() {
        BundleContext context = FrameworkUtil.getBundle(WSATConfigService.class)
                        .getBundleContext();
        ServiceReference<WSATConfigService> serviceRef = context.getServiceReference(WSATConfigService.class);
        if (serviceRef != null)
            return context.getService(serviceRef);
        else
            return null;
    }

    public WSATSSLService getSSLService() {
        BundleContext context = FrameworkUtil.getBundle(WSATSSLService.class)
                        .getBundleContext();
        ServiceReference<WSATSSLService> serviceRef = context.getServiceReference(WSATSSLService.class);
        if (serviceRef != null)
            return context.getService(serviceRef);
        else
            return null;
    }

    public ClassLoadingService getThreadContextClassLoaderService() {
        if (cl_service == null) {
            BundleContext context = FrameworkUtil.getBundle(ClassLoadingService.class)
                            .getBundleContext();
            ServiceReference<ClassLoadingService> serviceRef = context.getServiceReference(ClassLoadingService.class);
            if (serviceRef != null)
                cl_service = context.getService(serviceRef);
        }
        return cl_service;
    }
}
