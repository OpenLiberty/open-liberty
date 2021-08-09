/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.config.tss;

import javax.security.auth.Subject;

import org.omg.CSI.EstablishContext;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

/**
 * Represents the authentication layer configuration for unknown OID.
 * It is set as the CompoundSecMech'as_context_mech when building the IOR when incoming OID is neither LTPA nor GSSUP.
 */
public class TSSUnknownASMechConfig extends TSSASMechConfig {
    public static final String NULL_OID = "oid:0.0.0.0.0.0";
    public static final String DEFAULT_MECH = "UNKNOWN";

    private String mechanism = DEFAULT_MECH;

    public TSSUnknownASMechConfig(String mechanism) {
        if (mechanism != null)
            this.mechanism = mechanism;
    }

    @Override
    public short getSupports() {
        return EstablishTrustInClient.value;
    }

    @Override
    public short getRequires() {
        return EstablishTrustInClient.value;
    }

    /**
     * This method should not be invoked, but just in case that is not the case,
     * return null OID.
     * 
     * @param orb
     * @param codec
     * @return
     * @throws Exception
     */
    @Override
    public AS_ContextSec encodeIOR(Codec codec) throws Exception {
        AS_ContextSec result = new AS_ContextSec();

        result.target_supports = 0;
        result.target_requires = 0;
        result.client_authentication_mech = Util.encodeOID(NULL_OID);
        result.target_name = Util.encodeGSSExportName(NULL_OID, "");

        return result;
    }

    @Override
    public Subject check(EstablishContext msg, Codec codec) throws SASException {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getMechanism() {
        return mechanism;
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        buf.append(spaces).append("TSSUnknownASMechConfig\n");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, EstablishContext msg, Codec codec) {
        return false;
    }
}
