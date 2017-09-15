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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.ssl.yoko;

import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @version $Revision: 452600 $ $Date: 2006-10-03 12:29:42 -0700 (Tue, 03 Oct 2006) $
 */
public class ORBInitializer extends LocalObject implements org.omg.PortableInterceptor.ORBInitializer {

    private static final TraceComponent tc = Tr.register(ORBInitializer.class);

    public ORBInitializer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.debug(tc, "ORBInitializer.<init>");
    }

    /**
     * Called during ORB initialization. If it is expected that initial
     * services registered by an interceptor will be used by other
     * interceptors, then those initial services shall be registered at
     * this point via calls to
     * <code>ORBInitInfo.register_initial_reference</code>.
     * 
     * @param info provides initialization attributes and operations by
     *            which Interceptors can be registered.
     */
    @Override
    public void pre_init(ORBInitInfo info) {}

    /**
     * Called during ORB initialization. If a service must resolve initial
     * references as part of its initialization, it can assume that all
     * initial references will be available at this point.
     * <p/>
     * Calling the <code>post_init</code> operations is not the final
     * task of ORB initialization. The final task, following the
     * <code>post_init</code> calls, is attaching the lists of registered
     * interceptors to the ORB. Therefore, the ORB does not contain the
     * interceptors during calls to <code>post_init</code>. If an
     * ORB-mediated call is made from within <code>post_init</code>, no
     * request interceptors will be invoked on that call.
     * Likewise, if an operation is performed which causes an IOR to be
     * created, no IOR interceptors will be invoked.
     * 
     * @param info provides initialization attributes and
     *            operations by which Interceptors can be registered.
     */
    @Override
    public void post_init(ORBInitInfo info) {

        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.debug(tc, "Registering IOR interceptor");

            try {
                info.add_server_request_interceptor(new ServiceContextInterceptor());
            } catch (DuplicateName dn) {
                Tr.error(tc, "Error registering interceptor", dn);
            }
        } catch (RuntimeException re) {
            Tr.error(tc, "Error registering interceptor", re);
            throw re;
        }
    }
}
