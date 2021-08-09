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
package com.ibm.ws.transport.iiop.security.config.css;

import javax.security.auth.Subject;

import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * This GSSUP mechanism obtains its username and password from a named username
 * password credential that is stored in the subject associated w/ the call
 * stack.
 * 
 * @version $Revision: 503493 $ $Date: 2007-02-04 13:47:55 -0800 (Sun, 04 Feb 2007) $
 */
public class CSSGSSUPMechConfigDynamic implements CSSASMechConfig {

    private static TraceComponent tc = Tr.register(CSSGSSUPMechConfigDynamic.class);

    private final String mechanism = "GSSUP";
    private final String domain;
    private final boolean required;
    private final String username;
    private final SerializableProtectedString password;

    /* ctor for the client */
    public CSSGSSUPMechConfigDynamic(String username, SerializableProtectedString password, String domain, boolean required) {
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.required = required;
    }

    /* ctor for the server */
    public CSSGSSUPMechConfigDynamic(String domain, boolean required) {
        this.username = null;
        this.password = null;
        this.domain = domain;
        this.required = required;
    }

    public CSSGSSUPMechConfigDynamic(String domain) {
        this.username = null;
        this.password = null;
        this.domain = domain;
        required = false;
    }

    @Override
    public short getSupports() {
        return EstablishTrustInClient.value;
    }

    @Override
    public short getRequires() {
        return (required ? EstablishTrustInClient.value : 0);
    }

    @Override
    public boolean canHandle(TSSASMechConfig asMech) {
        // We can handle if the server mechanism is also GSSUP or if the server authentication layer is not required and we do not require authentication.
        if (asMech instanceof TSSGSSUPMechConfig || (asMech.getRequires() == 0 && (required == false))) {
            return true;
        }
        return false;
    }

    @Override
    public String getMechanism() {
        return mechanism;
    }

    @Sensitive
    @Override
    public byte[] encode(TSSASMechConfig tssasMechConfig, CSSSASMechConfig sas_mech, ClientRequestInfo ri, Codec codec) {
        byte[] encoding = null;

        if (tssasMechConfig instanceof TSSGSSUPMechConfig) {
            String targetName = ((TSSGSSUPMechConfig) tssasMechConfig).getTargetName();
            if (sas_mech.isAsserting()) {
                encoding = encodeGSSUPFromTrustedIdAndPassword(sas_mech, codec, targetName);
            } else {
                encoding = encodeGSSUPFromBasicAuthSubject(codec, targetName);
            }
        }

        if (encoding == null) {
            encoding = new byte[0];
        }
        return encoding;
    }

    @Sensitive
    private byte[] encodeGSSUPFromTrustedIdAndPassword(CSSSASMechConfig sas_mech, Codec codec, String targetName) {
        byte[] encoding = new byte[0];
        String trustedIdentity = sas_mech.getTrustedIdentity();
        SerializableProtectedString trustedPassword = sas_mech.getTrustedPassword();

        if (trustedIdentity != null && (trustedPassword != null && trustedPassword.isEmpty() == false)) {
            String password = PasswordUtil.passwordDecode(new String(trustedPassword.getChars()));
            encoding = commonEncode(codec, trustedIdentity, password, targetName);
        }
        return encoding;
    }

    @Sensitive
    private byte[] encodeGSSUPFromBasicAuthSubject(Codec codec, String targetName) {
        byte[] encoding = new byte[0];
        Subject subject = getSubject();
        SubjectHelper subjectHelper = new SubjectHelper();
        WSCredential wsCredential = null;
        if (subject != null) {
            wsCredential = subjectHelper.getWSCredential(subject);
        }

        // There must be a WSCredential in the subject, but check for null to satisfy findbugs
        if (wsCredential != null && wsCredential.isBasicAuth()) {
            try {
                encoding = commonEncode(codec, wsCredential.getSecurityName(), new String(wsCredential.getCredentialToken(), "UTF-8"), targetName);
            } catch (Exception e) {
                // This should never happen since a basic auth credential never expires and it is never in a destroyed state, but this exception needs to be caught.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The GSSUP token could not be encoded because of exception with message: " + e.getMessage());
                }
            }
        } else if (password != null && !password.isEmpty() && username != null && !username.isEmpty()) {
            // fallback to the default.
            String scopedUserName = Util.buildScopedUserName(username, targetName);
            String pwd = PasswordUtil.passwordDecode(new String(password.getChars()));
            encoding = Util.encodeGSSUPToken(codec, scopedUserName, pwd.toCharArray(), targetName);
        }
        return encoding;
    }

    private Subject getSubject() {
        SubjectManager subjectManager = new SubjectManager();
        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }
        return subject;
    }

    @Sensitive
    private byte[] commonEncode(Codec codec, String username, @Sensitive String password, String targetName) {
        String extendedUserName = Util.buildScopedUserName(username, targetName);
        return Util.encodeGSSUPToken(codec, extendedUserName, password.toCharArray(), targetName);

    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("CSSGSSUPMechConfigDynamic: [\n");
        buf.append(moreSpaces).append("domain:   ").append(domain).append("\n");
        if (username != null) {
            buf.append(moreSpaces).append("user:   ").append(username).append("\n");
        }
        buf.append(spaces).append("]\n");
    }

}
