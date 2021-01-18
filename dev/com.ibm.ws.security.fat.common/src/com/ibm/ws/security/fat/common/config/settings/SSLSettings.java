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
package com.ibm.ws.security.fat.common.config.settings;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

public class SSLSettings extends BaseConfigSettings {

    private static final Class<?> thisClass = SSLSettings.class;

    public static final String CONFIG_ELEMENT_NAME = "ssl";

    public static final String ATTR_SSL_ID = "id";
    public static final String ATTR_SSL_KEY_STORE_REF = "keyStoreRef";
    public static final String ATTR_SSL_TRUST_STORE_REF = "trustStoreRef";
    public static final String ATTR_SSL_SSL_PROTOCOL = "sslProtocol";
    public static final String ATTR_SSL_CLIENT_AUTHENTICATION = "clientAuthentication";
    public static final String ATTR_SSL_CLIENT_AUTHENTICATION_SUPPORTED = "clientAuthenticationSupported";
    public static final String ATTR_SSL_SECURITY_LEVEL = "securityLevel";
    public static final String ATTR_SSL_CLIENT_KEY_ALIAS = "clientKeyAlias";
    public static final String ATTR_SSL_SERVER_KEY_ALIAS = "serverKeyAlias";
    public static final String ATTR_SSL_ENABLED_CIPHERS = "enabledCiphers";

    private String id = null;
    private String keyStoreRef = null;
    private String trustStoreRef = null;
    private String sslProtocol = null;
    private String clientAuthentication = null;
    private String clientAuthenticationSupported = null;
    private String securityLevel = null;
    private String clientKeyAlias = null;
    private String serverKeyAlias = null;
    private String enabledCiphers = null;

    public SSLSettings() {
        configElementName = CONFIG_ELEMENT_NAME;
    }

    public SSLSettings(String id, String keyStoreRef, String trustStoreRef, String sslProtocol, String clientAuthentication,
                       String clientAuthenticationSupported, String securityLevel, String clientKeyAlias, String serverKeyAlias, String enabledCiphers) {

        configElementName = CONFIG_ELEMENT_NAME;

        this.id = id;
        this.keyStoreRef = keyStoreRef;
        this.trustStoreRef = trustStoreRef;
        this.sslProtocol = sslProtocol;
        this.clientAuthentication = clientAuthentication;
        this.clientAuthenticationSupported = clientAuthenticationSupported;
        this.securityLevel = securityLevel;
        this.clientKeyAlias = clientKeyAlias;
        this.serverKeyAlias = serverKeyAlias;
        this.enabledCiphers = enabledCiphers;
    }

    @Override
    public SSLSettings createShallowCopy() {
        return new SSLSettings(id, keyStoreRef, trustStoreRef, sslProtocol, clientAuthentication, clientAuthenticationSupported, securityLevel, clientKeyAlias, serverKeyAlias, enabledCiphers);
    }

    @Override
    public SSLSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigAttributesMap() {
        String method = "getConfigAttributesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of SSL attributes");
        }
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put(ATTR_SSL_ID, getId());
        attributes.put(ATTR_SSL_KEY_STORE_REF, getKeyStoreRef());
        attributes.put(ATTR_SSL_TRUST_STORE_REF, getTrustStoreRef());
        attributes.put(ATTR_SSL_SSL_PROTOCOL, getSslProtocol());
        attributes.put(ATTR_SSL_CLIENT_AUTHENTICATION, getClientAuthentication());
        attributes.put(ATTR_SSL_CLIENT_AUTHENTICATION_SUPPORTED, getClientAuthenticationSupported());
        attributes.put(ATTR_SSL_SECURITY_LEVEL, getSecurityLevel());
        attributes.put(ATTR_SSL_CLIENT_KEY_ALIAS, getClientKeyAlias());
        attributes.put(ATTR_SSL_SERVER_KEY_ALIAS, getServerKeyAlias());
        attributes.put(ATTR_SSL_ENABLED_CIPHERS, getEnabledCiphers());

        return attributes;
    }

    /********************************* Private member getters and setters *********************************/

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setKeyStoreRef(String keyStoreRef) {
        this.keyStoreRef = keyStoreRef;
    }

    public String getKeyStoreRef() {
        return this.keyStoreRef;
    }

    public void setTrustStoreRef(String trustStoreRef) {
        this.trustStoreRef = trustStoreRef;
    }

    public String getTrustStoreRef() {
        return this.trustStoreRef;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public String getSslProtocol() {
        return this.sslProtocol;
    }

    public void setClientAuthentication(String clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
    }

    public String getClientAuthentication() {
        return this.clientAuthentication;
    }

    public void setClientAuthenticationSupported(String clientAuthenticationSupported) {
        this.clientAuthenticationSupported = clientAuthenticationSupported;
    }

    public String getClientAuthenticationSupported() {
        return this.clientAuthenticationSupported;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getSecurityLevel() {
        return this.securityLevel;
    }

    public void setClientKeyAlias(String clientKeyAlias) {
        this.clientKeyAlias = clientKeyAlias;
    }

    public String getClientKeyAlias() {
        return this.clientKeyAlias;
    }

    public void setServerKeyAlias(String serverKeyAlias) {
        this.serverKeyAlias = serverKeyAlias;
    }

    public String getServerKeyAlias() {
        return this.serverKeyAlias;
    }

    public void setEnabledCiphers(String enabledCiphers) {
        this.enabledCiphers = enabledCiphers;
    }

    public String getEnabledCiphers() {
        return this.enabledCiphers;
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, "SSL config settings:");
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_ID + ": " + id);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_KEY_STORE_REF + ": " + keyStoreRef);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_TRUST_STORE_REF + ": " + trustStoreRef);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_SSL_PROTOCOL + ": " + sslProtocol);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_CLIENT_AUTHENTICATION + ": " + clientAuthentication);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_CLIENT_AUTHENTICATION_SUPPORTED + ": " + clientAuthenticationSupported);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_SECURITY_LEVEL + ": " + securityLevel);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_CLIENT_KEY_ALIAS + ": " + clientKeyAlias);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_SERVER_KEY_ALIAS + ": " + serverKeyAlias);
        Log.info(thisClass, thisMethod, indent + ATTR_SSL_ENABLED_CIPHERS + ": " + enabledCiphers);
    }
}
