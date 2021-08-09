/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2.server.config.css;

import javax.security.auth.Subject;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.server.config.tss.ServerLTPAMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 * This class deals with encoding the LTPA token as part of the CSIv2 Authentication Layer
 * 
 * @author Sunil M. George
 */
public class ClientLTPAMechConfig implements CSSASMechConfig {

    private static final long serialVersionUID = 1L;

    private final String domain;
    private final boolean required;
    private final transient Authenticator authenticator;
    private final String mechanism = "LTPA";

    public ClientLTPAMechConfig(Authenticator authenticator, String domain, boolean required) {
        this.authenticator = authenticator;
        this.domain = domain;
        this.required = required;
    }

    /** {@inheritDoc} */
    @Override
    public short getSupports() {
        return EstablishTrustInClient.value;
    }

    /** {@inheritDoc} */
    @Override
    public short getRequires() {
        return (required ? EstablishTrustInClient.value : 0);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canHandle(TSSASMechConfig asMech) {
        // We can handle if the server mechanism is also LTPA or if the server authentication layer is not required and we do not require authentication.
        if (asMech instanceof ServerLTPAMechConfig || (asMech.getRequires() == 0 && (required == false))) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getMechanism() {
        return mechanism;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] encode(TSSASMechConfig tssasMechConfig, CSSSASMechConfig sas_mech, ClientRequestInfo ri, Codec codec) {
        Subject subject = null;

        if (tssasMechConfig instanceof ServerLTPAMechConfig) {
            subject = getSubject(sas_mech, subject);
        }

        return createEncoding(subject, ri, codec);
    }

    @FFDCIgnore(AuthenticationException.class)
    private Subject getSubject(CSSSASMechConfig sas_mech, Subject subject) {
        if (sas_mech.isAsserting()) {
            // For identity assertion, get the trusted id and put in this authentication layer
            String trustedIdentity = sas_mech.getTrustedIdentity();
            try {
                subject = authenticator.authenticate(trustedIdentity);
            } catch (AuthenticationException e) {
                // TODO Determine if a message is needed here.
            }
        } else {
            SubjectManager subjectManager = new SubjectManager();
            subject = subjectManager.getInvocationSubject();
            if (subject == null) {
                subject = subjectManager.getCallerSubject();
            }
        }
        return subject;
    }

    @Sensitive
    private byte[] createEncoding(Subject subject, ClientRequestInfo ri, Codec codec) {
        if (subject != null && (new SubjectHelper().isUnauthenticated(subject) == false)) {
            byte[] ltpaTokenBytes = getSSOTokenBytes(subject);
            if (isEncodingForWASClassic(ri)) {
                return Util.encodeLTPATokenForWASClassic(codec, ltpaTokenBytes);
            }
            return Util.encodeLTPAToken(codec, ltpaTokenBytes);
        }
        return new byte[0];
    }

    @Sensitive
    public byte[] getSSOTokenBytes(final javax.security.auth.Subject subject) {
        SingleSignonToken ssoToken = null;
        byte[] ssoTokenBytes = null;
        java.util.Iterator<?> ssoIterator = subject.getPrivateCredentials(SingleSignonToken.class).iterator();
        // TODO: Just get the first SSO token as there should only be one.  If there are more, then we will need to determine how to handle.
        if (ssoIterator.hasNext()) {
            ssoToken = (SingleSignonToken) ssoIterator.next();
        }

        // TODO: There should always be an SSO token unless SSO is disabled.
        // IF SSO is disabled, then this mechanism must not be available in the CSSCompoundSecMechConfig.  Need to determine how to know if SSO is enabled or not.
        if (ssoToken != null) {
            ssoTokenBytes = ssoToken.getBytes();
        }
        return ssoTokenBytes;
    }

    /*
     * WAS Classic puts the IBM_PV_TC_ID tagged component in the IOR.
     * Use that tag to automatically detect that the target server is WAS Classic.
     */
    @FFDCIgnore(BAD_PARAM.class)
    private boolean isEncodingForWASClassic(ClientRequestInfo ri) {
        try {
            int IBM_PV_TC_ID = 0x49424d0a;
            ri.get_effective_component(IBM_PV_TC_ID);
            return true;
        } catch (BAD_PARAM bpe) {
            // Expected if not talking to a WAS Classic server with an IBM ORB.
            return false;
        }
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("CSSLTPAMechConfig: [\n");
        buf.append(moreSpaces).append("domain:   ").append(domain).append("\n");
        buf.append(moreSpaces).append("required  :   ").append(required).append("\n");
        buf.append(spaces).append("]\n");
    }

}
