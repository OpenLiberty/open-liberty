/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo;

import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Interface for the mongo feature to access SSL functions
 * <p>
 * This is implemented by an autofeature which depends on the ssl feature.
 */
public interface MongoSslHelper {

    /**
     * Get an ssl socket factory.
     *
     * @param sslConfig - the service obtained from the ssl configuration reference or null if one is
     *            not defined. Where null is passed the properties are obtained from the system default ssl
     *            configuration.
     * @param connectionInfo - the target machine host/port
     * @return an SSL socket factory
     * @throws Exception
     */
    public SSLSocketFactory getSSLSocketFactory(Object sslConfig, final Map<String, Object> connectionInfo) throws Exception;

    /**
     * Get the properties for the provided ssl configuration
     *
     * @param sslConfig
     * @param connectionInfo
     * @param changeListener change listener to register for changes to the returned ssl properties, may be null
     * @return
     * @throws Exception
     */
    public Properties getSSLProperties(Object sslConfig, Map<String, Object> connectionInfo, MongoChangeListener changeListener) throws Exception;

    /**
     * Create the connectionInfo information using the com.ibm.websphere.ssl constants
     * (which is why this is in this helper class).
     *
     * @param hostname the first (or only) hostname on the mongo element
     * @param port the first (or only) port on the mongo element
     * @return a map of connection information
     */
    public Map<String, Object> getConnectionInfo(String hostname, String port);

    /**
     * Get the subject String from the provided X509 certificate.
     *
     * @param sslProps - the SSL properties from getSSLProperties
     * @return String - the subject from the certificate in RFC2253 format
     */
    public String getClientKeyCertSubject(AtomicServiceReference<Object> keyStoreServiceRef, Properties sslProps) throws KeyStoreException, CertificateException;

    /**
     * Remove a change listener which was added previously with {@link #getSSLProperties(Object sslConfig, Map<String, Object> connectionInfo, MongoChangeListener changeListener)}
     * <p>
     * Has no effect if called with a change listener which has not been previously added.
     */
    public void removeChangeListener(MongoChangeListener listener);
}
