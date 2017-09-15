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

import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.transport.iiop.security.SASException;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSITTAnonymous extends TSSSASIdentityToken {

    public static final String OID = "";
    private transient UnauthenticatedSubjectService unauthenticatedSubjectService;

    public TSSITTAnonymous() {}

    /**
     * @param unauthenticatedSubjectService used to obtain the unauthenticated subject.
     */
    public TSSITTAnonymous(UnauthenticatedSubjectService unauthenticatedSubjectService) {
        this.unauthenticatedSubjectService = unauthenticatedSubjectService;
    }

    @Override
    public short getType() {
        return ITTAnonymous.value;
    }

    @Override
    public String getOID() {
        return OID;
    }

    @Override
    public Subject check(IdentityToken identityToken, Codec codec) throws SASException {
        return unauthenticatedSubjectService.getUnauthenticatedSubject();
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        buf.append(spaces).append("TSSITTAnonymous\n");
    }

}
