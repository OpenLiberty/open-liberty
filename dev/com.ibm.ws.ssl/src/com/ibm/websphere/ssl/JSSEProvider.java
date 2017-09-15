/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ssl;

import java.net.URLStreamHandler;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * JSSE provider utility interface.
 * <p>
 * This is the interface for the various JSSEProviders. For v7, mostly IBMJSSE2
 * is used, but for the pluggable client it could be SunJSSE.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public interface JSSEProvider {

    /**
     * Query the package for the HTTPS classes for this provider.
     * 
     * @return String
     */
    String getSSLProtocolPackageHandler();

    /**
     * Query the default protocol value for this provider, ie. SSL, TLS, etc.
     * 
     * @return String
     */
    String getDefaultProtocol();

    /**
     * Query all encryption ciphers for the specified security level supported by
     * this provider.
     * 
     * @param isClient
     * @param securityLevel
     * @return String[]
     */
    String[] getCiphersForSecurityLevel(boolean isClient, String securityLevel);

    /**
     * Access the SSLContext instance that matchs the provided configuration.
     * 
     * @param config
     * @return SSLContext
     * @throws SSLException
     */
    SSLContext getSSLContextInstance(SSLConfig config) throws SSLException;

    /**
     * Access the SSLContext instance that matchs the provided configuration and
     * connection information.
     * 
     * @param connectionInfo
     * @param config
     * @return SSLContext
     * @throws Exception
     */
    SSLContext getSSLContext(Map<String, Object> connectionInfo, SSLConfig config) throws Exception;

    /**
     * Get the URL stream handler for the given configuration.
     * 
     * @param config
     * @return URLStreamHandler
     * @throws Exception
     */
    URLStreamHandler getURLStreamHandler(SSLConfig config) throws Exception;

    /**
     * Get the SSL socket factory that matchs the provided parameters.
     * 
     * @param connectionInfo
     * @param config
     * @return SSLSocketFactory
     * @throws Exception
     */
    SSLSocketFactory getSSLSocketFactory(Map<String, Object> connectionInfo, SSLConfig config) throws Exception;

    /**
     * Get the SSL socket factory that matchs the provided parameters.
     * 
     * @param config
     * @return SSLSocketFactory
     * @throws SSLException
     */
    SSLServerSocketFactory getSSLServerSocketFactory(SSLConfig config) throws SSLException;

    /**
     * Get the trust manager factory for this provider.
     * 
     * @return TrustManagerFactory
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    TrustManagerFactory getTrustManagerFactoryInstance() throws NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * Get the key manager factory for this provider.
     * 
     * @return KeyManagerFactory
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    KeyManagerFactory getKeyManagerFactoryInstance() throws NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * Get a keystore instance for the provided information.
     * 
     * @param keystoretype
     * @param keystoreprovider
     * @return KeyStore
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     */
    KeyStore getKeyStoreInstance(String keystoretype, String keystoreprovider) throws KeyStoreException, NoSuchProviderException;

    /**
     * Get the name of key manager for this provider, ie "SunX509".
     * 
     * @return String
     */
    String getKeyManager();

    /**
     * Get the name of the trust manager for this provider, ie. "SunX509".
     * 
     * @return String
     */
    String getTrustManager();

    /**
     * Get the name of the context provider, ie. "SunJSSE".
     * 
     * @return String
     */
    String getContextProvider();

    /**
     * Get the name of the keystore provider, ie. "SUN".
     * 
     * @return String
     */
    String getKeyStoreProvider();

    /**
     * Get the package and class name of the socket factory for this provider.
     * 
     * @return String
     */
    String getSocketFactory();

    /**
     * Set the default SSL factory for the server.
     * 
     * @return void
     * @throws SSLException
     * @throws Exception
     */
    void setServerDefaultSSLContext(SSLConfig sslConfig) throws SSLException, Exception;

}
