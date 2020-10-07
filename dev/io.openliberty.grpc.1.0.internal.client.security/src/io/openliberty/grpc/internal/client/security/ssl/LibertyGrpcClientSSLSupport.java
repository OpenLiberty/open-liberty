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

import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import io.grpc.netty.GrpcSslContexts;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.openliberty.grpc.internal.client.GrpcClientMessages;
import io.openliberty.grpc.internal.client.GrpcSSLService;

@Component(service = {GrpcSSLService.class}, property = { "service.vendor=IBM" })
public class LibertyGrpcClientSSLSupport implements GrpcSSLService{

	private static final TraceComponent tc = Tr.register(LibertyGrpcClientSSLSupport.class, GrpcClientMessages.GRPC_TRACE_NAME, GrpcClientMessages.GRPC_CLIENT_SECURITY_BUNDLE);

    static final String KEY_KEYSTORE_SERVICE_REF = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE_REF);

    @Activate
    protected void activate(ComponentContext cc) {
    	keyStoreServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
    	keyStoreServiceRef.deactivate(cc);
    }

    @Reference(name = KEY_KEYSTORE_SERVICE_REF, service = KeyStoreService.class)
    protected void setKeyStoreService(ServiceReference<KeyStoreService> reference) {
        keyStoreServiceRef.setReference(reference);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> reference) {
        keyStoreServiceRef.unsetReference(reference);
    }
	
	/**
	 * Build a io.grpc.netty.shaded.io.netty.handler.ssl.SslContext for the given outbound connection
	 * 
	 * @param String sslRef server.xml SSL reference ID
	 * @param String host
	 * @param String port
	 * @return SslContext
	 */
	public SslContext getOutboundClientSSLContext(String sslRef, String host, String port) {
		
		SslContext context = null;
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "getOutboundClientSSLContext ssl reference ID: {0}", sslRef);
		}
		Properties props = getSSLProps(sslRef, host, port);
		if (props != null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "attempting to build SslContext with props: {0}", props);
			}
			try {
				SslContextBuilder builder = GrpcSslContexts.forClient();
				
				TrustManagerFactory trustFactory = getTrustManagerFactory(props);
				if (trustFactory != null) {
					builder.trustManager(trustFactory);
				}
				
				KeyManagerFactory keyFactory = getKeyManagerFactory(props);
				if (keyFactory != null) {
					builder.keyManager(keyFactory);
				}
				
				String sslProtocol = getSSLProtocol(props);
				if (sslProtocol != null) {
					if (!!!(sslProtocol.equals("TLSv1.2") || sslProtocol.equals("TLSv1.3"))) {
						Tr.warning(tc, "invalid.ssl.protocol", new Object[] { sslProtocol, getSSLAlias(props) } );
					}
					builder.protocols(sslProtocol);
				}
				
				List<String> ciphers = getCiphers(props);
				if (ciphers != null && !ciphers.isEmpty()) {
					builder.ciphers(ciphers);
				}
				
				builder.clientAuth(ClientAuth.OPTIONAL);
				context = builder.build();
			} catch (Exception e) {
				Tr.warning(tc, "client.ssl.failed", new Object[] { getSSLAlias(props), e } );
			}
		}		
		return context;
	}

    /**
     * Get the SSL properties for a given host:port and server.xml SSL reference
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
			        return helper.getProperties(sslRef, connectionInfo, null, false);
			    }
			});
		} catch (PrivilegedActionException e) {
    		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    			Tr.debug(tc, "getSSLProps failed", e);
    		}
		}
        return sslProps;
    }

    protected static String getSSLAlias(Properties props) {
        return props.getProperty(Constants.SSLPROP_ALIAS);
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
    protected KeyManagerFactory getKeyManagerFactory(Properties props) {
    	String keyStoreName = props.getProperty(Constants.SSLPROP_KEY_STORE_NAME);
		char[] keyStorePass = props.getProperty(Constants.SSLPROP_KEY_STORE_PASSWORD).toCharArray();
        KeyManagerFactory keyFactory = null;
        try {
        	keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = loadKeyStore(keyStoreName);
            keyFactory.init(ks, keyStorePass);
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
    		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    			Tr.debug(tc, "getKeyManagerFactory failed to load  factory for {0}", keyStoreName);
    		}
        }
        return keyFactory;
    }
    
    /**
     * Build a TrustManagerFactory from the given SSL props
     * @param Properties
     * @return
     */
    protected TrustManagerFactory getTrustManagerFactory(Properties props) {
    	String trustStoreName = props.getProperty(Constants.SSLPROP_TRUST_STORE_NAME);
        TrustManagerFactory trustFactory = null;
        try {
			trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			KeyStore ks = loadKeyStore(trustStoreName);
			trustFactory.init(ks);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
    		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    			Tr.debug(tc, "getTrustManagerFactory failed to load  factory for {0}", trustStoreName);
    		}
        }
        return trustFactory;
    }

    /**
     * Use the KeyStoreService to load a KeyStore instance matching the given name
     * 
     * @param name
     * @return KeyStore
     * @throws KeyStoreException
     */
    private KeyStore loadKeyStore(String name) throws KeyStoreException{
		KeyStoreService service = keyStoreServiceRef.getServiceWithException();
		KeyStore store = null;
		if (service != null) {
			store = service.getKeyStore(name);
		}
		return store;
    }
}
