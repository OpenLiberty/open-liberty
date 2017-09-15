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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.omg.CORBA.NO_PERMISSION;
import org.omg.CSI.AuthorizationElement;
import org.omg.CSI.ITTAbsent;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.ITTX509CertChain;
import org.omg.CSI.IdentityToken;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.IOP.Codec;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.security.csiv2.TraceConstants;
import com.ibm.ws.transport.iiop.security.config.ConfigUtil;
import com.ibm.ws.transport.iiop.security.config.tss.TSSSASMechConfig;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class CSSSASMechConfig implements Serializable {

    private static final TraceComponent tc = Tr.register(CSSSASMechConfig.class);

    public static final String TYPE_ITTAnonymous = "ITTAnonymous";
    public static final String TYPE_ITTPrincipalName = "ITTPrincipalName";
    public static final String TYPE_ITTX509CertChain = "ITTX509CertChain";
    public static final String TYPE_ITTDistinguishedName = "ITTDistinguishedName";

    protected short supports;
    protected final Map<Integer, CSSSASIdentityToken> idTokens = new HashMap<Integer, CSSSASIdentityToken>();

    private short requires;
    private boolean required;
    private CSSSASIdentityToken identityToken;
    private String trustedIdentity;
    private SerializableProtectedString trustedPassword;
    private int supportedIdentityTypes;
    private String cantHandleMsg = null;
    private final List<String> configuredTypes = new ArrayList<String>();

    public short getSupports() {
        return supports;
    }

    public short getRequires() {
        return requires;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public CSSSASIdentityToken getIdentityToken() {
        return identityToken;
    }

    public void setIdentityToken(CSSSASIdentityToken identityToken) {
        this.identityToken = identityToken;
    }

    public void addIdentityToken(CSSSASIdentityToken identityToken) {
        int type = (short) identityToken.getType();
        supportedIdentityTypes |= type;
        idTokens.put(identityToken.getType(), identityToken);

        if (identityToken.getType() > 0) {
            supports |= IdentityAssertion.value;
            if (type != ITTAnonymous.value) {
                configuredTypes.add(typesToString(type));
            }
        }
    }

    public boolean canHandle(TSSSASMechConfig sasMech, String clientMech) {
        if ((supports & sasMech.getRequires()) != sasMech.getRequires()) {
            buildSupportsFailedMsg(sasMech, clientMech);
            return false;
        }
        if ((requires & sasMech.getSupports()) != requires) {
            buildRequiresFailedMsg(sasMech, clientMech);
            return false;
        }
        // If both of us support identity assertion other than absent, check that we have at least one compatible type.
        int targetServerSupportedTypes = sasMech.getSupportedIdentityTypes();
        if (supportedIdentityTypes != 0 && targetServerSupportedTypes != 0 && ((supportedIdentityTypes & targetServerSupportedTypes) == 0)) {
            buildIdentityAssertionFailedMsg(sasMech, clientMech);
            return false;
        }

        return true;
    }

    public String getCantHandleMsg() {
        return cantHandleMsg;
    }

    public AuthorizationElement[] encodeAuthorizationElement() {
        return new AuthorizationElement[0];
    }

    public IdentityToken encodeIdentityToken(TSSSASMechConfig tsssasMechConfig, Codec codec) {
        CSSSASIdentityToken identityToken = idTokens.get(ITTAbsent.value);

        if (supports != 0) {
            Subject subject = getClientSubject();
            SubjectHelper subjectHelper = new SubjectHelper();
            int targetServerSupportedTypes = tsssasMechConfig.getSupportedIdentityTypes();

            if (subjectHelper.isUnauthenticated(subject)) {
                identityToken = getAnonymousIdentityToken(targetServerSupportedTypes);
            } else {
                identityToken = getIdentityTokenBasedOnConfiguredTypes(targetServerSupportedTypes);
            }
        }
        return identityToken.encodeIdentityToken(codec);
    }

    protected Subject getClientSubject() {
        SubjectManager subjectManager = new SubjectManager();
        Subject subject = subjectManager.getInvocationSubject();
        if (subject == null) {
            subject = subjectManager.getCallerSubject();
        }
        return subject;
    }

    private CSSSASIdentityToken getAnonymousIdentityToken(int targetServerSupportedTypes) {
        CSSSASIdentityToken identityToken = null;
        if (idTokens.get(ITTAnonymous.value) != null) {
            if ((targetServerSupportedTypes & ITTAnonymous.value) != 0) {
                identityToken = idTokens.get(ITTAnonymous.value);
            } else {
                debugAndThrowNoPermissionException("The client cannot create the ITTAnonymous identity assertion token because it is not supported by the configuration of the remote server.",
                                                   "CSIv2_CLIENT_ANONYMOUS_ASSERTION_NOT_SUPPORTED_BY_SERVER",
                                                   "CWWKS9545E: The client cannot create the ITTAnonymous identity assertion token because it is not supported by the configuration of the remote server.");
            }
        } else {
            debugAndThrowNoPermissionException("The client cannot create the ITTAnonymous identity assertion token because it is not supported by the configuration of this client.",
                                               "CSIv2_CLIENT_ANONYMOUS_ASSERTION_NOT_SUPPORTED_BY_CLIENT",
                                               "CWWKS9546E: The client cannot create the ITTAnonymous identity assertion token because it is not supported by the configuration of this client.");
        }
        return identityToken;
    }

    private void debugAndThrowNoPermissionException(String debugMsg, String formattedMsgKey, String backupMsg) throws NO_PERMISSION {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, debugMsg);
        }
        String messageFromBundle = TraceNLS.getFormattedMessage(this.getClass(),
                                                                TraceConstants.MESSAGE_BUNDLE,
                                                                formattedMsgKey,
                                                                new Object[] {},
                                                                backupMsg);
        throw new org.omg.CORBA.NO_PERMISSION(messageFromBundle, SecurityMinorCodes.SECURITY_MECHANISM_NOT_SUPPORTED, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
    }

    private CSSSASIdentityToken getIdentityTokenBasedOnConfiguredTypes(int targetServerSupportedTypes) {
        CSSSASIdentityToken identityToken = null;

        for (String assertionType : configuredTypes) {
            int tokenType = stringToType(assertionType);
            if (canPerformAssertionWith(tokenType, targetServerSupportedTypes)) {
                identityToken = idTokens.get(tokenType);
                break;
            }
        }

        ensureThereIsAnIdentityToken(identityToken);
        return identityToken;
    }

    private int stringToType(String typeString) {
        int result = 0;
        if (TYPE_ITTAnonymous.equals(typeString)) {
            result = ITTAnonymous.value;
        } else if (TYPE_ITTPrincipalName.equals(typeString)) {
            result = ITTPrincipalName.value;
        } else if (TYPE_ITTX509CertChain.equals(typeString)) {
            result = ITTX509CertChain.value;
        } else if (TYPE_ITTDistinguishedName.equals(typeString)) {
            result = ITTDistinguishedName.value;
        }
        return result;
    }

    private boolean canPerformAssertionWith(int tokenType, int targetServerSupportedTypes) {
        return ((supportedIdentityTypes & tokenType) != 0) && ((targetServerSupportedTypes & tokenType) != 0);
    }

    private void ensureThereIsAnIdentityToken(CSSSASIdentityToken identityToken) {
        if (identityToken == null) {
            String messageFromBundle = null;
            if (supportedIdentityTypes == ITTAnonymous.value) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "The client cannot assert an authenticated subject because it supports identity assertions with ITTAnonymous only.");
                }
                messageFromBundle = TraceNLS.getFormattedMessage(this.getClass(),
                                                                 TraceConstants.MESSAGE_BUNDLE,
                                                                 "CSIv2_CLIENT_ANONYMOUS_ASSERTION_ONLY",
                                                                 new Object[] {},
                                                                 "CWWKS9547E: The client cannot assert an authenticated subject because it supports identity assertions with ITTAnonymous only.");
            } else {
                String configuredTypesExcludingAnonymous = typesToString(supportedIdentityTypes & (ITTPrincipalName.value | ITTX509CertChain.value | ITTDistinguishedName.value)); // Exclude ITTAnonymous
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "The client cannot assert an authenticated subject because the configuration of the remote server does not support identity assertions with types:",
                             configuredTypesExcludingAnonymous);
                }
                messageFromBundle = TraceNLS.getFormattedMessage(this.getClass(),
                                                                 TraceConstants.MESSAGE_BUNDLE,
                                                                 "CSIv2_CLIENT_ASSERTION_TYPE_NOT_SUPPORTED",
                                                                 new Object[] { configuredTypesExcludingAnonymous },
                                                                 "CWWKS9548E: The client cannot assert an authenticated subject because the configuration of the remote server does not support identity assertions with types <{0}>.");
            }

            throw new org.omg.CORBA.NO_PERMISSION(messageFromBundle, SecurityMinorCodes.SECURITY_MECHANISM_NOT_SUPPORTED, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
    }

    @Trivial
    private String typesToString(int type) {
        String result = "";
        final String SPACE = " ";

        if ((ITTAnonymous.value & type) != 0) {
            result += TYPE_ITTAnonymous + SPACE;
        }
        if ((ITTPrincipalName.value & type) != 0) {
            result += TYPE_ITTPrincipalName + SPACE;
        }
        if ((ITTX509CertChain.value & type) != 0) {
            result += TYPE_ITTX509CertChain + SPACE;
        }
        if ((ITTDistinguishedName.value & type) != 0) {
            result += TYPE_ITTDistinguishedName;
        }

        if (result.isEmpty()) {
            return "None";
        }

        return result.trim();
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
        buf.append(spaces).append("CSSSASMechConfig: [\n");
        buf.append(moreSpaces).append("SUPPORTS: ").append(ConfigUtil.flags(supports)).append("\n");
        buf.append(moreSpaces).append("REQUIRES: ").append(ConfigUtil.flags(requires)).append("\n");
        if (identityToken != null) {
            identityToken.toString(moreSpaces, buf);
        }
        buf.append("\n");
        for (Iterator<CSSSASIdentityToken> iterator = idTokens.values().iterator(); iterator.hasNext();) {
            CSSSASIdentityToken identityToken = iterator.next();
            identityToken.toString(moreSpaces, buf);
        }
        buf.append(spaces).append("]\n");
    }

    /**
     * @return true if this attribute layer is asserting other than ITTAbsent
     */
    public boolean isAsserting() {
        // TODO Right now trustedIdentity is only set if there is an assertion other than ITTAbsent.
        return trustedIdentity != null;
    }

    /**
     * @param trustedIdentity
     */
    public void setTrustedIdentity(String trustedIdentity) {
        this.trustedIdentity = trustedIdentity;
    }

    public String getTrustedIdentity() {
        return trustedIdentity;
    }

    /**
     * @param trustedPassword
     */
    public void setTrustedPassword(SerializableProtectedString trustedPassword) {
        this.trustedPassword = trustedPassword;
    }

    public SerializableProtectedString getTrustedPassword() {
        return trustedPassword;
    }

    /**
     * @return the supportedIdentityTypes
     */
    public int getSupportedIdentityTypes() {
        return supportedIdentityTypes;
    }

    public boolean isAssertingITTAbsent(TSSSASMechConfig tsssasMechConfig) {
        int targetServerSupportedTypes = tsssasMechConfig.getSupportedIdentityTypes();
        return ((supportedIdentityTypes & targetServerSupportedTypes) == 0);
    }

    private void buildSupportsFailedMsg(TSSSASMechConfig sasMech, String clientMech) {
        if (!clientMech.equalsIgnoreCase(CSSNULLASMechConfig.mechanism)) {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_ATTRIBUTE_SUPPORTS_FAILED",
                                                         new Object[] { clientMech, ConfigUtil.flags(supports), ConfigUtil.flags(sasMech.getRequires()) },
                                                         "CWWKS9559E: The client security policy has the attribute layer configured for {0} with <{1}> as Supported in the server.xml file and the server security policy is configured with <{2}> as Required.");
        } else {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_ATTRIBUTE_SUPPORTS_NO_AUTH_FAILED",
                                                         new Object[] { ConfigUtil.flags(supports), ConfigUtil.flags(sasMech.getRequires()) },
                                                         "CWWKS9560E: The client security policy has the attribute layer configured with <{0}> as Supported in the server.xml file and the server security policy is configured with <{1}> as Required.");
        }
    }

    private void buildRequiresFailedMsg(TSSSASMechConfig sasMech, String clientMech) {
        if (!clientMech.equalsIgnoreCase(CSSNULLASMechConfig.mechanism)) {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_ATTRIBUTE_REQUIRES_FAILED",
                                                         new Object[] { clientMech, ConfigUtil.flags(requires), ConfigUtil.flags(sasMech.getSupports()) },
                                                         "CWWKS9561E: The client security policy has the attribute layer configured for {0} with <{1}> as Required in the server.xml file and the server security policy is configured with <{2}> as Supported.");
        } else {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_ATTRIBUTE_REQUIRES_NO_AUTH_FAILED",
                                                         new Object[] { ConfigUtil.flags(requires), ConfigUtil.flags(sasMech.getSupports()) },
                                                         "CWWKS9562E: The client security policy has the attribute layer configured with <{0}> as Required in the server.xml file and the server security policy is configured with <{1}> as Supported.");
        }
    }

    private void buildIdentityAssertionFailedMsg(TSSSASMechConfig sasMech, String clientMech) {
        if (!clientMech.equalsIgnoreCase(CSSNULLASMechConfig.mechanism)) {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_ATTRIBUTE_IDENTITY_ASSERTION_FAILED",
                                                         new Object[] { clientMech, ConfigUtil.flags(supports), ConfigUtil.flags(sasMech.getRequires()) },
                                                         "CWWKS9563E: The client security policy has the attribute layer configured for <{0}> with identity assertion type <{1}> in the server.xml file and the server security policy is configured with identity assertion type <{2}>.");
        } else {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_ATTRIBUTE_IDENTITY_ASSERTION_NO_AUTH_FAILED",
                                                         new Object[] { ConfigUtil.flags(supports), ConfigUtil.flags(sasMech.getRequires()) },
                                                         "CWWKS9564E: The client security policy has the attribute layer configured with identity assertion type <{0}> in the server.xml file and the server security policy is configured with identity assertion type <{1}>.");
        }
    }

}
