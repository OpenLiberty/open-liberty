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
package com.ibm.ws.ssl;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Properties;

import javax.crypto.SecretKey;

import com.ibm.websphere.ssl.SSLException;

/**
 * Provides an interface into the contents of the configured keystores
 * for the server.
 * <p>
 * This will allow semi-restricted access to entries within the available
 * keystores for basic certificate management activities.
 */
public interface KeyStoreService {

    /**
     * Returns the location of the keystore.
     *
     * @param keyStoreName The keystore's configuration ID
     * @return the keystore location. {@code null} is not returned.
     * @throws KeyStoreException if the keystore does not exist in the configuration
     */
    String getKeyStoreLocation(String keyStoreName) throws KeyStoreException;

    /**
     * Returns the set of trusted cert entries in the keystore.
     *
     * @param keyStoreName The keystore's configuration ID
     * @return the collection of trusted cert entries. {@code null} is not
     *         returned, but the collection may be empty.
     * @throws KeyStoreException if the keystore does not exist in the configuration
     */
    Collection<String> getTrustedCertEntriesInKeyStore(String keyStoreName) throws KeyStoreException;

    /**
     * Loads the Certificate with the given alias from the specified
     * keystore.
     *
     * @param keyStoreName The keystore's configuration ID
     * @param alias the alias of the certificate to load
     * @return The Certificate for the given alias. {@code null} is not returned.
     * @throws KeyStoreException if the keystore does not exist in the configuration
     * @throws CertificateException if the specified alias does not exist
     */
    Certificate getCertificateFromKeyStore(String keyStoreName, String alias) throws KeyStoreException, CertificateException;

    /**
     * Loads the Certificate[] (chain) with the given alias from the specified
     * keystore.
     *
     * @param keyStoreName The keystore's configuration ID
     * @param alias the alias of the certificate to load
     * @return The Certificate[] (chain) for the given alias. {@code null} is not returned.
     * @throws KeyStoreException if the keystore does not exist in the configuration
     * @throws CertificateException if the specified alias does not exist
     */
    Certificate[] getCertificateChainFromKeyStore(String keyStoreName, String alias) throws KeyStoreException, CertificateException;

    /**
     * Loads the X509Certificate with the given alias from the specified
     * keystore.
     *
     * @param keyStoreName The keystore's configuration ID
     * @param alias the alias of the certificate to load
     * @return The X509Certificate for the given alias. {@code null} is not returned.
     * @throws KeyStoreException if the keystore does not exist in the configuration
     * @throws CertificateException if the specified alias does not exist or is not an X509 certificate
     */
    X509Certificate getX509CertificateFromKeyStore(String keyStoreName, String alias) throws KeyStoreException, CertificateException;

    /**
     * Loads the private key specified by the alias from the specified
     * keystore.
     * <p>
     * If keyPassword is null, the keystore password will be used.
     *
     * @param keyStoreName The keystore's configuration ID
     * @param alias the alias of the key to load
     * @param keyPassword the keystore password. The password may be encoded.
     * @return The PrivateKey for the given alias. {@code null} is not returned.
     * @throws KeyStoreException if the keystore does not exist in the configuration
     * @throws CertificateException
     */
    PrivateKey getPrivateKeyFromKeyStore(String keyStoreName, String alias, String keyPassword) throws KeyStoreException, CertificateException;

    /**
     * Add the Certificate with the given alias to the specified
     * keystore.
     *
     * @param keyStoreName The keystore's configuration ID
     * @param alias the alias of the certificate to add
     * @return The Certificate for the given alias
     * @throws KeyStoreException if the keystore does not exist in the configuration
     * @throws CertificateException if the specified alias does not exist
     */
    void addCertificateToKeyStore(String keyStoreName, String alias, Certificate certificate) throws KeyStoreException, CertificateException;

    /**
     * @param keyStoreName
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     */
    PrivateKey getPrivateKeyFromKeyStore(String keyStoreName) throws KeyStoreException, CertificateException;

    /**
     * @param keyStoreName
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     */
    X509Certificate getX509CertificateFromKeyStore(String keyStoreName) throws KeyStoreException, CertificateException;

    /**
     * Loads the secret key specified by the alias from the specified
     * keystore.
     * <p>
     * If keyPassword is null, the keystore password will be used.
     *
     * @param keyStoreName The keystore's configuration ID
     * @param alias the alias of the key to load
     * @param keyPassword the keystore password. The password may be encoded.
     * @return The SecretKey for the given alias. {@code null} is not returned.
     * @throws KeyStoreException if the keystore does not exist in the configuration
     * @throws CertificateException
     */
    SecretKey getSecretKeyFromKeyStore(String keyStoreName, String alias, String keyPassword) throws KeyStoreException, CertificateException;

    /**
     * Returns the client certificate which will either be the only key in the keystore
     * file or the key specified by the clientKeyAlias property from the ssl configuration.
     *
     * @param sslConfigAlias
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws SSLException
     */
    X509Certificate getClientKeyCert(String sslConfigAlias) throws KeyStoreException, CertificateException, SSLException;

    /**
     * Returns the client certificate which will either be the only key in the keystore
     * file or the key specified by the clientKeyAlias property from the ssl configuration.
     *
     * @param sslConfigAlias
     * @return
     * @throws KeyStoreException
     * @throws CertificateException
     */
    X509Certificate getClientKeyCert(Properties sslProps) throws KeyStoreException, CertificateException;

    /**
     * Returns all keyStore Aliases known to the server.
     *
     * @return
     */
    String[] getAllKeyStoreAliases();

    /**
     * Returns the number of distinct keystores known to the server
     *
     * @return
     */
    int getKeyStoreCount();

    /**
     * Assigns the given key to the given alias.
     * <p/>
     * If the given key is of type {@link PrivateKey}, it must be accompanied
     * by a certificate chain certifying the corresponding public key.
     * <p/>
     * If the given alias already exists, the keystore information associated
     * with it is overridden by the given key (and possibly certificate chain).
     *
     * @param keyStoreName The keystore's configuration ID
     * @param alias the alias of the key to store
     * @param key the key to store
     * @param chain the certificate chain to store
     * @throws KeyStoreException
     * @throws CertificateException
     */
    public void setKeyEntryToKeyStore(String keyStoreName, String alias, Key key, Certificate[] chain) throws KeyStoreException, CertificateException;
}
