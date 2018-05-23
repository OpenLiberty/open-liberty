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
package com.ibm.ws.mongo.ssl.internal;

import java.security.AccessController;
import java.security.KeyStoreException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.x500.X500Principal;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfigChangeEvent;
import com.ibm.websphere.ssl.SSLConfigChangeListener;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.mongo.MongoChangeListener;
import com.ibm.ws.mongo.MongoSslHelper;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component
public class MongoSslHelperImpl implements MongoSslHelper {

    private final Map<MongoChangeListener, SSLConfigChangeListener> changeListeners = Collections.synchronizedMap(new HashMap<MongoChangeListener, SSLConfigChangeListener>());

    @Override
    public SSLSocketFactory getSSLSocketFactory(Object sslConfig, final Map<String, Object> connectionInfo) throws Exception {

        String sslConfigurationAlias = null;
        if (sslConfig != null) {
            sslConfigurationAlias = ((com.ibm.wsspi.ssl.SSLConfiguration) sslConfig).getAlias();
        }

        // get the JSSEHelper...
        final com.ibm.websphere.ssl.JSSEHelper helper = com.ibm.websphere.ssl.JSSEHelper.getInstance();

        // ...and the properties from it.  If the alias passed in is null it will get
        // the properties from the default ssl configuration.
        final Properties sslProps = helper.getProperties(sslConfigurationAlias, connectionInfo, null);

        SSLSocketFactory sslSF = AccessController.doPrivileged(new PrivilegedExceptionAction<SSLSocketFactory>() {
            @Override
            public SSLSocketFactory run() throws Exception {
                return helper.getSSLSocketFactory(connectionInfo, sslProps);
            }
        });
        return sslSF;
    }

    @Override
    public Properties getSSLProperties(Object sslConfig, Map<String, Object> connectionInfo, MongoChangeListener changeListener) throws Exception {
        Properties sslProperties = null;

        // get the ssl configuration alias or null for default ssl alias
        String sslAlias = null;
        if (sslConfig != null) {
            sslAlias = ((com.ibm.wsspi.ssl.SSLConfiguration) sslConfig).getAlias();
        }

        // get the JSSEHelper
        final com.ibm.websphere.ssl.JSSEHelper helper = com.ibm.websphere.ssl.JSSEHelper.getInstance();

        SSLConfigChangeListener configListener = null;
        if (changeListener != null) {
            configListener = new MongoSSLConfigChangeListener(changeListener);
            changeListeners.put(changeListener, configListener);
        }

        // and the properties from it (either defined ones or default ones)
        sslProperties = helper.getProperties(sslAlias, connectionInfo, configListener);
        return sslProperties;
    }

    @Override
    public Map<String, Object> getConnectionInfo(String hostname, String port) {

        // use the com.ibm.websphere.ssl.Contants constants for this
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, "outbound");
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, hostname);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port);

        return connectionInfo;
    }

    @Override
    public String getClientKeyCertSubject(AtomicServiceReference<Object> keyStoreServiceRef, Properties sslProps) throws KeyStoreException, CertificateException {
        KeyStoreService keyStoreService = (KeyStoreService) keyStoreServiceRef.getService();
        X509Certificate cert = keyStoreService.getClientKeyCert(sslProps);

        // Get the subject of the certificate in RFC2253 format
        X500Principal x500 = (cert == null) ? null : cert.getSubjectX500Principal();
        String name = (x500 == null) ? null : x500.getName();
        return name;
    }

    @Override
    public void removeChangeListener(MongoChangeListener listener) {
        synchronized (changeListeners) {
            SSLConfigChangeListener sslConfigListener = changeListeners.get(listener);
            if (sslConfigListener != null) {
                JSSEHelper helper = JSSEHelper.getInstance();
                try {
                    helper.deregisterSSLConfigChangeListener(sslConfigListener);
                } catch (SSLException e) {
                    throw new RuntimeException(e);
                }
                changeListeners.remove(listener);
            }
        }
    }

    /**
     * SSLConfigChangeListener which passes events to a delegate ChangeListener
     */
    private static class MongoSSLConfigChangeListener implements SSLConfigChangeListener {

        private final MongoChangeListener delegate;

        public MongoSSLConfigChangeListener(MongoChangeListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void stateChanged(SSLConfigChangeEvent e) {
            delegate.changeOccurred();
        }

    }

}
