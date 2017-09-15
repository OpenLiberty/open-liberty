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
package com.ibm.ws.transport.iiop.transaction;

import org.omg.CORBA.Any;
import org.omg.CORBA.INV_POLICY;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CosTSInteroperation.TAG_INV_POLICY;
import org.omg.CosTransactions.ADAPTS;
import org.omg.CosTransactions.SHARED;
import org.omg.IOP.Codec;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TAG_OTS_POLICY;
import org.omg.IOP.TaggedComponent;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
final class IORTransactionInterceptor extends LocalObject implements IORInterceptor {

    /**  */
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(IORTransactionInterceptor.class);
    private final Codec codec;

    public IORTransactionInterceptor(Codec codec) {
        this.codec = codec;
    }

    @Override
    public void establish_components(IORInfo info) {

        try {
            Any invAny = ORB.init().create_any();
            invAny.insert_short(SHARED.value);
            byte[] invBytes = codec.encode_value(invAny);
            TaggedComponent invocationPolicyComponent = new TaggedComponent(TAG_INV_POLICY.value, invBytes);
            info.add_ior_component_to_profile(invocationPolicyComponent, TAG_INTERNET_IOP.value);

            Any otsAny = ORB.init().create_any();
            otsAny.insert_short(ADAPTS.value);
            byte[] otsBytes = codec.encode_value(otsAny);
            TaggedComponent otsPolicyComponent = new TaggedComponent(TAG_OTS_POLICY.value, otsBytes);
            info.add_ior_component_to_profile(otsPolicyComponent, TAG_INTERNET_IOP.value);
        } catch (INV_POLICY e) {
            // do nothing
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Generating IOR", e);
        }
    }

    @Override
    public void destroy() {}

    @Override
    public String name() {
        return getClass().getName();
    }

}
