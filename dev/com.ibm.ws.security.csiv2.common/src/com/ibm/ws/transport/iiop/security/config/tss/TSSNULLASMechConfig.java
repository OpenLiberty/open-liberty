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
package com.ibm.ws.transport.iiop.security.config.tss;

import javax.security.auth.Subject;

import org.omg.CSI.EstablishContext;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSNULLASMechConfig extends TSSASMechConfig {

    // Using the GSSUP OID since the CTS implementation is not ignoring the OID for a disabled mechanism as required per the spec.
    public static final String NULL_OID = GSSUPMechOID.value;
    public static final String mechanism = "DISABLED";

    @Override
    public short getSupports() {
        return 0;
    }

    @Override
    public short getRequires() {
        return 0;
    }

    /**
     * Encode a virtually null AS context. Since supports is zero, everything
     * else should be ignored.
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
        buf.append(spaces).append("TSSNULLASMechConfig\n");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, EstablishContext msg, Codec codec) {
        return false;
    }
}
