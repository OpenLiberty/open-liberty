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

import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;

/**
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
final class IORSecurityInterceptor extends LocalObject implements IORInterceptor {
    /**  */
    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(IORSecurityInterceptor.class);

    private final TSSConfig defaultConfig;
    private final Codec codec;

    public IORSecurityInterceptor(TSSConfig defaultConfig, Codec codec) {
        this.codec = codec;
        this.defaultConfig = defaultConfig;
    }

    @Override
    public void establish_components(IORInfo info) {

        try {
            // TODO: Restore when the server policy is finally placed in the ORB, root POA, or some other proper location.
            ServerPolicy policy = (ServerPolicy) info.get_effective_policy(ServerPolicyFactory.POLICY_TYPE);
            // Try to get a config from the policy, and fall back on the default

            // TODO: Remove when the server policy is finally placed in the ORB, root POA, or some other proper location.
            // Due to a timing problem, the CSIv2SubsystemFactory starts after this IORSecurityInterceptor is created and there was no default config.
//            ServerPolicy policy = (ServerPolicy) IIOPEndpointImpl.getTemporaryServerSecurityPolicy();

            TSSConfig config;
            if (policy == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "There was no server policy found of type: " + ServerPolicyFactory.POLICY_TYPE + ". Using default configuration.");
                }
                config = defaultConfig;
            }
            else {
                config = policy.getConfig();
                if (config == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "There was no TSSConfig object found in the server policy. Using default configuration.");
                    }
                    config = defaultConfig;
                }
            }
            // nothing to work with, just return 
            if (config == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "There was no TSSConfig object found.");
                }
                return;
            }

            info.add_ior_component_to_profile(config.generateIOR(codec), TAG_INTERNET_IOP.value);
        } catch (INV_POLICY e) {
            // do nothing
        } catch (Exception e) {
            Tr.error(tc, "Generating IOR", e);
        }
    }

    @Override
    public void destroy() {}

    @Override
    public String name() {
        return "org.apache.geronimo.corba.security.IORSecurityInterceptor";
    }

}
