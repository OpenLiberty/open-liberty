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

import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import org.omg.CSI.ITTX509CertChain;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.Constants;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidEvidenceException;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSITTX509CertChain extends TSSSASIdentityToken {

    public static final String OID = "";
    private final String realmName;
    private final String domainName;
    private transient Authenticator authenticator;

    public TSSITTX509CertChain(String realmName, String domainName) {
        this.realmName = realmName;
        this.domainName = domainName;
    }

    /**
     * @param authenticator
     */
    public TSSITTX509CertChain(Authenticator authenticator) {
        this.authenticator = authenticator;
        this.realmName = null;
        this.domainName = null;
    }

    @Override
    public short getType() {
        return ITTX509CertChain.value;
    }

    @Override
    public String getOID() {
        return OID;
    }

    @FFDCIgnore(Exception.class)
    @Override
    public Subject check(IdentityToken identityToken, Codec codec) throws SASException {
        Subject identityAssertionSubject = null;
        byte[] encodedCertChain = identityToken.certificate_chain();
        X509Certificate[] certificateChain = Util.decodeCertChain(codec, encodedCertChain);

        try {
            identityAssertionSubject = authenticator.authenticate(certificateChain);
            /* Here we need to get the WSCredential from the subject. We will use the subject manager for the same. */
            SubjectHelper subjectHelper = new SubjectHelper();
            WSCredential wsCredential = subjectHelper.getWSCredential(identityAssertionSubject);
            /*
             * First we tell the WSCredential that the identity token is in the form of a certificate chain. This is
             * done by setting the property "wssecurity.identity_name" to "ClientCertificate"
             */
            wsCredential.set(Constants.IDENTITY_NAME, Constants.ClientCertificate);
            /*
             * Now we need to put the certificate chain into the WScredential object. By doing this, we
             * make sure that, the authenticated certificates are indeed part of the credential. This credential
             * can be used further down the CSIv2 flow. The certificate is set as a name value pair, with the name
             * "wssecurity.identity_value"
             */
            wsCredential.set(Constants.IDENTITY_VALUE, certificateChain);

        } catch (Exception e) {
            /*
             * We consolidate all possible exceptions, including AuthenticationExcetion, CredentianExpiredException
             * and CredentialDestroyedException. We then throw them as a single SASInvalidEvidenceException exception.
             */
            throw new SASInvalidEvidenceException(e.getMessage(), SecurityMinorCodes.AUTHENTICATION_FAILED);
        }
        return identityAssertionSubject;
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSITTX509CertChain: [\n");
        buf.append(moreSpaces).append("domain: ").append(domainName).append("\n");
        buf.append(moreSpaces).append("realm: ").append(realmName).append("\n");
        buf.append(spaces).append("]\n");
    }

}
