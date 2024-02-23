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
package com.ibm.ws.userbundle2.myhandler;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.webservices.handler.Handler;

public class MyActivitor2 implements BundleActivator {
    private ServiceRegistration<Handler> serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        final Hashtable<String, Object> handlerProps = new Hashtable<String, Object>();
        // handlerProps.put(HandlerConstants.ENGINE_TYPE, HandlerConstants.ENGINE_TYPE_JAXWS);
        // handlerProps.put(HandlerConstants.FLOW_TYPE, HandlerConstants.FLOW_TYPE_INOUT);
        // handlerProps.put(HandlerConstants.IS_CLIENT_SIDE, true);
        // handlerProps.put(HandlerConstants.IS_SERVER_SIDE, true);
        handlerProps.put(org.osgi.framework.Constants.SERVICE_DESCRIPTION, "TestHandler1InBundle2");
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 1);
        TestHandler1InBundle2 testHandler1InBundle2 = new TestHandler1InBundle2();
        serviceRegistration = context.registerService(Handler.class, testHandler1InBundle2, handlerProps);

        handlerProps.put(org.osgi.framework.Constants.SERVICE_DESCRIPTION, "TestHandler2InBundle2");
        handlerProps.put(org.osgi.framework.Constants.SERVICE_RANKING, 3);
        TestHandler2InBundle2 testHandler2InBundle2 = new TestHandler2InBundle2();
        serviceRegistration = context.registerService(Handler.class, testHandler2InBundle2, handlerProps);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }
}
