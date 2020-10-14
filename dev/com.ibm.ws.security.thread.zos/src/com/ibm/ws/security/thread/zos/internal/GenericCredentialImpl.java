/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.thread.zos.internal;

import javax.resource.spi.security.GenericCredential;

/**
 * Basic implementation of GenericCredential.
 *
 * As far as I can tell, the only thing we use GenericCredentials for is
 * to signal to the J2C code that the given Subject (which contains the
 * GenericCredential) represents an MVS user for which we have extracted
 * the UTOKEN from his ACEE; therefore this user's ACEE can be sync'ed
 * to the thread and used by z/OS native resource managers (DB2/CICS/IMS)
 * when obtaining a connection to the resource.
 *
 */
public class GenericCredentialImpl implements GenericCredential {

    public final static String secMechUToken = "oid:1.3.18.0.2.30.1";

    /**
     * The userId associated with the SAFCredential that was used to
     * extract the utoken and build this credential.
     */
    private String userId = null;

    /**
     * The UTOKEN extracted from the ACEE associated with the SAFCredential
     * used to build this credential.
     */
    private byte[] utoken = null;

    public GenericCredentialImpl(String userId, byte[] utoken) {
        this.userId = userId;
        this.utoken = utoken;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getCredentialData() throws SecurityException {
        return utoken;
    }

    /** {@inheritDoc} */
    @Override
    public String getMechType() {
        return secMechUToken;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return userId;
    }
}
