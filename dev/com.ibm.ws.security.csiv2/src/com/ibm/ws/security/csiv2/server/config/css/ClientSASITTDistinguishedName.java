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

import javax.security.auth.Subject;

import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.TraceOptions;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.security.csiv2.server.TraceConstants;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASIdentityToken;
import com.ibm.ws.transport.iiop.security.util.Util;

@TraceOptions(traceGroup = TraceConstants.TRACE_GROUP, messageBundle = TraceConstants.MESSAGE_BUNDLE)
public class ClientSASITTDistinguishedName implements CSSSASIdentityToken {

    private static TraceComponent tc = Tr.register(ClientSASITTDistinguishedName.class);

    /** {@inheritDoc} */
    @FFDCIgnore(Exception.class)
    @Override
    public IdentityToken encodeIdentityToken(Codec codec) {
        IdentityToken token = null;
        String distinguishedName = null;

        try {
            distinguishedName = getDistinguishedName();
            token = createIdentityToken(codec, distinguishedName);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client cannot create the ITTDistinguishedName identity assertion token for distinguished name " + distinguishedName
                             + ". The exception message is: " + e.getMessage());
            }
            String messageFromBundle = Tr.formatMessage(tc, "CSIv2_CLIENT_ASSERTION_CANNOT_ENCODE_DN", distinguishedName, e.getMessage());
            throw new org.omg.CORBA.NO_PERMISSION(messageFromBundle, SecurityMinorCodes.CREDENTIAL_NOT_AVAILABLE, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
        return token;
    }

    private String getDistinguishedName() throws Exception {
        Subject subject = getClientSubject();
        SubjectHelper subjectHelper = new SubjectHelper();
        WSCredential wsCredential = subjectHelper.getWSCredential(subject);
        return wsCredential.getUniqueSecurityName();
    }

    private Subject getClientSubject() {
        SubjectManager subjectManager = new SubjectManager();
        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }
        return subject;
    }

    private IdentityToken createIdentityToken(Codec codec, String distinguishedName) throws Exception {
        byte[] encodedDN = Util.encodeDN(codec, distinguishedName);
        IdentityToken token = new IdentityToken();
        token.dn(encodedDN);
        return token;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        buf.append(spaces).append("ClientSASITTDistinguishedName: [\n");
        buf.append(spaces).append("]\n");
    }

    /** {@inheritDoc} */
    @Override
    public int getType() {
        return ITTDistinguishedName.value;
    }

}
