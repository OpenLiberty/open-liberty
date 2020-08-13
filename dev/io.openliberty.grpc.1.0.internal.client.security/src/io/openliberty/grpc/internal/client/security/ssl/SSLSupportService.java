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
package io.openliberty.grpc.internal.client.security.ssl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;

public class SSLSupportService {

    private static final TraceComponent tc = Tr.register(SSLSupportService.class);

    /**
     * Get the SSL properties for a given host:port and server.xml SSL reference
     * TODO: caching
     * 
     * @param String server.xml SSL reference ID
     * @param String target host
     * @param String target port
     * @return
     */
    protected static Properties getSSLProps(String sslRef, String target, String port) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "getSSLProps for ssl reference ID: {0} host: {1} port: {2}", sslRef, target, port);
		}
        JSSEHelper helper = com.ibm.websphere.ssl.JSSEHelper.getInstance();
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, "outbound");
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, target);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port);
        
        Properties sslProps = null;
        
        try {
        	sslProps = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
			    @Override
			    public Properties run() throws Exception {
			        return helper.getProperties(sslRef, connectionInfo, null);
			    }
			});
		} catch (PrivilegedActionException e) {
			// FFDC
		}
        return sslProps;
    }
    
    protected static String getSSLProtocol(Properties props) {
    	return props.getProperty(Constants.SSLPROP_PROTOCOL);
    }
    
    protected static List<String> getCiphers(Properties props) {
        String enabledCipherString = props.getProperty(Constants.SSLPROP_ENABLED_CIPHERS);
        if (enabledCipherString != null) {
            String[] requested = enabledCipherString.split("[,\\s]+");
            return Arrays.asList(requested);
        }
        return null;
    }
    
    /**
     * Build a KeyManagerFactory from the given SSL props
     * @param Properties
     * @return
     */
    protected static KeyManagerFactory getKeyManagerFactory(Properties props) {
		String keyStorePath = props.getProperty(Constants.SSLPROP_KEY_STORE);
		char[] keyStorePass = props.getProperty(Constants.SSLPROP_KEY_STORE_PASSWORD).toCharArray();
		String keyStoreType = props.getProperty(Constants.SSLPROP_KEY_STORE_TYPE);
        KeyManagerFactory keyFactory = null;
        try {
        	keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = loadKeyStore(keyStorePath, keyStoreType, keyStorePass);
            keyFactory.init(ks, keyStorePass);		                
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            // FFDC
        }
        return keyFactory;
    }
    
    /**
     * Build a TrustManagerFactory from the given SSL props
     * @param Properties
     * @return
     */
    protected static TrustManagerFactory getTrustManagerFactory(Properties props) {
		String trustStorePath = props.getProperty(Constants.SSLPROP_TRUST_STORE);
		char[] trustStorePass = props.getProperty(Constants.SSLPROP_TRUST_STORE_PASSWORD).toCharArray();
		String trustStoreType = props.getProperty(Constants.SSLPROP_TRUST_STORE_TYPE);
        TrustManagerFactory trustFactory = null;
        try {
        	trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = loadKeyStore(trustStorePath, trustStoreType, trustStorePass);
        	trustFactory.init(ks);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            // FFDC
        }
        return trustFactory;
    }
    
    private static KeyStore loadKeyStore(String path, String type, char[] pass) {
    	KeyStore store = null;
        try {
        	store = KeyStore.getInstance(type);
            InputStream readStream = new FileInputStream(path);
            store.load(readStream, pass);
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
        	// FFDC
        }
        return store;
    }
}
