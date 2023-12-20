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
package com.ibm.ws.userbundle5.myhandler;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class MyActivitor5 implements BundleActivator {
    private ServiceRegistration<Handler> serviceRegistration1;
    private ServiceRegistration<Handler> serviceRegistration2;
    private ServiceRegistration<Handler> serviceRegistration3;
    private ServiceRegistration<Handler> serviceRegistration4;
    private ServiceRegistration<Handler> serviceRegistration5;

    @Override
    public void start(BundleContext context) throws Exception {

        final Hashtable<String, Object> handlerProps = new Hashtable<String, Object>();

        handlerProps.put(HandlerConstants.ENGINE_TYPE, HandlerConstants.ENGINE_TYPE_JAXWS);
        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_OUT);
        handlerProps.put(HandlerConstants.IS_CLIENT_SIDE, true);
        handlerProps.put(HandlerConstants.IS_SERVER_SIDE, true);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 1);

        OutHandler1InBuldle5 myHandler = new OutHandler1InBuldle5();
        serviceRegistration1 = context.registerService(Handler.class, myHandler, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);
        OutHandler2InBuldle5 myHandler2 = new OutHandler2InBuldle5();
        serviceRegistration2 = context.registerService(Handler.class, myHandler2, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 3);
        OutHandler3InBuldle5 myHandler3 = new OutHandler3InBuldle5();
        serviceRegistration3 = context.registerService(Handler.class, myHandler3, handlerProps);

        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_IN);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 3);
        InHandler1InBuldle5 outHandler1 = new InHandler1InBuldle5();
        serviceRegistration4 = context.registerService(Handler.class, outHandler1, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);
        InHandler2InBuldle5 outHandler2 = new InHandler2InBuldle5();
        serviceRegistration5 = context.registerService(Handler.class, outHandler2, handlerProps);

        System.out.println("in start method in bundle activator");

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceRegistration1 != null) {
            serviceRegistration1.unregister();
        }
        if (serviceRegistration2 != null) {
            serviceRegistration2.unregister();
        }
        if (serviceRegistration3 != null) {
            serviceRegistration3.unregister();
        }
        if (serviceRegistration4 != null) {
            serviceRegistration4.unregister();
        }
        if (serviceRegistration5 != null) {
            serviceRegistration5.unregister();
        }

    }

}
