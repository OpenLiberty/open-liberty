/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.internal;

import java.util.ResourceBundle;

public class WSSecurityConstants {

    public static final String TR_GROUP = "WSSecurity";

    public static final String TR_RESOURCE_BUNDLE = "com.ibm.ws.wssecurity.resources.WSSecurityMessages";

    public static final ResourceBundle messages = ResourceBundle.getBundle(TR_RESOURCE_BUNDLE);

    public static final String WSSEC = "ws-security";
    public static final String SEC = "security"; //v3
    public static final String CXF_USER_NAME = WSSEC + ".username";
    public static final String CXF_USER_PASSWORD = WSSEC + ".password";
    public static final String CXF_CBH = WSSEC + ".callback-handler";

    public static final String WSS4J_KS_TYPE = "org.apache.ws.security.crypto.merlin.keystore.type";
    public static final String WSS4J_KS_PASSWORD = "org.apache.ws.security.crypto.merlin.keystore.password";
    public static final String WSS4J_KEY_PASSWORD = "org.apache.ws.security.crypto.merlin.keystore.private.password";
    public static final String WSS4J_KS_ALIAS = "org.apache.ws.security.crypto.merlin.keystore.alias";
    public static final String WSS4J_KS_FILE = "org.apache.ws.security.crypto.merlin.keystore.file";
    public static final String WSS4J_CRYPTO_PROVIDER = "org.apache.ws.security.crypto.provider";

    public static final String WSS4J_TS_PASSWORD = "org.apache.ws.security.crypto.merlin.truststore.password";

    public static final String CXF_SIG_PROPS = WSSEC + ".signature.properties";
    public static final String CXF_ENC_PROPS = WSSEC + ".encryption.properties";
    public static final String CXF_NONCE_CACHE_CONFIG_FILE = WSSEC + ".cache.config.file";

    public static final String CXF_SAML_CALLBACK_HANDLER = WSSEC + ".saml-callback-handler";
    public static final String DEFAULT_SAML_CALLBACK_HANDLER = "com.ibm.ws.wssecurity.callback.Saml20PropagationCallbackHandler";

    public static final String CXF_SIG_CRYPTO = WSSEC + ".signature.crypto";
    public static final String CXF_ENC_CRYPTO = WSSEC + ".encryption.crypto";
    //v3
    public static final String SEC_SIG_CRYPTO = SEC + ".signature.crypto";
    public static final String SEC_ENC_CRYPTO = SEC + ".encryption.crypto";

    public static final String CALLER_CONFIG = "callerConfig";
    public static final String CALLER_NAME = "name";
    public static final String CALLER_ENDORSING_TOKEN = "endorsingSupportingToken";
    public static final String UNT_CALLER_NAME = "UsernameToken";
    public static final String X509_CALLER_NAME = "X509Token";
    public static final String SAML_CALLER_NAME = "SamlToken";

    public static final String KEY_wantAssertionsSigned = "wantAssertionsSigned";
    public static final String KEY_clockSkew = "clockSkew";
    public static final String KEY_requiredSubjectConfirmationMethod = "requiredSubjectConfirmationMethod";
    public static final String KEY_timeToLive = "timeToLive";
    public static final String KEY_audienceRestrictions = "audienceRestrictions";

}
