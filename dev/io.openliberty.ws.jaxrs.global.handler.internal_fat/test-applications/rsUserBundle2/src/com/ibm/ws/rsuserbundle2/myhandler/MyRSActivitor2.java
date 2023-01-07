/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.rsuserbundle2.myhandler;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class MyRSActivitor2 implements BundleActivator {
    private ServiceRegistration<Handler> serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {

        final Hashtable<String, Object> handlerProps = new Hashtable<String, Object>();

        handlerProps.put(HandlerConstants.ENGINE_TYPE, HandlerConstants.ENGINE_TYPE_JAXRS);
        handlerProps.put(HandlerConstants.IS_SERVER_SIDE, true);
        handlerProps.put(HandlerConstants.IS_CLIENT_SIDE, false);

        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_IN);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 1);
        RSInHandler2 inHandler2 = new RSInHandler2();
        serviceRegistration = context.registerService(Handler.class, inHandler2, handlerProps);

        RSClientInHander clientHander = new RSClientInHander();
        handlerProps.put(HandlerConstants.ENGINE_TYPE, HandlerConstants.ENGINE_TYPE_JAXRS);
        handlerProps.put(HandlerConstants.IS_CLIENT_SIDE, true);
        handlerProps.put(HandlerConstants.IS_SERVER_SIDE, false);
        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_IN);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 3);
        serviceRegistration = context.registerService(Handler.class, clientHander, handlerProps);

        System.out.println("in start method in bundle activator");

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceRegistration != null)
            serviceRegistration.unregister();

    }

}
