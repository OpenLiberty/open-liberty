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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import org.omg.CSI.EstablishContext;
import org.omg.CSI.ITTAbsent;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.CSIIOP.DelegationByClient;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.CSIIOP.SAS_ContextSec;
import org.omg.CSIIOP.ServiceConfiguration;
import org.omg.IOP.Codec;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.SASInvalidEvidenceException;
import com.ibm.ws.transport.iiop.security.config.ConfigUtil;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSSASMechConfig implements Serializable {

    public static final String TYPE_ITTAnonymous = "ITTAnonymous";
    public static final String TYPE_ITTPrincipalName = "ITTPrincipalName";
    public static final String TYPE_ITTX509CertChain = "ITTX509CertChain";
    public static final String TYPE_ITTDistinguishedName = "ITTDistinguishedName";

    private short supports;
    private short requires;
    private int supportedIdentityTypes;
    private boolean required;
    private final ArrayList<TSSServiceConfigurationConfig> privilegeAuthorities = new ArrayList<TSSServiceConfigurationConfig>();
    private final Map<Short, TSSSASIdentityToken> idTokens = new HashMap<Short, TSSSASIdentityToken>();
    private transient TrustedIDEvaluator trustedIDEvaluator;

    public TSSSASMechConfig() {}

    public TSSSASMechConfig(TrustedIDEvaluator trustedIDEvaluator) {
        this.trustedIDEvaluator = trustedIDEvaluator;
    }

    public TSSSASMechConfig(SAS_ContextSec context) throws Exception {
        supports = context.target_supports;
        requires = context.target_requires;

        ServiceConfiguration[] c = context.privilege_authorities;
        for (int i = 0; i < c.length; i++) {
            privilegeAuthorities.add(TSSServiceConfigurationConfig.decodeIOR(c[i]));
        }

        byte[][] n = context.supported_naming_mechanisms;
        for (int i = 0; i < n.length; i++) {
            String oid = Util.decodeOID(n[i]);

            //TODO is this needed?
            if (TSSITTPrincipalNameGSSUP.OID.equals(oid)) {
                //TODO this doesn't make sense if we plan to use this for identity check.
                addIdentityToken(new TSSITTPrincipalNameGSSUP(null, null, null));
            }
        }

        supports = context.target_supports;
        requires = context.target_requires;
        supportedIdentityTypes = context.supported_identity_types;
    }

    public void addServiceConfigurationConfig(TSSServiceConfigurationConfig config) {
        privilegeAuthorities.add(config);

        supports |= DelegationByClient.value;
        if (required) {
            requires = DelegationByClient.value;
        }
    }

    public TSSServiceConfigurationConfig serviceConfigurationAt(int i) {
        return privilegeAuthorities.get(i);
    }

    public int paSize() {
        return privilegeAuthorities.size();
    }

    public void addIdentityToken(TSSSASIdentityToken token) {
        short type = token.getType();
        idTokens.put(type, token);
        supportedIdentityTypes |= type;

        if (token.getType() > 0) {
            supports |= IdentityAssertion.value;
        }
    }

    public short getSupports() {
        return supports;
    }

    public short getRequires() {
        return requires;
    }

    public int getSupportedIdentityTypes() {
        return supportedIdentityTypes;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
        if (required) {
            requires |= (short) (supports & DelegationByClient.value);
        }
    }

    public SAS_ContextSec encodeIOR(Codec codec) throws Exception {

        SAS_ContextSec result = new SAS_ContextSec();

        int i = 0;
        result.privilege_authorities = new ServiceConfiguration[privilegeAuthorities.size()];
        for (Iterator<TSSServiceConfigurationConfig> iter = privilegeAuthorities.iterator(); iter.hasNext();) {
            result.privilege_authorities[i++] = iter.next().generateServiceConfiguration();
        }

        ArrayList<TSSSASIdentityToken> list = new ArrayList<TSSSASIdentityToken>();
        for (Iterator<TSSSASIdentityToken> iter = idTokens.values().iterator(); iter.hasNext();) {
            TSSSASIdentityToken token = iter.next();

            if (token.getType() == ITTPrincipalName.value) {
                list.add(token);
            }
        }

        i = 0;
        result.supported_naming_mechanisms = new byte[list.size()][];
        for (Iterator<TSSSASIdentityToken> iter = list.iterator(); iter.hasNext();) {
            TSSSASIdentityToken token = iter.next();

            result.supported_naming_mechanisms[i++] = Util.encodeOID(token.getOID());
        }

        result.target_supports = supports;
        result.target_requires = requires;
        result.supported_identity_types = supportedIdentityTypes;

        return result;
    }

    public Subject check(TSSCompoundSecMechConfig compoundSecMech, SSLSession session, EstablishContext msg, Codec codec) throws SASException {
        if (msg != null && msg.identity_token != null) {
            IdentityToken identityToken = msg.identity_token;
            int discriminator = identityToken.discriminator();
            // TODO: Change TSSSASIdentityToken's getType method from short to int.
            TSSSASIdentityToken tssIdentityToken = idTokens.get((short) discriminator);
            if (tssIdentityToken == null) {
                throw new SASInvalidEvidenceException("Unsupported identity token type: " + discriminator, SecurityMinorCodes.INVALID_IDENTITY_TOKEN);
            } else {
                if (isTokenTypeAlwaysAllowed(discriminator) || isPresumedTrust() || compoundSecMech.getAs_mech().isTrusted(trustedIDEvaluator, msg, codec)
                    || compoundSecMech.getTransport_mech().isTrusted(trustedIDEvaluator, session)) {
                    return tssIdentityToken.check(identityToken, codec);
                } else {
                    // TODO: This is the same message as in tWAS, but review message.
                    throw new SASInvalidEvidenceException("Authentication failed. Could not validate Client Authentication Token and/or Client Certificates during Identity Assertion",
                                    SecurityMinorCodes.IDENTITY_SERVER_NOT_TRUSTED);
                }
            }
        } else {
            return null;
        }
    }

    /**
     * ITTAbsent and ITTAnonymous are always allowed regardless of trust.
     * 
     * @param discriminator
     * @return
     */
    private boolean isTokenTypeAlwaysAllowed(int discriminator) {
        return discriminator == ITTAbsent.value || discriminator == ITTAnonymous.value;
    }

    /**
     * Presumed trust is to accept assertions from a known limited set of trusted entities,
     * but without performing trust validation of the principal received at the authentication or transport layers.
     */
    private boolean isPresumedTrust() {
        return trustedIDEvaluator.isTrusted("*");
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Trivial
    void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append(getName()).append(": [\n");
        buf.append(moreSpaces).append("required: ").append(required).append("\n");
        buf.append(moreSpaces).append("SUPPORTS: ").append(ConfigUtil.flags(supports)).append("\n");
        buf.append(moreSpaces).append("REQUIRES: ").append(ConfigUtil.flags(requires)).append("\n");
        for (Iterator<TSSServiceConfigurationConfig> iterator = privilegeAuthorities.iterator(); iterator.hasNext();) {
            TSSServiceConfigurationConfig tssServiceConfigurationConfig = iterator.next();
            tssServiceConfigurationConfig.toString(moreSpaces, buf);
        }
        buf.append("\n");
        for (Iterator<TSSSASIdentityToken> iterator = idTokens.values().iterator(); iterator.hasNext();) {
            TSSSASIdentityToken identityToken = iterator.next();
            identityToken.toString(moreSpaces, buf);
        }
        buf.append(spaces).append("]\n");
    }

    protected String getName() {
        return "TSSSASMechConfig";
    }

}
