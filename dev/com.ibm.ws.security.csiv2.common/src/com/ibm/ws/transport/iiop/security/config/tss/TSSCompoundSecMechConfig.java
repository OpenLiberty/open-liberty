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

import java.io.Serializable;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import org.omg.CSI.EstablishContext;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.config.ConfigUtil;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSCompoundSecMechConfig implements Serializable {

    private static final TraceComponent tc = Tr.register(TSSCompoundSecMechConfig.class);
    private TSSTransportMechConfig transport_mech;
    private TSSASMechConfig as_mech;
    private TSSSASMechConfig sas_mech;

    public TSSTransportMechConfig getTransport_mech() {
        return transport_mech;
    }

    public void setTransport_mech(TSSTransportMechConfig transport_mech) {
        this.transport_mech = transport_mech;
    }

    public TSSASMechConfig getAs_mech() {
        return as_mech;
    }

    public void setAs_mech(TSSASMechConfig as_mech) {
        this.as_mech = as_mech;
    }

    public TSSSASMechConfig getSas_mech() {
        return sas_mech;
    }

    public void setSas_mech(TSSSASMechConfig sas_mech) {
        this.sas_mech = sas_mech;
    }

    public short getSupports() {
        short result = 0;

        if (transport_mech != null)
            result |= transport_mech.getSupports();
        if (as_mech != null)
            result |= as_mech.getSupports();
        if (sas_mech != null)
            result |= sas_mech.getSupports();

        return result;
    }

    public short getRequires() {
        short result = 0;

        if (transport_mech != null)
            result |= transport_mech.getRequires();
        if (as_mech != null)
            result |= as_mech.getRequires();
        if (sas_mech != null)
            result |= sas_mech.getRequires();

        return result;
    }

    public CompoundSecMech encodeIOR(Codec codec) throws Exception {
        CompoundSecMech result = new CompoundSecMech();

        result.target_requires = 0;

        // transport mechanism
        result.transport_mech = transport_mech.encodeIOR(codec);
        result.target_requires |= transport_mech.getRequires();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(tc, "transport adds supported: " + ConfigUtil.flags(transport_mech.getSupports()));
            Tr.debug(tc, "transport adds required: " + ConfigUtil.flags(transport_mech.getRequires()));
        }

        // AS_ContextSec
        result.as_context_mech = as_mech.encodeIOR(codec);
        result.target_requires |= as_mech.getRequires();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(tc, "AS adds supported: " + ConfigUtil.flags(as_mech.getSupports()));
            Tr.debug(tc, "AS adds required: " + ConfigUtil.flags(as_mech.getRequires()));
        }

        // SAS_ContextSec
        result.sas_context_mech = sas_mech.encodeIOR(codec);
        result.target_requires |= sas_mech.getRequires();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(tc, "SAS adds supported: " + ConfigUtil.flags(sas_mech.getSupports()));
            Tr.debug(tc, "SAS adds required: " + ConfigUtil.flags(sas_mech.getRequires()));

            Tr.debug(tc, "REQUIRES: " + ConfigUtil.flags(result.target_requires));
        }

        return result;
    }

    public static TSSCompoundSecMechConfig decodeIOR(Codec codec, CompoundSecMech compoundSecMech) throws Exception {
        TSSCompoundSecMechConfig result = new TSSCompoundSecMechConfig();

        result.setTransport_mech(TSSTransportMechConfig.decodeIOR(codec, compoundSecMech.transport_mech));
        result.setAs_mech(TSSASMechConfig.decodeIOR(compoundSecMech.as_context_mech));
        result.setSas_mech(new TSSSASMechConfig(compoundSecMech.sas_context_mech));

        return result;
    }

    public Subject check(SSLSession session, EstablishContext msg, Codec codec) throws SASException {
        Subject transportSubject = null;
        Subject authenticationLayerSubject = null;
        Subject attributeLayerSubject = null;

        if (isAssertingAnonymous(msg)) {
            attributeLayerSubject = sas_mech.check(this, session, msg, codec);
        } else {
            transportSubject = transport_mech.check(session);
            authenticationLayerSubject = as_mech.check(msg, codec);
            attributeLayerSubject = sas_mech.check(this, session, msg, codec);
        }

        if (attributeLayerSubject != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The attribute layer subject was selected.");
            }
            return attributeLayerSubject;
        }

        if (authenticationLayerSubject != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The authentication layer subject was selected.");
            }
            return authenticationLayerSubject;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The transport subject was selected.");
        }
        return transportSubject;
    }

    private boolean isAssertingAnonymous(EstablishContext msg) {
        return msg != null && msg.identity_token != null && msg.identity_token.discriminator() == ITTAnonymous.value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Trivial
    void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSCompoundSecMechConfig: [\n");
        buf.append(moreSpaces).append("SUPPORTS (aggregate): ").append(ConfigUtil.flags(getSupports())).append("\n");
        buf.append(moreSpaces).append("REQUIRES (aggregate): ").append(ConfigUtil.flags(getRequires())).append("\n");
        if (transport_mech != null) {
            transport_mech.toString(moreSpaces, buf);
        }
        if (as_mech != null) {
            as_mech.toString(moreSpaces, buf);
        }
        if (sas_mech != null) {
            sas_mech.toString(moreSpaces, buf);
        }
        buf.append(spaces).append("]\n");
    }

}
