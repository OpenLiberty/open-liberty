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

import org.omg.CORBA.Any;
import org.omg.CSI.GSS_NT_ExportedNameHelper;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidEvidenceException;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Rev: 503493 $ $Date: 2007-02-04 13:47:55 -0800 (Sun, 04 Feb 2007) $
 */
public class TSSITTPrincipalNameGSSUP extends TSSSASIdentityToken {

    public static final String OID = GSSUPMechOID.value.substring(4);
    private final String realmName;
    private final String domainName;
    private final transient Authenticator authenticator;

    public TSSITTPrincipalNameGSSUP(Class principalClass, String realmName, String domainName) throws NoSuchMethodException {
        this.realmName = realmName;
        this.domainName = domainName;
        authenticator = null;
    }

    /**
     * @param authenticator
     * @param targetName
     */
    public TSSITTPrincipalNameGSSUP(Authenticator authenticator, String realmName) {
        this.authenticator = authenticator;
        this.realmName = realmName;
        this.domainName = realmName;
    }

    @Override
    public short getType() {
        return ITTPrincipalName.value;
    }

    @Override
    public String getOID() {
        return OID;
    }

    @FFDCIgnore(AuthenticationException.class)
    @Override
    public Subject check(IdentityToken identityToken, Codec codec) throws SASException {
        Subject identityAssertionSubject = null;

        try {
            String principalName = getPrincipalName(identityToken, codec);
            identityAssertionSubject = authenticator.authenticate(principalName);
        } catch (AuthenticationException e) {
            throw new SASInvalidEvidenceException(e.getMessage(), SecurityMinorCodes.AUTHENTICATION_FAILED);
        }
        return identityAssertionSubject;
    }

    private String getPrincipalName(IdentityToken identityToken, Codec codec) throws SASException {
        byte[] principalNameToken = identityToken.principal_name();
        Any any = null;
        try {
            any = codec.decode_value(principalNameToken, GSS_NT_ExportedNameHelper.type());
        } catch (Exception e) {
            throw new SASException(1, e);
        }
        byte[] principalNameBytes = GSS_NT_ExportedNameHelper.extract(any);
        String principalName = Util.decodeGSSExportedName(principalNameBytes).getName();
        principalName = Util.extractUserNameFromScopedName(principalName);
        return principalName;
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSITTPrincipalNameGSSUP: [\n");
        buf.append(moreSpaces).append("domain: ").append(domainName).append("\n");
        buf.append(moreSpaces).append("realm: ").append(realmName).append("\n");
        buf.append(spaces).append("]\n");
    }

}
