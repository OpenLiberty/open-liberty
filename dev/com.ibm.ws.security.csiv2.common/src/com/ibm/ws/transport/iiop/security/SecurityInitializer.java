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
package com.ibm.ws.transport.iiop.security;

import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;
import com.ibm.ws.transport.iiop.security.util.Util;

public class SecurityInitializer extends LocalObject implements ORBInitializer {
    private static final Encoding CDR_1_2_ENCODING = new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2);
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(SecurityInitializer.class);

    public SecurityInitializer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.debug(tc, "SecurityInitializer.<init>");
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
                Tr.debug(tc, "Registering interceptors and policy factories");

            TSSConfig config = Util.getRegisteredTSSConfig(info.orb_id());

            try {
                Codec codec;
                try {
                    codec = info.codec_factory().create_codec(CDR_1_2_ENCODING);
                } catch (UnknownEncoding e) {
                    INITIALIZE err = new org.omg.CORBA.INITIALIZE("Could not create CDR 1.2 codec");
                    err.initCause(e);
                    throw err;
                }

                info.add_client_request_interceptor(new ClientSecurityInterceptor(codec));
                info.add_server_request_interceptor(new ServerSecurityInterceptor(codec));
                info.add_ior_interceptor(new IORSecurityInterceptor(config, codec));
            } catch (DuplicateName dn) {
                Tr.error(tc, "Error registering interceptor", dn);
            }

            info.register_policy_factory(ClientPolicyFactory.POLICY_TYPE, new ClientPolicyFactory());
            info.register_policy_factory(ServerPolicyFactory.POLICY_TYPE, new ServerPolicyFactory());
        } catch (RuntimeException re) {
            Tr.error(tc, "Error registering interceptor", re);
            throw re;
        }
    }
}
