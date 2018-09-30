/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.streams.cdi;

import java.util.ServiceLoader;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.NamingException;

import org.eclipse.microprofile.reactive.streams.spi.ReactiveStreamsEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.reactive.streams.spi.impl.WASReactiveStreamsEngineImpl;

/*
 * This class is used for creating the Injected JMSContext instances.
 */

public class RSBean {

    private final static TraceComponent tc = Tr.register(RSBean.class);

    /*
     * This method is called while JMSContext is injected in the container. For every new @Inject
     * Annotation a new instance of JMSContext is returned. In case of active transaction when the
     * injected JMSContext is being used, a new instance is created and added to the transaction
     * registry so that next request with the same configuration will return the existing JMSContext
     * rather than creating a new one.
     */
    @Produces
    @ApplicationScoped
    public ReactiveStreamsEngine getEngine(InjectionPoint injectionPoint) throws NamingException {
        System.out.println("GDH1");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "GDH3 " + injectionPoint);
        }
        ServiceLoader<ReactiveStreamsEngine> z = ServiceLoader.load(ReactiveStreamsEngine.class);

        if (z.iterator().hasNext()) {
            return z.iterator().next();
        } else {
            return WASReactiveStreamsEngineImpl.getEngine();
        }
    }

    //The CDI container will call this method once the injected JMSContext goes out of scope
    public void disposeEngine(@Disposes ReactiveStreamsEngine context) {
        System.out.println("GDH2");
    }

}
