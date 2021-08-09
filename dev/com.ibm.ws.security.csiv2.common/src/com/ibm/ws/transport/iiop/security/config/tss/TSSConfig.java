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
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.SASException;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSConfig implements Serializable {

//    private TSSTransportMechConfig transport_mech;
    private final TSSCompoundSecMechListConfig mechListConfig = new TSSCompoundSecMechListConfig();

//    public TSSTransportMechConfig getTransport_mech() {
//        return transport_mech;
//    }
//
//    public void setTransport_mech(TSSTransportMechConfig transport_mech) {
//        this.transport_mech = transport_mech;
//    }

    public TSSCompoundSecMechListConfig getMechListConfig() {
        return mechListConfig;
    }

    public TaggedComponent generateIOR(Codec codec) throws Exception {
        return mechListConfig.encodeIOR(codec);
    }

    public Subject check(SSLSession session, EstablishContext msg, Codec codec) throws SASException {

//        Subject transportSubject = transport_mech.check(session);

        Subject mechSubject = mechListConfig.check(session, msg, codec);
        return mechSubject;
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
        buf.append(spaces).append("TSSConfig: [\n");
//        if (transport_mech != null) {
//            transport_mech.toString(moreSpaces, buf);
//        } else {
//            buf.append(moreSpaces).append("null transport_mech\n");
//        }
        mechListConfig.toString(moreSpaces, buf);
        buf.append(spaces).append("]\n");
    }
}
