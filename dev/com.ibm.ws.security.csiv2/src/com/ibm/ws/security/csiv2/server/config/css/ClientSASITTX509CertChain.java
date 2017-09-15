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
package com.ibm.ws.security.csiv2.server.config.css;

import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import org.omg.CSI.ITTX509CertChain;
import org.omg.CSI.IdentityToken;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.csiv2.Constants;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.security.csiv2.server.TraceConstants;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASIdentityToken;
import com.ibm.ws.transport.iiop.security.util.Util;

@TraceOptions(traceGroup = TraceConstants.TRACE_GROUP, messageBundle = TraceConstants.MESSAGE_BUNDLE)
public class ClientSASITTX509CertChain implements CSSSASIdentityToken {

    private final String oid;
    private final String domain;
    private final String realm;

    private static TraceComponent tc = Tr.register(ClientSASITTX509CertChain.class);

    public ClientSASITTX509CertChain(String oid, Class principalClass, String domain, String realm) {
        this.oid = (oid == null ? GSSUPMechOID.value.substring(4) : oid);
        this.domain = domain;
        this.realm = realm;
    }

    public ClientSASITTX509CertChain(String oid, String domain) {
        this.oid = (oid == null ? GSSUPMechOID.value.substring(4) : oid);
        this.domain = domain;
        this.realm = domain;
    }

    /**
     * TODO should also use login domains?
     * 
     * @return IdentityToken
     */
    @Override
    public IdentityToken encodeIdentityToken(Codec codec) {
        IdentityToken token = null;

        SubjectManager subjectManager = new SubjectManager();
        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }

        /*
         * At this point, we have either the invocation subject or the caller subject. Now we need to extract the
         * credentials from this subject. We will use the SubjectHelper utility class to acheive the same.
         */
        SubjectHelper subjectHelper = new SubjectHelper();
        WSCredential wsCredential = subjectHelper.getWSCredential(subject);
        /*
         * in wsCredential object, the certificate is stored against the property name "wssecurity.identity_value".
         * We need to get the value corresponding to this property name.
         */
        try {
            /*
             * Now, from the WSCredential, make sure that the identity token type is certificate chain. To do this,
             * get the value of the property called "wssecurity.identity_name"
             */
            String identityTypeValue = (String) wsCredential.get(Constants.IDENTITY_NAME);
            /*
             * The identityTypeValue must be “ClientCertificate”. There is no point in continuing the process
             * if the value is not “ClientCertificate”
             */
            boolean isITTCert = Constants.ClientCertificate.equalsIgnoreCase(identityTypeValue);
            /* Get the certificate chain from WSCredential */
            X509Certificate[] certificateChain = (X509Certificate[]) wsCredential.get(Constants.IDENTITY_VALUE);

            if (isITTCert && (certificateChain != null)) {
                token = new IdentityToken();
                byte[] encodedCertificateChain = Util.encodeCertChain(codec, certificateChain);
                token.certificate_chain(encodedCertificateChain);
            } else {
                /*
                 * This else means, either the token type is not set to certificate, or the certificate does not exist.
                 * In either case, we need to throw an exception. This exception will be captured in the catch
                 * block, along with other possible exceptions like CredentialExpiredException and
                 * CredentialDestroyedException.
                 */
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Certificate is not available to continue with Identity Assertion. identityTypeValue=" + identityTypeValue);
                }
                String messageFromBundle = Tr.formatMessage(tc, "CSIv2_CLIENT_ASSERTION_CERTIFICATE_INVALID");
                throw new Exception(messageFromBundle);
            }
        } catch (Exception e) {
            /*
             * We consolidate all possible exceptions here. We log the message and then throw them as a single
             * NO_PERMISSION exception
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client cannot create the ITTX509CertChain identity assertion token. The exception message is: " + e.getMessage());
            }
            String messageFromBundle = Tr.formatMessage(tc, "CSIv2_CLIENT_ASSERTION_CANNOT_ENCODE_CC", e.getMessage());
            throw new org.omg.CORBA.NO_PERMISSION(messageFromBundle, SecurityMinorCodes.CREDENTIAL_NOT_AVAILABLE, org.omg.CORBA.CompletionStatus.COMPLETED_NO);

        }
        return token;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("ClientSASITTX509CertChain: [\n");
        buf.append(moreSpaces).append("oid: ").append(oid).append("\n");
        buf.append(moreSpaces).append("domain: ").append(domain).append("\n");
        buf.append(moreSpaces).append("realm: ").append(realm).append("\n");
        buf.append(spaces).append("]\n");
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return ITTX509CertChain.value;
    }

}
