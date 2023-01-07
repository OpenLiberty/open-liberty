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
package com.ibm.ws.userbundle.myhandler;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class MyActivitor implements BundleActivator {
    private ServiceRegistration<Handler> serviceRegistration1;
    private ServiceRegistration<Handler> serviceRegistration2;
    private ServiceRegistration<Handler> serviceRegistration3;
    private ServiceRegistration<Handler> serviceRegistration4;
    private ServiceRegistration<Handler> serviceRegistration5;
    private ServiceRegistration<Handler> serviceRegistration6;

    @Override
    public void start(BundleContext context) throws Exception {

        final Hashtable<String, Object> handlerProps = new Hashtable<String, Object>();

        handlerProps.put(HandlerConstants.ENGINE_TYPE, HandlerConstants.ENGINE_TYPE_JAXWS);
        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_IN);
        handlerProps.put(HandlerConstants.IS_CLIENT_SIDE, true);
        handlerProps.put(HandlerConstants.IS_SERVER_SIDE, true);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 3);

        OutHandler1 myHandler = new OutHandler1();
        serviceRegistration1 = context.registerService(Handler.class, myHandler, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);
        OutHandler2 myHandler2 = new OutHandler2();
        serviceRegistration2 = context.registerService(Handler.class, myHandler2, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 1);
        OutHandler3 myHandler3 = new OutHandler3();
        serviceRegistration3 = context.registerService(Handler.class, myHandler3, handlerProps);

        handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_OUT);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 1);
        InHandler1 inHandler1 = new InHandler1();
        serviceRegistration4 = context.registerService(Handler.class, inHandler1, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 2);
        INHandler2 inHandler2 = new INHandler2();
        serviceRegistration5 = context.registerService(Handler.class, inHandler2, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 3);
        InHandler3 inHandler3 = new InHandler3();
        serviceRegistration6 = context.registerService(Handler.class, inHandler3, handlerProps);

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
        if (serviceRegistration6 != null) {
            serviceRegistration6.unregister();
        }

    }

}
