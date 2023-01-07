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
package com.ibm.ws.rsuserbundle3.myhandler;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class RSMessageContextAPITestActivitor implements BundleActivator {
    private ServiceRegistration<Handler> serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {

        final Hashtable<String, Object> handlerProps = new Hashtable<String, Object>();

        handlerProps.put(HandlerConstants.ENGINE_TYPE, HandlerConstants.ENGINE_TYPE_JAXRS);
        handlerProps.put(HandlerConstants.IS_SERVER_SIDE, true);
        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_IN);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);

        RSMessageContextAPITestInHandler1 inHandler = new RSMessageContextAPITestInHandler1();
        serviceRegistration = context.registerService(Handler.class, inHandler, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 1);
        RSMessageContextAPITestInHandler2 inHandler2 = new RSMessageContextAPITestInHandler2();
        serviceRegistration = context.registerService(Handler.class, inHandler2, handlerProps);

        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_OUT);
        RSMessageContextAPITestOutHandler2 outHandler2 = new RSMessageContextAPITestOutHandler2();
        serviceRegistration = context.registerService(Handler.class, outHandler2, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);
        RSMessageContextAPITestOutHandler1 outHandler = new RSMessageContextAPITestOutHandler1();
        serviceRegistration = context.registerService(Handler.class, outHandler, handlerProps);

        System.out.println("in start method in bundle activator");

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceRegistration != null)
            serviceRegistration.unregister();
    }

}
