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

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import org.omg.CSIIOP.TAG_NULL_TAG;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

/**
 * At the moment, this config class can only handle a single address.
 * 
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSNULLTransportConfig extends TSSTransportMechConfig {

    @Override
    public short getSupports() {
        return 0;
    }

    @Override
    public short getRequires() {
        return 0;
    }

    @Override
    public TaggedComponent encodeIOR(Codec codec) {
        TaggedComponent result = new TaggedComponent();

        result.tag = TAG_NULL_TAG.value;
        result.component_data = new byte[0];

        return result;
    }

    /**
     * Returns null subject, since the transport layer can not establish the subject.
     * 
     * @param session
     * @return
     * @throws SASException
     */
    @Override
    public Subject check(SSLSession session) throws SASException {
        return null;
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        buf.append(spaces).append("TSSNULLTransportConfig\n");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, SSLSession session) {
        return false;
    }

}
