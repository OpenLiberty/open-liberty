/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;

import javax.crypto.SecretKey;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.ws.ssl.config.WSKeyStore;

/**
 *
 */
@Component(service = KeyStoreService.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class KeyStoreServiceImpl implements KeyStoreService {
    // Intentionally left package protected for unit test
    KeyStoreManager ksMgr;
    private static final TraceComponent tc = Tr.register(KeyStoreServiceImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public KeyStoreServiceImpl() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "<init>");
        }
    }

    @Reference(service = KeystoreConfig.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(id=*)")
    protected void setKeyStore(KeystoreConfig config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "set KeystoreConfig: " + config);
        }
    }

    protected void unsetKeyStore(KeystoreConfig config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Unset KeystoreConfig: " + config);
        }
    }

    protected void updateKeyStore(KeystoreConfig config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "update KeystoreConfig: " + config);
        }
    }

    protected void activate() {
        ksMgr = KeyStoreManager.getInstance();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "activate");
        }
    }

    protected void deactivate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "deactivate");
        }
    }

    @Override
    public String[] getAllKeyStoreAliases() {
        String[] aliases = KeyStoreManager.getInstance().getKeyStoreAliases();
        // each alias has two entries, one by name and one by pid, strip the pid entry
        ArrayList<String> alist = new ArrayList<String>();
        for (int i = 0; i < aliases.length; i++) {
            String entry = aliases[i];
            if (!entry.startsWith("com.ibm.ws.ssl.keystore_")) {
                alist.add(aliases[i]);
            }
        }
        return alist.toArray(new String[] {});
    }

    @Override
    public int getKeyStoreCount() {
        return ksMgr.getKeyStoreCount();
    }

    /** {@inheritDoc} */
    @Override
    public String getKeyStoreLocation(String keyStoreName) throws KeyStoreException {
        WSKeyStore ks = ksMgr.getKeyStore(keyStoreName);
        if (ks != null) {
            return ks.getLocation();
        } else {
            throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getTrustedCertEntriesInKeyStore(String keyStoreName) throws KeyStoreException {
        try {
            KeyStore ks = ksMgr.getJavaKeyStore(keyStoreName);
            if (ks != null) {
                Collection<String> trustedCerts = new HashSet<String>();
                Enumeration<String> aliases = ks.aliases();
                if (aliases != null) {
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        if (ks.isCertificateEntry(alias)) {
                            trustedCerts.add(alias);
                        }
                    }
                }
                return trustedCerts;
            } else {
                throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
            }
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the request trusted certificate entries from keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Certificate getCertificateFromKeyStore(String keyStoreName, String alias) throws KeyStoreException, CertificateException {
        try {
            KeyStore ks = ksMgr.getJavaKeyStore(keyStoreName);
            if (ks == null) {
                throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
            } else {
                if (!ks.isCertificateEntry(alias) && !ks.isKeyEntry(alias)) {
                    throw new CertificateException("The alias [" + alias + "] is not present in the KeyStore as a certificate entry");
                } else {
                    return ks.getCertificate(alias);
                }
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the request Certificate for alias [" + alias + "] from keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Certificate[] getCertificateChainFromKeyStore(String keyStoreName, String alias) throws KeyStoreException, CertificateException {
        try {
            KeyStore ks = ksMgr.getJavaKeyStore(keyStoreName);
            if (ks == null) {
                throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
            } else {
                if (!ks.isCertificateEntry(alias) && !ks.isKeyEntry(alias)) {
                    throw new CertificateException("The alias [" + alias + "] is not present in the KeyStore as a certificate entry");
                } else {
                    return ks.getCertificateChain(alias);
                }
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the request Certificate chain for alias [" + alias + "] from keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public X509Certificate getX509CertificateFromKeyStore(String keyStoreName, String alias) throws KeyStoreException, CertificateException {
        try {
            KeyStore ks = ksMgr.getJavaKeyStore(keyStoreName);
            if (ks == null) {
                throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
            } else {
                if (!ks.isCertificateEntry(alias) && !ks.isKeyEntry(alias)) {
                    throw new CertificateException("The alias [" + alias + "] is not present in the KeyStore as a certificate entry");
                } else {
                    Certificate cert = ks.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        return (X509Certificate) cert;
                    } else {
                        throw new CertificateException("The alias [" + alias + "] is not an instance of X509Certificate");
                    }
                }
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the request X509Certificate for alias [" + alias + "] from keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PrivateKey getPrivateKeyFromKeyStore(String keyStoreName, String alias,
                                                @Sensitive String keyPassword) throws KeyStoreException, CertificateException {
        try {
            WSKeyStore wsKS = ksMgr.getKeyStore(keyStoreName);
            if (wsKS == null) {
                throw new KeyStoreException("The WSKeyStore [" + keyStoreName + "] is not present in the configuration");
            }

            Key key = wsKS.getKey(alias, keyPassword);
            if (key instanceof PrivateKey) {
                return (PrivateKey) key;
            } else {
                throw new CertificateException("The alias [" + alias + "] is not an instance of PrivateKey");
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the requested private key for alias [" + alias + "] from keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PrivateKey getPrivateKeyFromKeyStore(String keyStoreName) throws KeyStoreException, CertificateException {
        try {
            WSKeyStore wsKS = ksMgr.getKeyStore(keyStoreName);
            if (wsKS == null) {
                throw new KeyStoreException("The WSKeyStore [" + keyStoreName + "] is not present in the configuration");
            }

            String keyAlias = null;
            Enumeration<String> aliases = wsKS.aliases();
            if (aliases != null) {
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if (wsKS.isKeyEntry(alias)) {
                        if (keyAlias != null)
                            return null;
                        else
                            keyAlias = alias;
                    }
                }
            }

            if (keyAlias != null) {
                Key key = wsKS.getKey(keyAlias, null);
                if (key instanceof PrivateKey) {
                    return (PrivateKey) key;
                } else {
                    throw new CertificateException("The alias [" + keyAlias + "] is not an instance of PrivateKey");
                }
            } else {
                return null;
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the private key for alias from keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public X509Certificate getX509CertificateFromKeyStore(String keyStoreName) throws KeyStoreException, CertificateException {
        try {
            KeyStore ks = ksMgr.getJavaKeyStore(keyStoreName);
            if (ks == null) {
                throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
            } else {
                String keyAlias = null;
                Enumeration<String> aliases = ks.aliases();
                if (aliases != null) {
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        if (ks.isKeyEntry(alias)) {
                            if (keyAlias != null)
                                return null;
                            else
                                keyAlias = alias;
                        }
                    }
                }
                if (keyAlias != null) {
                    WSKeyStore wsKS = ksMgr.getKeyStore(keyStoreName);
                    if (wsKS == null) {
                        throw new CertificateException("The WSKeyStore [" + keyStoreName + "] is not present in the configuration");
                    }

                    // get the private key
                    Certificate cert = ks.getCertificate(keyAlias);
                    if (cert instanceof X509Certificate) {
                        return (X509Certificate) cert;
                    } else {
                        throw new CertificateException("The alias [" + keyAlias + "] is not an instance of X509Certificate");
                    }
                }
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (SSLException e) {
            throw new KeyStoreException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the private key for alias from keystore: " + keyStoreName, e);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addCertificateToKeyStore(String keyStoreName, String alias, Certificate certificate) throws KeyStoreException, CertificateException {
        try {
            WSKeyStore wks = ksMgr.getKeyStore(keyStoreName);
            if (wks == null) {
                throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
            } else {
                wks.setCertificateEntry(alias, certificate);
            }
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while adding the Certificate for alias [" + alias + "] to keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setKeyEntryToKeyStore(String keyStoreName, String alias, Key key, Certificate[] chain) throws KeyStoreException, CertificateException {
        try {
            WSKeyStore wks = ksMgr.getKeyStore(keyStoreName);
            if (wks == null) {
                throw new KeyStoreException("The keystore [" + keyStoreName + "] is not present in the configuration");
            } else {
                wks.setKeyEntry(alias, key, chain);
            }
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while setting the key entry for alias [" + alias + "] to keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public SecretKey getSecretKeyFromKeyStore(String keyStoreName, String alias, @Sensitive String keyPassword) throws KeyStoreException, CertificateException {
        try {
            WSKeyStore wsKS = ksMgr.getKeyStore(keyStoreName);
            if (wsKS == null) {
                throw new KeyStoreException("The WSKeyStore [" + keyStoreName + "] is not present in the configuration");
            }

            Key key = wsKS.getKey(alias, keyPassword);
            if (key instanceof SecretKey) {
                return (SecretKey) key;
            } else {
                throw new CertificateException("The alias [" + alias + "] is not an instance of SecretKey");
            }
        } catch (CertificateException e) {
            throw e;
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the requested secret key for alias [" + alias + "] from keystore: " + keyStoreName, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public X509Certificate getClientKeyCert(String sslConfigAlias) throws KeyStoreException, CertificateException, SSLException {
        JSSEHelper jsseHelper = JSSEHelper.getInstance();
        Properties sslProps = jsseHelper.getProperties(sslConfigAlias);

        return getClientKeyCert(sslProps);
    }

    /** {@inheritDoc} */
    @Override
    public X509Certificate getClientKeyCert(Properties sslProps) throws KeyStoreException, CertificateException {

        if (sslProps != null && !sslProps.isEmpty()) {
            String keyStoreName = sslProps.getProperty(Constants.SSLPROP_KEY_STORE_NAME);

            String certAlias = sslProps.getProperty(Constants.SSLPROP_KEY_STORE_CLIENT_ALIAS);
            if (certAlias != null) {
                // client alias is in properties so get the certificate from the keystore
                return getX509CertificateFromKeyStore(keyStoreName, certAlias);
            } else {
                return getX509CertificateFromKeyStore(keyStoreName);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public KeyStore getKeyStore(String keyStoreName) throws KeyStoreException {
        KeyStore ks = null;
        try {
            ks = ksMgr.getJavaKeyStore(keyStoreName);
        } catch (Exception e) {
            throw new KeyStoreException("Unexpected error while loading the keystore [" + keyStoreName + "]", e);
        }
        return ks;
    }
}
