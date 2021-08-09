/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;
import org.omg.CSIIOP.TAG_NULL_TAG;
import org.omg.CSIIOP.TAG_SECIOP_SEC_TRANS;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public abstract class TSSTransportMechConfig implements Serializable {

    private boolean trustEveryone;
    private boolean trustNoone = true;
    private final List entities = new ArrayList();

    public boolean isTrustEveryone() {
        return trustEveryone;
    }

    public void setTrustEveryone(boolean trustEveryone) {
        this.trustEveryone = trustEveryone;
    }

    public boolean isTrustNoone() {
        return trustNoone;
    }

    public void setTrustNoone(boolean trustNoone) {
        this.trustNoone = trustNoone;
    }

    public List getEntities() {
        return entities;
    }

    public abstract short getSupports();

    public abstract short getRequires();

    public abstract TaggedComponent encodeIOR(Codec codec) throws Exception;

    public static TSSTransportMechConfig decodeIOR(Codec codec, TaggedComponent tc) throws Exception {
        TSSTransportMechConfig result = null;

        if (tc.tag == TAG_NULL_TAG.value) {
            result = new TSSNULLTransportConfig();
        } else if (tc.tag == TAG_TLS_SEC_TRANS.value) {
            result = new TSSSSLTransportConfig(tc, codec);
        } else if (tc.tag == TAG_SECIOP_SEC_TRANS.value) {
            result = new TSSSECIOPTransportConfig(tc, codec);
        }

        return result;
    }

    public abstract Subject check(SSLSession session) throws SASException;

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        try {
            toString("", buf);
        } catch (Exception e) {
            // will FFDC
            buf.append("TSSTransportMechConfig.toString() threw ").append(e);
        }
        return buf.toString();
    }

    @Trivial
    abstract void toString(String spaces, StringBuilder buf);

    public abstract boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, SSLSession session);

}
