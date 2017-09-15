/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.crypto.SecretKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.mp.jwt.SslRefInfo;
import com.ibm.ws.security.mp.jwt.TraceConstants;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

public class SslRefInfoImpl implements SslRefInfo {
    public static final TraceComponent tc = Tr.register(SslRefInfoImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    SSLSupport sslSupport = null;
    String sslRef = null;
    JSSEHelper jsseHelper = null;
    String sslKeyStoreName = null;
    String sslTrustStoreName = null;
    private String keyAliasName = null;
    AtomicServiceReference<KeyStoreService> keyStoreServiceRef = null;

    public SslRefInfoImpl(SSLSupport sslSupport, AtomicServiceReference<KeyStoreService> keyStoreServiceRef, String sslRef, String keyAliasName) {
        String methodName = "<init>";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName, sslSupport, keyStoreServiceRef, sslRef, keyAliasName);
        }
        this.sslSupport = sslSupport;
        this.sslRef = sslRef;
        this.keyStoreServiceRef = keyStoreServiceRef;
        this.keyAliasName = keyAliasName;
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    @Override
    public String getTrustStoreName() throws MpJwtProcessingException {
        String methodName = "getTrustStoreName";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (sslTrustStoreName == null) {
            init();
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, sslTrustStoreName);
        }
        return sslTrustStoreName;
    }

    @Override
    public String getKeyStoreName() throws MpJwtProcessingException {
        String methodName = "getKeyStoreName";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (sslKeyStoreName == null) {
            init();
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, sslKeyStoreName);
        }
        return sslKeyStoreName;
    }

    // init when needed
    void init() throws MpJwtProcessingException {
        String methodName = "init";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        setUpJsseHelper();
        if (this.jsseHelper == null) {
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName);
            }
            return;
        }
        Properties sslProps = null;
        try {
            sslProps = getSslPropertiesFromJsseHelper();
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "ERROR_LOADING_SSL_PROPS", new Object[] { e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg, e);
        }
        setKeystoreAndTruststoreNames(sslProps);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    void setUpJsseHelper() {
        if (sslSupport == null) {
            return;
        }
        jsseHelper = sslSupport.getJSSEHelper();
    }

    Properties getSslPropertiesFromJsseHelper() throws SSLException {
        if (sslRef != null) {
            return getSslPropertiesFromSslRef();
        } else {
            return getSslPropertiesFromConnectionInfo();
        }
    }

    Properties getSslPropertiesFromSslRef() throws SSLException {
        return jsseHelper.getProperties(sslRef); // SSLConfig
    }

    Properties getSslPropertiesFromConnectionInfo() throws SSLException {
        String methodName = "getSslPropertiesFromConnectionInfo";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
        Properties props = jsseHelper.getProperties(null, connectionInfo, null, true);
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, props);
        }
        return props;
    }

    void setKeystoreAndTruststoreNames(Properties sslProps) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "sslConfig (" + sslRef + ") get: " + sslProps);
        }
        if (sslProps != null) {
            this.sslKeyStoreName = sslProps.getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_KEY_STORE_NAME);
            this.sslTrustStoreName = sslProps.getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_TRUST_STORE_NAME);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "sslTrustStoreName: " + this.sslTrustStoreName);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws MpJwtProcessingException
     */
    @Override
    public HashMap<String, PublicKey> getPublicKeys() throws MpJwtProcessingException {
        String methodName = "getPublicKeys";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (this.jsseHelper == null) {
            init();
        }
        // TODO due to dynamic changes on keyStore, we have to load the public keys every time.
        HashMap<String, PublicKey> publicKeys = new HashMap<String, PublicKey>();
        if (this.sslTrustStoreName == null) {
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, publicKeys);
            }
            return publicKeys;
        }
        try {
            publicKeys = getPublicKeysFromKeystore();
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "FAILED_TO_LOAD_PUBLIC_KEYS", new Object[] { sslTrustStoreName, e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg, e);
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, publicKeys);
        }
        return publicKeys;
    }

    HashMap<String, PublicKey> getPublicKeysFromKeystore() throws MpJwtProcessingException {
        KeyStoreService keyStoreService = getKeyStoreService();
        return getPublicKeysFromTrustedCertAliases(keyStoreService);
    }

    KeyStoreService getKeyStoreService() throws MpJwtProcessingException {
        KeyStoreService keyStoreService = keyStoreServiceRef.getService();
        if (keyStoreService == null) {
            String msg = Tr.formatMessage(tc, "KEYSTORE_SERVICE_NOT_FOUND");
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg);
        }
        return keyStoreService;
    }

    HashMap<String, PublicKey> getPublicKeysFromTrustedCertAliases(KeyStoreService keyStoreService) throws MpJwtProcessingException {
        Collection<String> names = getTrustedCertAliases(keyStoreService);
        if (names == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find any trusted certificate aliases in the keystore");
            }
            return new HashMap<String, PublicKey>();
        }
        return getPublicKeysFromAliasNames(keyStoreService, names);
    }

    Collection<String> getTrustedCertAliases(KeyStoreService keyStoreService) throws MpJwtProcessingException {
        try {
            return keyStoreService.getTrustedCertEntriesInKeyStore(sslTrustStoreName);
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "ERROR_LOADING_KEYSTORE_CERTIFICATES", new Object[] { sslTrustStoreName, e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg, e);
        }
    }

    HashMap<String, PublicKey> getPublicKeysFromAliasNames(KeyStoreService keyStoreService, Collection<String> names) throws MpJwtProcessingException {
        String methodName = "getPublicKeysFromAliasNames";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        HashMap<String, PublicKey> publicKeys = new HashMap<String, PublicKey>();
        if (names == null) {
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, publicKeys);
            }
            return publicKeys;
        }
        for (String aliasName : names) {
            PublicKey publicKey = getPublicKeyFromAlias(keyStoreService, aliasName);
            publicKeys.put(aliasName, publicKey);
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, publicKeys);
        }
        return publicKeys;
    }

    PublicKey getPublicKeyFromAlias(KeyStoreService keyStoreService, String aliasName) throws MpJwtProcessingException {
        try {
            return keyStoreService.getCertificateFromKeyStore(sslTrustStoreName, aliasName).getPublicKey();
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "ERROR_LOADING_CERTIFICATE", new Object[] { aliasName, sslTrustStoreName, e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg, e);
        }
    }

    /** {@inheritDoc} */
    @FFDCIgnore({ Exception.class })
    @Override
    public PublicKey getPublicKey() throws MpJwtProcessingException {
        String methodName = "getPublicKey";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        if (this.jsseHelper == null) {
            init();
        }
        if (sslKeyStoreName == null) {
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, null);
            }
            return null;
        }
        try {
            PublicKey key = getKeyFromKeyAliasOrFirstAvailable();
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, key);
            }
            return key;
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "FAILED_TO_LOAD_PUBLIC_KEY", new Object[] { sslKeyStoreName, e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg, e);
        }
    }

    PublicKey getKeyFromKeyAliasOrFirstAvailable() throws MpJwtProcessingException {
        if (isKeyAliasConfigured()) {
            return getKeyFromKeyAlias();
        } else {
            return getFirstAvailableKey();
        }
    }

    boolean isKeyAliasConfigured() {
        return keyAliasName != null && !keyAliasName.trim().isEmpty();
    }

    PublicKey getKeyFromKeyAlias() throws MpJwtProcessingException {
        KeyStoreService keyStoreService = getKeyStoreService();
        // TODO: Determine if the first public key should be used if a public key is not found for the key alias.
        try {
            return keyStoreService.getCertificateFromKeyStore(sslKeyStoreName, keyAliasName).getPublicKey();
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "ERROR_LOADING_CERTIFICATE", new Object[] { keyAliasName, sslTrustStoreName, e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw new MpJwtProcessingException(msg, e);
        }
    }

    PublicKey getFirstAvailableKey() throws MpJwtProcessingException {
        String methodName = "getFirstAvailableKey";
        if (tc.isDebugEnabled()) {
            Tr.entry(tc, methodName);
        }
        Iterator<Entry<String, PublicKey>> publicKeysIterator = null;
        try {
            // Get first public key
            publicKeysIterator = getPublicKeys().entrySet().iterator();
        } catch (MpJwtProcessingException e) {
            String msg = Tr.formatMessage(tc, "FAILED_TO_LOAD_FIRST_AVAILABLE_KEY", new Object[] { sslTrustStoreName, e.getLocalizedMessage() });
            Tr.error(tc, msg);
            throw e;
        }
        if (publicKeysIterator.hasNext()) {
            PublicKey key = publicKeysIterator.next().getValue();
            if (tc.isDebugEnabled()) {
                Tr.exit(tc, methodName, key);
            }
            return key;
        }
        if (tc.isDebugEnabled()) {
            Tr.exit(tc, methodName, null);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PrivateKey getPrivateKey() throws MpJwtProcessingException {
        //        if (this.jsseHelper == null) {
        //            init();
        //        }
        //        if (sslKeyStoreName != null) {
        //            KeyStoreService keyStoreService = keyStoreServiceRef.getService();
        //            if (keyStoreService == null) {
        //                String msg = Tr.formatMessage(tc, "KEYSTORE_SERVICE_NOT_FOUND");
        //                Tr.error(tc, msg);
        //                throw new MpJwtProcessingException(msg);
        //            }
        //            if (keyAliasName != null && keyAliasName.trim().isEmpty() == false) {
        //                // TODO: Determine if the first private key should be used if a private key is not found for the key alias.
        //                try {
        //                    return keyStoreService.getPrivateKeyFromKeyStore(sslKeyStoreName, keyAliasName, null);
        //                } catch (GeneralSecurityException e) {
        //                    String msg = Tr.formatMessage(tc, "ERROR_LOADING_SPECIFIC_PRIVATE_KEY", new Object[] { keyAliasName, sslKeyStoreName, e.getLocalizedMessage() });
        //                    Tr.error(tc,  msg);
        //                    throw new MpJwtProcessingException(msg, e);
        //                }
        //            } else {
        //                // Get first public key
        //                try {
        //                    return keyStoreService.getPrivateKeyFromKeyStore(sslKeyStoreName);
        //                } catch (GeneralSecurityException e) {
        //                    String msg = Tr.formatMessage(tc, "ERROR_LOADING_PRIVATE_KEY", new Object[] { sslKeyStoreName, e.getLocalizedMessage() });
        //                    Tr.error(tc,  msg);
        //                    throw new MpJwtProcessingException(msg, e);
        //                }
        //            }
        //        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SecretKey getSecretKey() throws MpJwtProcessingException {
        //        if (this.jsseHelper == null) {
        //            init();
        //        }
        //        if (sslKeyStoreName != null) {
        //            if (keyAliasName != null && keyAliasName.trim().isEmpty() == false) {
        //                KeyStoreService keyStoreService = keyStoreServiceRef.getService();
        //                if (keyStoreService == null) {
        //                    throw new MpJwtProcessingException("KEYSTORE_SERVICE_NOT_FOUND", null, new Object[0]);
        //                }
        //                try {
        //                    return keyStoreService.getSecretKeyFromKeyStore(sslKeyStoreName, keyAliasName, null);
        //                } catch (GeneralSecurityException e) {
        //                    throw new MpJwtProcessingException("ERROR_LOADING_SECRET_KEY", e, new Object[] { keyAliasName, sslKeyStoreName, e.getLocalizedMessage() });
        //                }
        //            }
        //        }
        return null;
    }

}
