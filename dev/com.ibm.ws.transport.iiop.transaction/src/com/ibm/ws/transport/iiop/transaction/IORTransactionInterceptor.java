/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.transaction;

import static com.ibm.ws.transport.iiop.transaction.TransactionIORConstants.SERVER_INSTANCE_UUID;
import static com.ibm.ws.transport.iiop.transaction.TransactionIORConstants.SERVER_INSTANCE_UUID_BYTES;
import static com.ibm.ws.transport.iiop.transaction.TransactionIORConstants.TAG_IBM_SERVER_UUID;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.omg.CORBA.Any;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CosTSInteroperation.TAG_INV_POLICY;
import org.omg.CosTransactions.ADAPTS;
import org.omg.CosTransactions.SHARED;
import org.omg.IOP.Codec;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionProtocolProvider;

/**
 * IOR interceptor that adds transaction-related tagged components to IORs.
 *
 * <p>Responsibilities (Plan B):
 * <ol>
 *   <li>Add standard OTS policy tags ({@code TAG_INV_POLICY}, {@code TAG_OTS_POLICY}) —
 *       protocol-agnostic, always the core's responsibility.</li>
 *   <li>Delegate IOR contribution to each enabled registered
 *       {@link TransactionProtocolProvider} via {@code contributeToIOR()} — the core
 *       has zero knowledge of any provider's tag format or payload.</li>
 *   <li>Add {@code TAG_IBM_SERVER_UUID} for local-call optimisation.</li>
 * </ol>
 *
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
final class IORTransactionInterceptor extends LocalObject implements IORInterceptor {

    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(IORTransactionInterceptor.class, "IIOP", null);

    private final Codec codec;

    public IORTransactionInterceptor(Codec codec, TransactionSubsystemFactory factory) {
        this.codec = codec;
    }

    @Override
    public void establish_components(IORInfo info) {
        try {
            // 1. Standard OTS policy tags — always added by core
            Any invAny = ORB.init().create_any();
            invAny.insert_short(SHARED.value);
            byte[] invBytes = codec.encode_value(invAny);
            info.add_ior_component_to_profile(
                new TaggedComponent(TAG_INV_POLICY.value, invBytes), TAG_INTERNET_IOP.value);

            Any otsAny = ORB.init().create_any();
            otsAny.insert_short(ADAPTS.value);
            byte[] otsBytes = codec.encode_value(otsAny);
            info.add_ior_component_to_profile(
                new TaggedComponent(org.omg.IOP.TAG_OTS_POLICY.value, otsBytes), TAG_INTERNET_IOP.value);

            // 2. Delegate IOR contribution to each enabled provider
            TransactionSubsystemFactory factory = TransactionSubsystemFactory.getActiveFactory();
            if (factory != null) {
                List<TransactionProtocolProvider> providers = factory.getSortedProviders();
                for (TransactionProtocolProvider provider : providers) {
                    try {
                        provider.contributeToIOR(info, codec);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "contributeToIOR: {0}", provider.getProtocolName());
                        }
                    } catch (Exception e) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "contributeToIOR failed for {0}: {1}",
                                     provider.getProtocolName(), e);
                        }
                    }
                }
            }

            // 3. Server UUID for local-call optimisation
            byte[] serverUuidData = buildIBMServerUUIDComponent();
            if (serverUuidData != null) {
                info.add_ior_component_to_profile(
                    new TaggedComponent(TAG_IBM_SERVER_UUID, serverUuidData), TAG_INTERNET_IOP.value);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added TAG_IBM_SERVER_UUID: {0}", SERVER_INSTANCE_UUID);
                }
            }

        } catch (INV_POLICY e) {
            // Policy not supported — skip silently
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Error generating IOR components", e);
            }
        }
    }

    /**
     * Build the TAG_IBM_SERVER_UUID component.
     * Format: [length MSB, length LSB, version(1), UUID(16 bytes)]
     */
    private byte[] buildIBMServerUUIDComponent() {
        try {
            int totalLength = 19; // 2 + 1 + 16
            ByteArrayOutputStream bos = new ByteArrayOutputStream(totalLength);
            bos.write((totalLength >> 8) & 0xFF);
            bos.write(totalLength & 0xFF);
            bos.write(1); // version
            bos.write(SERVER_INSTANCE_UUID_BYTES);
            return bos.toByteArray();
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Error building TAG_IBM_SERVER_UUID", e);
            }
            return null;
        }
    }

    @Override
    public void destroy() {}

    @Override
    public String name() {
        return getClass().getName();
    }
}

// Made with Bob
