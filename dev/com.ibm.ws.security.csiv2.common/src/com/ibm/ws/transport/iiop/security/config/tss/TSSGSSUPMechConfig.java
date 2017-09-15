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

import java.io.UnsupportedEncodingException;

import javax.security.auth.Subject;

import org.omg.CSI.EstablishContext;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.GSSUP.InitialContextToken;
import org.omg.IOP.Codec;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.security.csiv2.TraceConstants;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidEvidenceException;
import com.ibm.ws.transport.iiop.security.SASInvalidMechanismException;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

/**
 * @version $Rev: 503493 $ $Date: 2007-02-04 13:47:55 -0800 (Sun, 04 Feb 2007) $
 */
public class TSSGSSUPMechConfig extends TSSASMechConfig {

    private transient Authenticator authenticator = null;
    public static final String mechanism = "GSSUP";
    private String targetName;
    private boolean required;

    public TSSGSSUPMechConfig() {}

    public TSSGSSUPMechConfig(Authenticator authenticator, String targetName, boolean required) {
        this.authenticator = authenticator;
        this.targetName = targetName;
        this.required = required;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
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
    public AS_ContextSec encodeIOR(Codec codec) throws Exception {
        AS_ContextSec result = new AS_ContextSec();

        result.target_supports = EstablishTrustInClient.value;
        result.target_requires = (required ? EstablishTrustInClient.value : 0);
        result.client_authentication_mech = Util.encodeOID(GSSUPMechOID.value);
        result.target_name = Util.encodeGSSExportName(GSSUPMechOID.value, targetName);

        return result;
    }

    @FFDCIgnore(AuthenticationException.class)
    @Override
    public Subject check(EstablishContext msg, Codec codec) throws SASException {
        Subject authenticationLayerSubject = null;

        if (msg != null && msg.client_authentication_token != null && msg.client_authentication_token.length > 0) {
            InitialContextToken token = new InitialContextToken();

            if (!Util.decodeGSSUPToken(codec, msg.client_authentication_token, token)) {
                String messageFromBundle = TraceNLS.getFormattedMessage(this.getClass(),
                                                                        TraceConstants.MESSAGE_BUNDLE,
                                                                        "CSIv2_SERVER_CANNOT_DECODE_GSSUP",
                                                                        new Object[] {},
                                                                        "CWWKS9549E: The server cannot decode the GSSUP token sent by the client and it cannot authenticate the token.");
                throw new SASInvalidMechanismException(messageFromBundle, SecurityMinorCodes.GSS_FORMAT_ERROR);
            }

            if (token.target_name == null) {
                return null;
            }

            try {
                String tokenTargetName = new String(token.target_name, "UTF8");

                if (!targetName.equals(tokenTargetName)) {
                    throw new SASException(2);
                }

                String username = Util.extractUserNameFromScopedName(token.username);
                authenticationLayerSubject = authenticator.authenticate(username, new String(token.password, "UTF8"));
            } catch (UnsupportedEncodingException e) {
                throw new SASException(1, e);
            } catch (AuthenticationException e) {
                // An exception must be thrown on authentication failure per CSIv2 spec regardless that the mechanism is required or not.
                throw new SASInvalidEvidenceException(e.getMessage(), SecurityMinorCodes.AUTHENTICATION_FAILED);
            }
        } else {
            if (required) {
                // TODO: Get translated message.
                throw new SASInvalidEvidenceException("Client authentication is required at the server, but there was no client authentication token sent by the client.", SecurityMinorCodes.AUTHENTICATION_FAILED);
            }
        }

        return authenticationLayerSubject;
    }

    /** {@inheritDoc} */
    @FFDCIgnore({ UnsupportedEncodingException.class })
    @Override
    public boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, EstablishContext msg, Codec codec) {
        // Get user and password from EstablishContext message and validate trust
        if (msg != null && msg.client_authentication_token != null && msg.client_authentication_token.length > 0) {
            InitialContextToken token = new InitialContextToken();
            if (Util.decodeGSSUPToken(codec, msg.client_authentication_token, token)) {
                try {
                    String user = Util.extractUserNameFromScopedName(token.username);
                    String password = new String(token.password, "UTF8");
                    return trustedIDEvaluator.isTrusted(user, password);
                } catch (UnsupportedEncodingException e) {
                    // TODO: Determine if a message is needed
                }
            }
        }
        return false;
    }

    @Override
    public String getMechanism() {
        return mechanism;
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
        buf.append(spaces).append("TSSGSSUPMechConfig: [\n");
        buf.append(moreSpaces).append("targetName:   ").append(targetName).append("\n");
        buf.append(moreSpaces).append("required  :   ").append(required).append("\n");
        buf.append(spaces).append("]\n");
    }

}
