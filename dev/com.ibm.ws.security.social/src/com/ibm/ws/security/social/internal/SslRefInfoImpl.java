/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
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
import com.ibm.ws.security.social.SslRefInfo;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
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
        this.sslSupport = sslSupport;
        this.sslRef = sslRef;
        this.keyStoreServiceRef = keyStoreServiceRef;
        this.keyAliasName = keyAliasName;
    }

    @Override
    public String getTrustStoreName() throws SocialLoginException {
        if (sslTrustStoreName == null) {
            init();
        }
        return sslTrustStoreName;
    }

    @Override
    public String getKeyStoreName() throws SocialLoginException {
        if (sslKeyStoreName == null) {
            init();
        }
        return sslKeyStoreName;
    }

    // init when needed
    void init() throws SocialLoginException {
        if (sslSupport != null) {
            Properties sslProps = null;
            this.jsseHelper = sslSupport.getJSSEHelper();
            if (this.jsseHelper != null) {
                try {
                    if (sslRef != null) {
                        sslProps = this.jsseHelper.getProperties(sslRef); // SSLConfig
                    } else {
                        Map<String, Object> connectionInfo = new HashMap<String, Object>();
                        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_INBOUND);
                        sslProps = this.jsseHelper.getProperties(null, connectionInfo, null, true); // default
                        // SSL
                    }
                } catch (SSLException e) {
                    throw new SocialLoginException("ERROR_LOADING_SSL_PROPS", e, new Object[] { e.getLocalizedMessage() });
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "sslConfig (" + sslRef + ") get: " + sslProps);
                }
                if (sslProps != null) {
                    this.sslKeyStoreName = sslProps.getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_KEY_STORE_NAME);
                    this.sslTrustStoreName = sslProps.getProperty(com.ibm.websphere.ssl.Constants.SSLPROP_TRUST_STORE_NAME);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "sslTrustStoreName: " + this.sslTrustStoreName);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws SocialLoginException
     */
    @Override
    public HashMap<String, PublicKey> getPublicKeys() throws SocialLoginException {
        if (this.jsseHelper == null) {
            init();
        }
        // TODO due to dynamic changes on keyStore, we have to load the public
        // keys everytime.
        HashMap<String, PublicKey> publicKeys = new HashMap<String, PublicKey>();
        if (this.sslTrustStoreName != null) {
            KeyStoreService keyStoreService = keyStoreServiceRef.getService();
            if (keyStoreService == null) {
                throw new SocialLoginException("KEYSTORE_SERVICE_NOT_FOUND", null, new Object[0]);
            }
            Collection<String> names = null;
            try {
                names = keyStoreService.getTrustedCertEntriesInKeyStore(sslTrustStoreName);
            } catch (KeyStoreException e) {
                throw new SocialLoginException("ERROR_LOADING_KEYSTORE_CERTIFICATES", e, new Object[] { sslTrustStoreName, e.getLocalizedMessage() });
            }
            Iterator<String> aliasNames = names.iterator();
            while (aliasNames.hasNext()) {
                String aliasName = aliasNames.next();
                PublicKey publicKey = null;
                try {
                    publicKey = keyStoreService.getCertificateFromKeyStore(sslTrustStoreName, aliasName).getPublicKey();
                } catch (GeneralSecurityException e) {
                    throw new SocialLoginException("ERROR_LOADING_CERTIFICATE", e, new Object[] { aliasName, sslTrustStoreName, e.getLocalizedMessage() });
                }
                publicKeys.put(aliasName, publicKey);
            }
        }

        return publicKeys;
    }

    /** {@inheritDoc} */
    @FFDCIgnore({ SocialLoginException.class })
    @Override
    public PublicKey getPublicKey() throws SocialLoginException {
        if (this.jsseHelper == null) {
            init();
        }
        if (sslKeyStoreName != null) {
            if (keyAliasName != null && keyAliasName.trim().isEmpty() == false) {
                KeyStoreService keyStoreService = keyStoreServiceRef.getService();
                if (keyStoreService == null) {
                    throw new SocialLoginException("KEYSTORE_SERVICE_NOT_FOUND", null, new Object[0]);
                }
                // TODO: Determine if the first public key should be used if a public key is not found for the key alias.
                try {
                    return keyStoreService.getCertificateFromKeyStore(sslKeyStoreName, keyAliasName).getPublicKey();
                } catch (GeneralSecurityException e) {
                    throw new SocialLoginException("ERROR_LOADING_CERTIFICATE", e, new Object[] { keyAliasName, sslTrustStoreName, e.getLocalizedMessage() });
                }
            } else {
                Iterator<Entry<String, PublicKey>> publicKeysIterator = null;
                try {
                    // Get first public key
                    publicKeysIterator = getPublicKeys().entrySet().iterator();
                } catch (SocialLoginException e) {
                    throw new SocialLoginException("ERROR_LOADING_GETTING_PUBLIC_KEYS", e, new Object[] { keyAliasName, sslTrustStoreName, e.getLocalizedMessage() });
                }
                if (publicKeysIterator.hasNext()) {
                    return publicKeysIterator.next().getValue();
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PrivateKey getPrivateKey() throws SocialLoginException {
        if (this.jsseHelper == null) {
            init();
        }
        if (sslKeyStoreName != null) {
            KeyStoreService keyStoreService = keyStoreServiceRef.getService();
            if (keyStoreService == null) {
                throw new SocialLoginException("KEYSTORE_SERVICE_NOT_FOUND", null, new Object[0]);
            }
            if (keyAliasName != null && keyAliasName.trim().isEmpty() == false) {
                // TODO: Determine if the first private key should be used if a private key is not found for the key alias.
                try {
                    return keyStoreService.getPrivateKeyFromKeyStore(sslKeyStoreName, keyAliasName, null);
                } catch (GeneralSecurityException e) {
                    throw new SocialLoginException("ERROR_LOADING_SPECIFIC_PRIVATE_KEY", e, new Object[] { keyAliasName, sslKeyStoreName, e.getLocalizedMessage() });
                }
            } else {
                // Get first public key
                try {
                    return keyStoreService.getPrivateKeyFromKeyStore(sslKeyStoreName);
                } catch (GeneralSecurityException e) {
                    throw new SocialLoginException("ERROR_LOADING_PRIVATE_KEY", e, new Object[] { sslKeyStoreName, e.getLocalizedMessage() });
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SecretKey getSecretKey() throws SocialLoginException {
        if (this.jsseHelper == null) {
            init();
        }
        if (sslKeyStoreName != null) {
            if (keyAliasName != null && keyAliasName.trim().isEmpty() == false) {
                KeyStoreService keyStoreService = keyStoreServiceRef.getService();
                if (keyStoreService == null) {
                    throw new SocialLoginException("KEYSTORE_SERVICE_NOT_FOUND", null, new Object[0]);
                }
                try {
                    return keyStoreService.getSecretKeyFromKeyStore(sslKeyStoreName, keyAliasName, null);
                } catch (GeneralSecurityException e) {
                    throw new SocialLoginException("ERROR_LOADING_SECRET_KEY", e, new Object[] { keyAliasName, sslKeyStoreName, e.getLocalizedMessage() });
                }
            }
        }
        return null;
    }

}
