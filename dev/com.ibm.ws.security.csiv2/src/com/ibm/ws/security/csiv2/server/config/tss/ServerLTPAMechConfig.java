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
package com.ibm.ws.security.csiv2.server.config.tss;

import javax.security.auth.Subject;

import org.omg.CSI.EstablishContext;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.security.csiv2.config.LTPAMech;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidEvidenceException;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;
import com.ibm.wsspi.security.ltpa.Token;

/**
 * Represents the authentication layer configuration for authenticating with LTPA token.
 * It is set as the CompoundSecMech'as_context_mech when building the IOR.
 */
public class ServerLTPAMechConfig extends TSSASMechConfig {

    public static final String LTPA_OID = LTPAMech.LTPA_OID;
    public static final String LTPA = "LTPA";
    private final transient Authenticator authenticator;
    private final transient TokenManager tokenManager;
    private final String targetName;
    private final boolean required;

    /**
     * @param authenticator
     * @param tokenManager
     * @param targetName
     * @param required
     */
    public ServerLTPAMechConfig(Authenticator authenticator, TokenManager tokenManager, String targetName, boolean required) {
        this.authenticator = authenticator;
        this.tokenManager = tokenManager;
        this.targetName = targetName;
        this.required = required;
    }

    /**
     * @param context
     *            test-only constructor
     */
    public ServerLTPAMechConfig(AS_ContextSec context) {
        targetName = Util.decodeGSSExportedName(context.target_name).getName();
        required = (context.target_requires == EstablishTrustInClient.value);
        authenticator = null;
        tokenManager = null;
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
    public AS_ContextSec encodeIOR(Codec codec) throws Exception {
        AS_ContextSec result = new AS_ContextSec();

        result.target_supports = EstablishTrustInClient.value;
        result.target_requires = (required ? EstablishTrustInClient.value : 0);
        result.client_authentication_mech = Util.encodeOID(LTPA_OID);
        result.target_name = Util.encodeGSSExportName(LTPA_OID, targetName);

        return result;
    }

    /** {@inheritDoc} */
    @FFDCIgnore(AuthenticationException.class)
    @Override
    public Subject check(EstablishContext msg, Codec codec) throws SASException {
        Subject authenticationLayerSubject = null;

        if (msg != null && msg.client_authentication_token != null && msg.client_authentication_token.length > 0) {
            byte[] decodedTokenBytes = Util.decodeLTPAToken(codec, msg.client_authentication_token);
            // TODO: Determine if target name validation is needed and how to do it.
            try {
                authenticationLayerSubject = authenticator.authenticate(decodedTokenBytes);
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
    @FFDCIgnore({ Exception.class })
    @Override
    public boolean isTrusted(TrustedIDEvaluator trustedIDEvaluator, EstablishContext msg, Codec codec) {
        boolean trusted = false;
        if (msg != null && msg.client_authentication_token != null && msg.client_authentication_token.length > 0) {
            try {
                // TODO: Optimize if possible.
                byte[] decodedTokenBytes = Util.decodeLTPAToken(codec, msg.client_authentication_token);
                Token ltpaToken = tokenManager.recreateTokenFromBytes(decodedTokenBytes);
                String[] accessIDs = ltpaToken.getAttributes("u");
                if (accessIDs != null && accessIDs.length > 0) {
                    String serverIdFromToken = AccessIdUtil.getUniqueId(accessIDs[0]);
                    trusted = trustedIDEvaluator.isTrusted(serverIdFromToken);
                }
            } catch (Exception e) {
                // TODO This is not a critical failure, but determine if a message is needed
            }
        }
        return trusted;
    }

    @Override
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
        buf.append(spaces).append("ServerLTPAMechConfig: [\n");
        buf.append(moreSpaces).append("targetName:   ").append(targetName).append("\n");
        buf.append(moreSpaces).append("required  :   ").append(required).append("\n");
        buf.append(spaces).append("]\n");
    }

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public String getMechanism() {
        return LTPA;
    }

}
