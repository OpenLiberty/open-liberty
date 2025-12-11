/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ssl.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.runtime.util.StreamHandlerUtils;
import com.ibm.ws.ssl.JSSEProviderFactory;
import com.ibm.ws.ssl.LibertySSLContext;
import com.ibm.ws.ssl.LibertySSLContextSpi;
import com.ibm.ws.ssl.LibertySSLSocketFactoryWrapper;
import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.ws.ssl.config.SSLConfigManager;
import com.ibm.ws.ssl.config.ThreadManager;
import com.ibm.ws.ssl.config.WSKeyStore;
import com.ibm.ws.ssl.core.TraceNLSHelper;
import com.ibm.ws.ssl.core.WSPKCSInKeyStoreList;
import com.ibm.ws.ssl.core.WSX509KeyManager;
import com.ibm.ws.ssl.core.WSX509TrustManager;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Generic abstract class for a JSSE provider. Subclasses add the provider
 * specific information and methods (Sun vs IBM, etc).
 * <p>
 * This is the abstract class which most providers implement.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public abstract class AbstractJSSEProvider implements JSSEProvider {
    private static final TraceComponent tc = Tr.register(AbstractJSSEProvider.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static final WSPKCSInKeyStoreList pkcsStoreList = new WSPKCSInKeyStoreList();
    private static final Map<SSLConfig, SSLContext> sslContextCacheJAVAX = new HashMap<SSLConfig, SSLContext>();
    private static final String PKGNAME_DELIMITER = "|";

    private static boolean handlersInitialized = false;
    private static Object _lockObj = new Object();

    private String keyManager = null;
    private String trustManager = null;
    private String contextProvider = null;
    private String keyStoreProvider = null;
    private String socketFactory = null;
    private final String protocolPackageHandler = null;
    private String defaultProtocol = null;

    /**
     * Constructor.
     */
    public AbstractJSSEProvider() {
    }

    protected void initialize(String keyMgr, String trustMgr, String cxtProvider, String keyProvider, String factory, String packageHandler, String protocolType) {
        this.keyManager = keyMgr;
        this.trustManager = trustMgr;
        this.contextProvider = cxtProvider;
        this.keyStoreProvider = keyProvider;
        this.socketFactory = factory;
        this.defaultProtocol = protocolType;
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize ", keyMgr, trustMgr, cxtProvider, keyProvider);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            String javaSecurityFile = AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return Security.getProperty("java.security.policy");
                }
            });

            Tr.debug(tc, "java security policy file: " + javaSecurityFile);
            Provider[] provider_list = Security.getProviders();
            for (int i = 0; i < provider_list.length; i++) {
                Tr.debug(tc, "Provider[" + i + "]: " + provider_list[i].getName() + ", info: " + provider_list[i].getInfo());
            }
        }

        if (CryptoUtils.isFips140_2Enabled() || CryptoUtils.isFips140_3Enabled()) {
            try {
                com.ibm.ws.ssl.JSSEProviderFactory.initializeFips();
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught initializing FIPS.", new Object[] { e });
            }
        }

        if (!handlersInitialized && System.getProperty("os.name").equalsIgnoreCase("z/OS")
            && JavaInfo.majorVersion() < 11)
            addHandlers();
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "handlers already initialized ");
        }
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getSSLProtocolPackageHandler()
     */
    @Override
    public String getSSLProtocolPackageHandler() {
        return this.protocolPackageHandler;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getDefaultProtocol()
     */
    @Override
    public String getDefaultProtocol() {
        return this.defaultProtocol;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getKeyManager()
     */
    @Override
    public String getKeyManager() {
        return this.keyManager;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getTrustManager()
     */
    @Override
    public String getTrustManager() {
        return this.trustManager;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getContextProvider()
     */
    @Override
    public String getContextProvider() {
        return this.contextProvider;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getKeyStoreProvider()
     */
    @Override
    public String getKeyStoreProvider() {
        return this.keyStoreProvider;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getSocketFactory()
     */
    @Override
    public String getSocketFactory() {
        return this.socketFactory;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getCiphersForSecurityLevel(boolean,
     * java.lang.String)
     */
    @Override
    public String[] getCiphersForSecurityLevel(boolean isClient, String securityLevel) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getCiphersForSecurityLevel: ", new Object[] { Boolean.valueOf(isClient), securityLevel });

        String[] supportedCiphers = null;

        if (isClient) {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            supportedCiphers = factory.getSupportedCipherSuites();
        } else {
            SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            supportedCiphers = factory.getSupportedCipherSuites();
        }

        return Constants.adjustSupportedCiphersToSecurityLevel(supportedCiphers, securityLevel);
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getSSLContext(java.util.Map,
     * com.ibm.ws.ssl.config.SSLConfig)
     */
    @Override
    public SSLContext getSSLContext(Map<String, Object> connectionInfo, SSLConfig sslConfig) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLContext", new Object[] { connectionInfo });

        // first try to get the SSLContext from the cache.
        SSLContext sslContext = sslContextCacheJAVAX.get(sslConfig);

        setOutboundConnectionInfoInternal(connectionInfo);

        if (sslContext != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getSSLContext -> (from cache)");
            return sslContext;
        }

        String direction = Constants.DIRECTION_OUTBOUND;
        if (connectionInfo != null) {
            direction = (String) connectionInfo.get(Constants.CONNECTION_INFO_DIRECTION);
        }

        // Create the SSL context needed by the JSSE.
        sslContext = getSSLContextInstance(sslConfig);

        List<KeyManager> keyMgrs = new ArrayList<KeyManager>();
        List<TrustManager> trustMgrs = new ArrayList<TrustManager>();

        // get Key and trust managers
        getWSKeyManager(keyMgrs, connectionInfo, sslConfig);
        getWSTrustmanager(trustMgrs, connectionInfo, sslConfig);

        if (!keyMgrs.isEmpty() && !trustMgrs.isEmpty()) {
            KeyManager[] keyManagers = keyMgrs.toArray(new KeyManager[keyMgrs.size()]);
            TrustManager[] trustManagers = trustMgrs.toArray(new TrustManager[trustMgrs.size()]);
            // use default SecureRandom
            sslContext.init(keyManagers, trustManagers, null);
        } else if (keyMgrs.isEmpty() && (direction != null && direction.equals(Constants.DIRECTION_INBOUND))) {
            String message = TraceNLSHelper.getInstance().getString("ssl.config.error.CWPKI0835E",
                                                                    "An SSL/TLS configuration cannot be created for inbound connection due to no key manager being created.");
            throw new SSLException(message);
        } else if (keyMgrs.isEmpty() && !trustMgrs.isEmpty()) {
            TrustManager[] trustManagers = trustMgrs.toArray(new TrustManager[trustMgrs.size()]);
            // use default SecureRandom
            sslContext.init(null, trustManagers, null);
        } else {
            String message = TraceNLSHelper.getInstance().getString("ssl.config.error.CWPKI0836E",
                                                                    "An SSL/TLS configuration cannot created due to no key and trust managers being created.");
            throw new SSLException(message);
        }

        // this may need to be made configurable at some point.
        if (sslContextCacheJAVAX.size() > 100) {
            // instead of clearing the entry cache, grab a few to delete
            Iterator<SSLConfig> keys = sslContextCacheJAVAX.keySet().iterator();
            SSLConfig[] victims = new SSLConfig[] { keys.next(), keys.next(), keys.next(), keys.next(), keys.next() };
            // delete the victim entries after using the iterator (not while)
            for (SSLConfig victim : victims) {
                sslContextCacheJAVAX.remove(victim);
            }
        }

        // wrap the SSLContext with LibertySSLContext to bind our SSL config on the resulting socket
        // alias will be null if it hasn't been configured
        LibertySSLContextSpi libertySSLContextSpi = new LibertySSLContextSpi(sslContext, sslConfig.getProperty(Constants.SSLPROP_ALIAS));
        LibertySSLContext libertySSLContext = new LibertySSLContext(libertySSLContextSpi, sslContext.getProvider(), sslContext.getProtocol());

        sslContextCacheJAVAX.put(sslConfig, libertySSLContext);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SSLContext cache size: " + sslContextCacheJAVAX.size());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getSSLContext -> (new)");

        return libertySSLContext;
    }

    /**
     * @param connectionInfo
     * @param sslConfig
     * @return
     * @throws Exception
     */
    private void getWSTrustmanager(List<TrustManager> tmHolder, Map<String, Object> connectionInfo, SSLConfig sslConfig) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getWSTrustmanager", new Object[] { tmHolder, connectionInfo, sslConfig });

        String direction = Constants.DIRECTION_UNKNOWN;
        String ctxtProvider = getSSLContextProperty(Constants.SSLPROP_CONTEXT_PROVIDER, sslConfig);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ctxtProvider: " + ctxtProvider);
        }
        String clientAuthentication = getSSLContextProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION, sslConfig);
        String trustStoreName = getSSLContextProperty(Constants.SSLPROP_TRUST_STORE_NAME, sslConfig);
        String trustStoreLocation = getSSLContextProperty(Constants.SSLPROP_TRUST_STORE, sslConfig);
        String trustMgr = getSSLContextProperty(Constants.SSLPROP_TRUST_MANAGER, sslConfig);

        if (connectionInfo != null) {
            direction = (String) connectionInfo.get(Constants.CONNECTION_INFO_DIRECTION);
        }

        /*
         * Try to first get truststore from KeyStoreManager.
         */
        KeyStore trustStore = getKeyStoreForManager(sslConfig, Constants.SSLPROP_TRUST_STORE_NAME);

        /*
         * Try and load from the trust store location.
         */
        if (trustStore == null && trustStoreLocation != null) {
            String type = getSSLContextProperty(Constants.SSLPROP_TRUST_STORE_TYPE, sslConfig);
            String password = WSKeyStore.decodePassword(getSSLContextProperty(Constants.SSLPROP_TRUST_STORE_PASSWORD, sslConfig));
            String provider = getSSLContextProperty(Constants.SSLPROP_TRUST_STORE_PROVIDER, sslConfig);

            if (provider != null) {
                trustStore = KeyStore.getInstance(type, provider);
            } else {
                trustStore = KeyStore.getInstance(type);
            }
            trustStore.load(new FileInputStream(new File(trustStoreLocation)), password.toCharArray());
        }

        // ---------------------
        // Handle Trust Store
        // ---------------------
        if (trustStore != null) {
            // Trust store specified.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Using trust store: " + trustStoreLocation);
            }
        } else {
            if (direction.equals(Constants.DIRECTION_INBOUND) && (clientAuthentication.equals(Constants.FALSE))) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "trust store permitted to be null since this is inbound and client auth is false");
                }
            } else {
                throw new IllegalArgumentException("Invalid trust file name of null");
            }
        }

        try {
            // Get instance of trust manager factory. Use contextProvider if
            // available.
            // Already got trustManagerFactory for crypto
            TrustManagerFactory trustManagerFactory = getTrustManagerFactoryInstance(trustMgr, ctxtProvider);
            trustManagerFactory.init(trustStore);

            // prepare trust manager wrapper.
            TrustManager[] defaultTMArray = trustManagerFactory.getTrustManagers();
            WSX509TrustManager wsTrustManager = new WSX509TrustManager(defaultTMArray, connectionInfo, sslConfig, trustStoreName, trustStoreLocation);
            tmHolder.add(wsTrustManager);

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception caught during trustmanager init, " + e);
            FFDCFilter.processException(e, getClass().getName(), "getWSTrustmanager", this);
            throw e;
        }
    }

    /**
     * @param sslConfig
     * @return
     */
    private KeyStore getKeyStoreForManager(SSLConfig sslConfig, String ksProp) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getKeyStoreForManager", new Object[] { sslConfig, ksProp });

        KeyStore ks = null;

        // Access a potentially set contextProvider.
        String keystoreFileName = getSSLContextProperty(ksProp, sslConfig);
        WSKeyStore wsts = null;
        if (keystoreFileName != null)
            wsts = KeyStoreManager.getInstance().getKeyStore(keystoreFileName);

        if (wsts != null) {
            try {
                ks = wsts.getKeyStore(false, false);
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught getting keystore, " + e);
                return null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getKeyStoreForManager", new Object[] { ks });
        return ks;
    }

    /**
     * Initializes the trustmanager factory with a null to get the JDK's default trustmanageer
     *
     */
    public static TrustManager[] getDefaultTrustManager() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getDefaultTrustManager");

        // If the javax.net.ssl.truststore system property is set the JDK uses it as the
        // default truststore.
        String tsFileName = System.getProperty("javax.net.ssl.trustStore");
        if (tsFileName != null && !tsFileName.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "The javax.net.ssl.trustStore property is set to " + tsFileName + " it will used as the JDK default truststore");
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "The default truststore will likely be the jssecacerts or cacerts file.");
        }

        try {
            // Get instance of trust manager factory. Use contextProvider if available.
            // Already got trustManagerFactory for crypto
            String trustMgr = JSSEProviderFactory.getTrustManagerFactoryAlgorithm();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustMgr);

            if (null != trustManagerFactory) {
                // A null truststore will give a trustmanager with the default trust,  typically
                // cacerts file.
                trustManagerFactory.init((KeyStore) null);
            }

            // prepare trust manager
            TrustManager[] cacertTMArray = trustManagerFactory.getTrustManagers();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getDefaultTrustManager");
            return cacertTMArray;

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception caught while trying to initialize default trustmanager: " + e);
            throw e;
        }
    }

    /**
     * @param connectionInfo
     * @param sslConfig
     * @return
     * @throws Exception
     */
    private void getWSKeyManager(List<KeyManager> kmHolder, Map<String, Object> connectionInfo, SSLConfig sslConfig) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getWSKeyManager", new Object[] { kmHolder, connectionInfo, sslConfig });

        KeyManagerFactory keyManagerFactory = null;
        String ctxtProvider = getSSLContextProperty(Constants.SSLPROP_CONTEXT_PROVIDER, sslConfig);
        String keyStoreName = getSSLContextProperty(Constants.SSLPROP_KEY_STORE_NAME, sslConfig);
        String keyStoreLocation = getSSLContextProperty(Constants.SSLPROP_KEY_STORE, sslConfig);
        String clientAliasName = getSSLContextProperty(Constants.SSLPROP_KEY_STORE_CLIENT_ALIAS, sslConfig);
        String serverAliasName = getSSLContextProperty(Constants.SSLPROP_KEY_STORE_SERVER_ALIAS, sslConfig);
        String keyMgr = getSSLContextProperty(Constants.SSLPROP_KEY_MANAGER, sslConfig);

        KeyStore keyStore = getKeyStoreForManager(sslConfig, Constants.SSLPROP_KEY_STORE_NAME);

        if (keyStore != null) {
            WSKeyStore wsks = KeyStoreManager.getInstance().getWSKeyStore(keyStoreName);

            // Key store specified.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Using software keystore: " + keyStoreLocation);
            }

            keyManagerFactory = getKeyManagerFactoryInstance(keyMgr, ctxtProvider);
            SerializableProtectedString keypass = wsks.getKeyPassword();
            if (keypass != null && !keypass.isEmpty()) {
                try {
                    String decodedPass = WSKeyStore.decodePassword(new String(keypass.getChars()));
                    synchronized (_lockObj) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Entering synchronized block around key manager factory init.");
                        }
                        keyManagerFactory.init(keyStore, decodedPass.toCharArray());
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exiting synchronized block around key manager factory init.");
                        }
                    }
                } catch (UnrecoverableKeyException exc) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error initializing key manager, the password can not be used to recover all keys");
                    }
                    Tr.error(tc, "ssl.unrecoverablekey.error.CWPKI0813E", new Object[] { keyStoreLocation, exc.getMessage() });
                    throw new UnrecoverableKeyException(exc.getMessage() + ": invalid password for key in file '" + keyStoreLocation + "'");
                }

                // Initialize the SSL context with the key and trust manager factories.
                WSX509KeyManager wsKeyManager = new WSX509KeyManager(keyStore, null, keyManagerFactory, sslConfig, null);

                if (serverAliasName != null && serverAliasName.length() > 0)
                    wsKeyManager.setServerAlias(serverAliasName);
                if (clientAliasName != null && clientAliasName.length() > 0)
                    wsKeyManager.setClientAlias(clientAliasName);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Initializing WSX509KeyManager.", new Object[] { serverAliasName, clientAliasName });
                kmHolder.add(wsKeyManager);

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No password provide so do not create a keymanager");
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No key store specified and no hardware crypto defined");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getWSKeyManager");
    }

    // returns the property based on system prop, global prop, then properties
    // object prop
    private String getSSLContextProperty(String propertyName, Properties prop) {
        String value = null;

        if (prop != null) {
            value = prop.getProperty(propertyName);
        } else {
            value = System.getProperty(propertyName);

            if (value == null) {
                value = SSLConfigManager.getInstance().getGlobalProperty(propertyName);
            }
        }

        return value;
    }

    /*
     * @see
     * com.ibm.wesphere.ssl.JSSEProvider#getURLStreamHandler(com.ibm.ws.ssl.config
     * .SSLConfig)
     */
    @Override
    public URLStreamHandler getURLStreamHandler(SSLConfig config) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getURLStreamHandler");
        URLStreamHandler urlStreamHandler = null;
        Properties existingProps = null;

        try {
            // pop/push old/new properties on the thread
            existingProps = ThreadManager.getInstance().getPropertiesOnThread();
            ThreadManager.getInstance().setPropertiesOnThread(config);

            // try getting the stream handler from factory for "https"
            urlStreamHandler = getHandler();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getURLStreamHandler");
            return urlStreamHandler;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getURLStreamHandler", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getURLStreamHandler().", new Object[] { e });
            if (e instanceof SSLException)
                throw (SSLException) e;
            throw new SSLException(e);
        } finally {
            // push old back on the thread.
            ThreadManager.getInstance().setPropertiesOnThread(existingProps);
        }
    }

    /*
     * @see
     * com.ibm.websphere.ssl.JSSEProvider#getSSLServerSocketFactory(com.ibm.ws
     * .ssl.config.SSLConfig)
     */
    @Override
    public SSLServerSocketFactory getSSLServerSocketFactory(SSLConfig config) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLServerSocketFactory");

        try {
            SSLContext context = getSSLContext(null, config);
            if (context != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getSSLServerSocketFactory");
                return context.getServerSocketFactory();
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getSSLServerSocketFactory", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getSSLServerSocketFactory().", new Object[] { e });
            if (e instanceof SSLException)
                throw (SSLException) e;
            throw new SSLException(e);
        }

        throw new SSLException("SSLContext could not be created to return an SSLServerSocketFactory.");
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getSSLSocketFactory(java.util.Map,
     * com.ibm.ws.ssl.config.SSLConfig)
     */
    @Override
    public SSLSocketFactory getSSLSocketFactory(Map<String, Object> connectionInfo, SSLConfig config) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLSocketFactory", new Object[] { connectionInfo });

        SSLContext context = getSSLContext(connectionInfo, config);
        if (context != null) {
            SSLSocketFactory factory = context.getSocketFactory();

            // wrap the SSLSocketFactory to apply Liberty's config to its sockets
            // alias will be null if it hasn't been configured
            LibertySSLSocketFactoryWrapper wrapper = new LibertySSLSocketFactoryWrapper(factory, config.getProperty(Constants.SSLPROP_ALIAS));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getSSLSocketFactory -> " + factory.getClass().getName());

            return wrapper;
        }

        throw new SSLException("SSLContext could not be created to return an SSLSocketFactory.");
    }

    /*
     * @see
     * com.ibm.websphere.ssl.JSSEProvider#getSSLContextInstance(com.ibm.ws.ssl
     * .config.SSLConfig)
     */
    @Override
    public SSLContext getSSLContextInstance(final SSLConfig config) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLContextInstance");

        if (config == null) {
            throw new IllegalArgumentException("SSL configuration is not specified.");
        }

        // now generate a new SSLContext
        //final String ctxtProvider = Constants.IBMJCEPlusFIPS_NAME;
        final String ctxtProvider = config.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
        final String alias = config.getProperty(Constants.SSLPROP_ALIAS);
        final String configURL = config.getProperty(Constants.SSLPROP_CONFIGURL_LOADED_FROM);
        String protocolVal = config.getProperty(Constants.SSLPROP_PROTOCOL);

        SSLContext sslContext = null;

        if (protocolVal == null) {
            throw new IllegalArgumentException("Protocol is not specified.");
        } else {
            String[] protocols = protocolVal.split(",");
            if (protocols.length > 1)
                protocolVal = defaultProtocol;
        }

        final String protocol = protocolVal;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "protocol:  " + protocolVal);
            Tr.debug(tc, "ctxtProvider:  " + ctxtProvider);
        }

        try {
            sslContext = AccessController.doPrivileged(new PrivilegedExceptionAction<SSLContext>() {
                @Override
                public SSLContext run() throws NoSuchAlgorithmException, NoSuchProviderException {
                    if (ctxtProvider != null)
                        return SSLContext.getInstance(protocol, ctxtProvider);
                    return SSLContext.getInstance(protocol);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception ex = e.getException();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception occurred getting SSL context.", new Object[] { ex });

            if (ex instanceof NoSuchAlgorithmException) {
                String message = TraceNLSHelper.getInstance().getFormattedMessage("ssl.no.such.algorithm.CWPKI0028E",
                                                                                  new Object[] { protocol, alias, configURL, ex.getMessage() },
                                                                                  "CWPKI0028E: SSL handshake protocol " + protocol
                                                                                                                                                + " is not valid.  This protocol is specified in the SSL configuration alias "
                                                                                                                                                + alias
                                                                                                                                                + " loaded from SSL configuration file "
                                                                                                                                                + configURL
                                                                                                                                                + ".  The extended error message is: "
                                                                                                                                                + ex.getMessage() + ".");
                Tr.error(tc, "ssl.no.such.algorithm.CWPKI0028E",
                         new Object[] { protocol, alias, configURL, ex.getMessage() });
                throw new SSLException(message, ex);
            } else if (ex instanceof NoSuchProviderException) {
                String message = TraceNLSHelper.getInstance().getFormattedMessage("ssl.invalid.context.provider.CWPKI0029E",
                                                                                  new Object[] { Constants.IBMJSSE2_NAME, alias, configURL, ex.getMessage() },
                                                                                  "CWPKI0029E: SSL context provider " + Constants.IBMJSSE2_NAME
                                                                                                                                                               + " is not valid.  This provider is specified in the SSL configuration alias "
                                                                                                                                                               + alias
                                                                                                                                                               + " loaded from SSL configuration file "
                                                                                                                                                               + configURL
                                                                                                                                                               + ".  The extended error message is: "
                                                                                                                                                               + ex.getMessage()
                                                                                                                                                               + ".");
                Tr.error(tc, "ssl.invalid.context.provider.CWPKI0029E",
                         new Object[] { Constants.IBMJSSE2_NAME, alias, configURL, ex.getMessage() });
                throw new SSLException(message, ex);
            } else {
                throw new SSLException(ex);
            }
        } catch (Throwable t) {
            Throwable cause = t.getCause();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Throwable occurred getting SSL context.", new Object[] { cause });
            throw new SSLException(cause.getMessage());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getSSLContextInstance");
        return sslContext;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getTrustManagerFactoryInstance()
     */
    @Override
    public TrustManagerFactory getTrustManagerFactoryInstance() throws NoSuchAlgorithmException, NoSuchProviderException {
        return getTrustManagerFactoryInstance(getTrustManager(), getContextProvider());
    }

    /**
     * Get the trust manager factory instance using the provided information.
     *
     * @see com.ibm.websphere.ssl.JSSEProvider#getTrustManagerFactoryInstance()
     * @param trustMgr
     * @param ctxtProvider
     * @return TrustManagerFactory
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public TrustManagerFactory getTrustManagerFactoryInstance(String trustMgr, String ctxtProvider) throws NoSuchAlgorithmException, NoSuchProviderException {
        String mgr = trustMgr;
        String provider = ctxtProvider;
        if (mgr.indexOf('|') != -1) {
            String[] trustManagerArray = mgr.split("\\|");
            if (trustManagerArray != null && trustManagerArray.length == 2) {
                mgr = trustManagerArray[0];
                provider = trustManagerArray[1];
            }
        }

        TrustManagerFactory rc = TrustManagerFactory.getInstance(mgr, provider);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getTrustManagerFactory.getInstance(" + mgr + ", " + provider + ")" + rc);
        return rc;
    }

    /*
     * @see com.ibm.websphere.ssl.JSSEProvider#getKeyManagerFactoryInstance()
     */
    @Override
    public KeyManagerFactory getKeyManagerFactoryInstance() throws NoSuchAlgorithmException, NoSuchProviderException {
        return getKeyManagerFactoryInstance(getKeyManager(), getContextProvider());
    }

    /**
     * Get the key manager factory instance using the provided information.
     *
     * @see com.ibm.websphere.ssl.JSSEProvider#getKeyManagerFactoryInstance()
     * @param keyMgr
     * @param ctxtProvider
     * @return KeyManagerFactory
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public KeyManagerFactory getKeyManagerFactoryInstance(String keyMgr, String ctxtProvider) throws NoSuchAlgorithmException, NoSuchProviderException {
        String mgr = keyMgr;
        String provider = ctxtProvider;
        if (mgr.indexOf('|') != -1) {
            String[] keyManagerArray = mgr.split("\\|");
            if (keyManagerArray != null && keyManagerArray.length == 2) {
                mgr = keyManagerArray[0];
                provider = keyManagerArray[1];
            }
        }

        KeyManagerFactory rc = KeyManagerFactory.getInstance(mgr, provider);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getKeyManagerFactory.getInstance(" + mgr + ", " + provider + ") " + rc);
        return rc;
    }

    /*
     * @see
     * com.ibm.websphere.ssl.JSSEProvider#getKeyStoreInstance(java.lang.String,
     * java.lang.String)
     */
    @Override
    public KeyStore getKeyStoreInstance(String type, String ksProvider) throws KeyStoreException, NoSuchProviderException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "KeyStore.getInstance(" + type + ", " + ksProvider + ")");

        String provider = ksProvider;
        if (null == provider) {
            provider = getKeyStoreProvider();
        }
        if (null == provider) {
            return KeyStore.getInstance(type);
        }
        return KeyStore.getInstance(type, provider);
    }

    /*
     * setServerDefaultSSLContext method will set the the process's default SSLContect to one
     * created with the server's default SSL configuration. Once this is set the javax.net.ssl.*
     * properties to set the process's default SSLContext will not take effect or will override
     * the properties if they are set when starting the process.
     */
    @Override
    public void setServerDefaultSSLContext(SSLConfig defaultSSLConfig) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setServerDefaultSSLContext");

        SSLContext context = getSSLContext(null, defaultSSLConfig);

        if (context != null) {
            SSLContext.setDefault(context);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Default SSLContext set to " + defaultSSLConfig.getProperty(Constants.SSLPROP_ALIAS));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setServerDefaultSSLContext");
    }

    /**
     * Query the default handler for this provider using HTTPS.
     *
     * @return URLStreamHandler
     * @throws Exception
     */
    public URLStreamHandler getHandler() throws Exception {
        String handlerString = getSSLProtocolPackageHandler() + ".https.Handler";
        URLStreamHandler streamHandler = null;

        try {
            ClassLoader cl = AccessController.doPrivileged(getCtxClassLoader);
            if (cl != null) {
                streamHandler = (URLStreamHandler) cl.loadClass(handlerString).newInstance();
            } else {
                streamHandler = (URLStreamHandler) Class.forName(handlerString).newInstance();
            }
            return streamHandler;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getHandler", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception loading https stream handler.", new Object[] { e });
            Tr.error(tc, "ssl.load.https.stream.handler.CWPKI0025E", new Object[] { handlerString, e.getMessage() });
            throw e;
        }
    }

    protected static void addHandlers() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "addHandlers");

        if (!handlersInitialized) {
            try {
                StreamHandlerUtils.create();
                if (!queryProvider("safkeyring")) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "Adding handler:  com.ibm.crypto.provider.safkeyring.Handler");
                    addProvider("safkeyring",
                                "com.ibm.crypto.provider.safkeyring.Handler");
                }
                if (!queryProvider("safkeyringhw")) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "Adding handler: com.ibm.crypto.hdwrCCA.provider.safkeyring.Handler");
                    addProvider("safkeyringhw",
                                "com.ibm.crypto.hdwrCCA.provider.safkeyring.Handler");
                }
                if (!queryProvider("safkeyringhybrid")) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "Adding handler: com.ibm.crypto.ibmjcehybrid.provider.safkeyring.Handler");
                    addProvider("safkeyringhybrid",
                                "com.ibm.crypto.ibmjcehybrid.provider.safkeyring.Handler");
                }
                handlersInitialized = true;
            } catch (Throwable t) {
                FFDCFilter.processException(t, AbstractJSSEProvider.class.getName(), "addHandlers");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Unable to set safkeyring stream handler", t);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "addHandlers");
    }

    // this loads the KeyManager class, if present
    private X509KeyManager loadCustomKeyManager(String kmClass) throws Exception {
        X509KeyManager km = null;

        try {
            ClassLoader cl = AccessController.doPrivileged(getCtxClassLoader);

            if (cl != null) {
                try {
                    km = (X509KeyManager) cl.loadClass(kmClass).newInstance();
                } catch (Exception e) {
                    // no ffdc needed
                }
            }

            if (km == null) {
                km = (X509KeyManager) Class.forName(kmClass).newInstance();
            }

            return km;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "loadCustomKeyManager", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception loading custom KeyManager.", new Object[] { e });
            Tr.error(tc, "ssl.load.keymanager.error.CWPKI0021E", new Object[] { kmClass, e.getMessage() });
            throw e;
        }
    }

    // this loads the custom TrustManager class(es), if present
    private X509TrustManager loadCustomTrustManager(String tmClass) throws Exception {
        X509TrustManager tm = null;

        try {
            ClassLoader cl = AccessController.doPrivileged(getCtxClassLoader);

            if (cl != null) {
                try {
                    tm = (X509TrustManager) cl.loadClass(tmClass).newInstance();
                } catch (Exception e) {
                    // no ffdc needed
                }
            }

            if (tm == null) {
                tm = (X509TrustManager) Class.forName(tmClass).newInstance();
            }

            return tm;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "loadCustomTrustManager", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception loading custom TrustManager.", new Object[] { e });
            Tr.error(tc, "ssl.load.trustmanager.error.CWPKI0020E", new Object[] { tmClass, e.getMessage() });
            throw e;
        }
    }

    private final static PrivilegedAction<ClassLoader> getCtxClassLoader = new PrivilegedAction<ClassLoader>() {
        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    };

    private static boolean queryProvider(String provider) {
        return StreamHandlerUtils.queryProvider(provider);
    }

    private static void addProvider(String provider, String handler) {
        try {
            StreamHandlerUtils.addProvider(provider, handler);
        } catch (Exception e) {
            FFDCFilter.processException(e, AbstractJSSEProvider.class.getName(), "addProvider");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception: " + e);
        }
    }

    /**
     * Clear the contents of the SSLContext cache.
     */
    public static void clearSSLContextCache() {
        if (sslContextCacheJAVAX != null && sslContextCacheJAVAX.size() > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Clearing standard javax.net.ssl.SSLContext cache.");
            sslContextCacheJAVAX.clear();
        }
    }

    /**
     * Clear the contents of the SSLContext cache if it uses the files passed in
     */
    public static void clearSSLContextCache(Collection<File> modifiedFiles) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Clearing standard javax.net.ssl.SSLContext cached object containing keystores: " + new Object[] { modifiedFiles });
        for (File modifiedKeystoreFile : modifiedFiles) {
            String filePath = null;
            try {
                filePath = modifiedKeystoreFile.getCanonicalPath();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception comparing file path.");
                continue;
            }
            removeEntryFromSSLContextMap(filePath);
        }
    }

    /**
     * Give a keyStoreFile location look for the SSLContexts that reference it as either a keystore or
     * truststore then remove those SSLContexts from the cache.
     */
    public static void removeEntryFromSSLContextMap(String keyStorePath) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removeEntryFromSSLContextMap: " + new Object[] { keyStorePath });

        List<SSLConfig> removeList = new ArrayList<SSLConfig>();

        for (Entry<SSLConfig, SSLContext> entry : sslContextCacheJAVAX.entrySet()) {
            SSLConfig cachedConfig = entry.getKey();

            if (cachedConfig != null) {
                String ksPropValue = cachedConfig.getProperty(Constants.SSLPROP_KEY_STORE, null);
                boolean ksFileBased = Boolean.parseBoolean(cachedConfig.getProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED));
                String tsPropValue = cachedConfig.getProperty(Constants.SSLPROP_TRUST_STORE, null);
                boolean tsFileBased = Boolean.parseBoolean(cachedConfig.getProperty(Constants.SSLPROP_TRUST_STORE_FILE_BASED));

                if ((ksPropValue != null && keyStorePath.equals(WSKeyStore.getCannonicalPath(ksPropValue, ksFileBased))) ||
                    (tsPropValue != null && keyStorePath.equals(WSKeyStore.getCannonicalPath(tsPropValue, tsFileBased)))) {
                    removeList.add(cachedConfig);
                }
            }
        }
        if (!removeList.isEmpty()) {
            for (SSLConfig removeEntry : removeList) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "removing from sslContext cache: " + removeEntry.toString());

                sslContextCacheJAVAX.remove(removeEntry);
            }
        }
    }

    private void setOutboundConnectionInfoInternal(Map<String, Object> connectionInfo) {
        Map<String, Object> outbound = null;
        if (connectionInfo != null) {
            String direction = (String) connectionInfo.get(Constants.CONNECTION_INFO_DIRECTION);
            if ((direction != null) && (direction.length() > 0) && (direction.equalsIgnoreCase(Constants.DIRECTION_OUTBOUND))) {
                outbound = connectionInfo;
            }
        }

        ThreadManager.getInstance().setOutboundConnectionInfoInternal(outbound);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "outboundConnectionInfo: " + outbound);
    }
}
