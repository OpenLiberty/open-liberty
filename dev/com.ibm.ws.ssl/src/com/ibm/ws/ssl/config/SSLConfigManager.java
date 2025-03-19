/*******************************************************************************
 * Copyright (c) 2007, 2024 IBM Corporation and others.
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

package com.ibm.ws.ssl.config;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.websphere.ssl.SSLConfigChangeEvent;
import com.ibm.websphere.ssl.SSLConfigChangeListener;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.JSSEProviderFactory;
import com.ibm.ws.ssl.internal.LibertyConstants;
import com.ibm.ws.ssl.provider.AbstractJSSEProvider;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Class that reads and controls access to SSL configuration objects. It
 * provides various utility methods related to configurations.
 * <p>
 * SSL configuration manager is responsible for managing the SSLConfig objects
 * which are primarily used to create SSLContext, SSLSocketFactory,
 * SSLServerSocketFactory, URLStreamHandlers, etc. The JSSEHelper API is the
 * primary way outside of this component to retrieve SSLConfig properties and
 * objects from them.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class SSLConfigManager {
    private static final TraceComponent tc = Tr.register(SSLConfigManager.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static class Singleton {
        static final SSLConfigManager INSTANCE = new SSLConfigManager();
    }

    private static final String SOCKET_FACTORY_PROP = "ssl.SocketFactory.provider";
    public static final String SOCKET_FACTORY_CLASS = "com.ibm.ws.kernel.boot.security.SSLSocketFactoryProxy";

    private boolean isServerProcess = false;
    private boolean transportSecuritySet = false;
    private final Properties globalConfigProperties = new Properties();
    // used to hold all of the SSL configs.
    private final Map<String, SSLConfig> sslConfigMap = new HashMap<String, SSLConfig>();

    // used to hold sslconfig -> listener references
    private final Map<String, List<SSLConfigChangeListener>> sslConfigListenerMap = new HashMap<String, List<SSLConfigChangeListener>>();
    // used to hold listener -> listener event references
    private final Map<SSLConfigChangeListener, SSLConfigChangeEvent> sslConfigListenerEventMap = new HashMap<SSLConfigChangeListener, SSLConfigChangeEvent>();

    //get the outbound selections method
    private final OutboundSSLSelections outboundSSL = new OutboundSSLSelections();

    private Map<String, String> aliasPIDs = null; // map LDAP ssl ref, example com.ibm.ws.ssl.repertoire_102 to LDAPSettings. Issue 876

    //get the protocolHelper
    private final ProtocolHelper protocolHelper = new ProtocolHelper();

    // Unsaved cfgs due to error
    private static final List<String> unSavedCfgs = new ArrayList<>();
    private static boolean messageIssued = false;

    /*
     * The outbound connection from the collective member to the controller in the
     * collective is protected by SSL. This SSL configuration also represents
     * the SSL certificate authentication for the connection.
     */
    private static final String CONTROLLER_SSL_CONFIG = "controllerConnectionConfig";

    /*
     * The outbound connection from the collective controller to a member server is
     * protected by SSL. This SSL configuration also represents the SSL
     * certificate authentication for the connection.
     */
    private static final String MEMBER_SSL_CONFIG = "memberConnectionConfig";

    // Collective controller and member serverIdentity
    private static final String SERVER_IDENTITY = "serverIdentity";

    /**
     * Private constructor, use getInstance().
     */
    private SSLConfigManager() {
        JSSEProviderFactory.getInstance();
    }

    /**
     * Access the singleton instance of this class.
     *
     * @return SSLConfigManager
     */
    public static SSLConfigManager getInstance() {
        return Singleton.INSTANCE;
    }

    /***
     * This method parses the configuration.
     *
     * @param map                      Global SSL configuration properties, most likely injected from SSLComponent
     * @param reinitialize             Boolean flag to indicate if the configuration should be re-loaded
     * @param isServer                 Boolean flag to indiciate if the code is running within a server process
     * @param transportSecurityEnabled Boolean flag to indicate if the transportSecurity-1.0 feature is enabled
     * @param aliasPIDs                Map of OSGi PID-indexed repertoire IDs
     * @throws Exception
     ***/
    public synchronized void initializeSSL(Map<String, Object> map,
                                           boolean reinitialize,
                                           boolean isServer, boolean transportSecurityEnabled, Map<String, String> aliasPIDs) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "initializeSSL");

        try {
            if (reinitialize) {
                // clear out the SSL context Cache
                AbstractJSSEProvider.clearSSLContextCache();
            }

            this.aliasPIDs = aliasPIDs;

            transportSecuritySet = transportSecurityEnabled;
            if (transportSecurityEnabled) {
                if (!isSocketFactorySet()) {
                    setSSLSocketFactory();
                }
            }

            // set the server process variable
            isServerProcess = isServer;

            // get all of the top level properties
            loadGlobalProperties(map);

            //Set the default SSL context
            if (!isTransportSecurityEnabled())
                setDefaultSSLContext();

        } catch (

        Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "initializeServerSSL", this);
            throw new SSLException(e);
        }

        checkURLHostNameVerificationProperty(reinitialize);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Total Number of SSLConfigs: " + sslConfigMap.size());
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "initializeSSL");
    }

    public synchronized void addSSLPropertiesToMap(Map<String, Object> properties, boolean transportSecurityEnabled) throws Exception {
        // initialize each SSL configuration into the SSLConfigManager
        String alias = (String) properties.get(LibertyConstants.KEY_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "initializeSSL on " + alias, properties);

        try {
            SSLConfig config = parseSecureSocketLayer(properties, true);

            if (config != null && config.requiredPropertiesArePresent()) {
                config.setProperty(Constants.SSLPROP_ALIAS, alias);
                config.decodePasswords();

                addSSLConfigToMap(alias, config);
                if (unSavedCfgs.contains(alias))
                    unSavedCfgs.remove(alias);
            }

            if (transportSecurityEnabled) {
                Set<String> newConnectionInfo = new HashSet<String>(); // will remove later
                outboundSSL.loadOutboundConnectionInfo(alias, properties, newConnectionInfo);
            }
        } catch (Exception e) {
            unSavedCfgs.add(alias);
            Tr.error(tc, "ssl.protocol.error.CWPKI0833E", new Object[] { alias });
            throw e;
        }

    }

    public synchronized void removeSSLPropertiesFromMap(String alias, boolean transportSecurityEnabled) {
        // initialize each SSL configuration into the SSLConfigManager
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "removing SSL properties from map " + alias);

        sslConfigMap.remove(alias);

        if (transportSecurityEnabled) {
            outboundSSL.removeDynamicSelectionsWithSSLConfig(alias);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "remove SSL properties from map: " + alias);

    }

    /**
     * Called after all the SSL configuration is processed, set the default SSLContext for the runtime.
     *
     * @throws SSLException
     */
    public synchronized void setDefaultSSLContext() throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setDefaultSSLContext");

        if (FrameworkState.isStopping()) {
            return;
        }

        SSLConfig defaultSSLConfig = getDefaultSSLConfig();

        if (defaultSSLConfig != null)
            JSSEProviderFactory.getInstance(null).setServerDefaultSSLContext(defaultSSLConfig);
        else {
            String defaultAlias = getGlobalProperty(Constants.SSLPROP_DEFAULT_ALIAS);
            if (unSavedCfgs.contains(defaultAlias) && !messageIssued) {
                Tr.error(tc, "ssl.config.error.CWPKI0834E", defaultAlias);
                messageIssued = true;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "There is no default SSLConfig.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setDefaultSSLContext");

    }

    public void resetDefaultSSLContextIfNeeded(Collection<File> modifiedFiles) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "resetDefaultSSLContextIfNeeded", modifiedFiles);

        String filePath = null;
        for (File modifiedKeystoreFile : modifiedFiles) {
            try {
                filePath = modifiedKeystoreFile.getCanonicalPath();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception comparing file path.");
                continue;
            }

            resetDefaultSSLContextIfNeeded(filePath);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "resetDefaultSSLContextIfNeeded");

    }

    /**
     * Called after all the SSL configuration is processed, set the default SSLContext for the runtime.
     *
     * @throws SSLException
     */
    public synchronized void resetDefaultSSLContextIfNeeded(String modifiedFile) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "resetDefaultSSLContextIfNeeded", modifiedFile);

        SSLConfig defaultSSLConfig = getDefaultSSLConfig();

        if (defaultSSLConfig != null && keyStoreModified(defaultSSLConfig, modifiedFile)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting new default SSLContext with: " + defaultSSLConfig);
            JSSEProviderFactory.getInstance(null).setServerDefaultSSLContext(defaultSSLConfig);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Modified keystore file are not part of the default SSL configuration.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "resetDefaultSSLContextIfNeeded");

    }

    /**
     * @param defaultSSLConfig
     * @param modifiedFiles
     * @return
     */
    private boolean keyStoreModified(SSLConfig defaultSSLConfig, String modifiedFile) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "keyStoreModified");

        String ksPropValue = defaultSSLConfig.getProperty(Constants.SSLPROP_KEY_STORE, null);
        boolean ksFileBased = Boolean.parseBoolean(defaultSSLConfig.getProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED));
        String ksProp = WSKeyStore.getCannonicalPath(ksPropValue, ksFileBased);
        String tsPropValue = defaultSSLConfig.getProperty(Constants.SSLPROP_TRUST_STORE, null);
        boolean tsFileBased = Boolean.parseBoolean(defaultSSLConfig.getProperty(Constants.SSLPROP_TRUST_STORE_FILE_BASED));
        String tsProp = WSKeyStore.getCannonicalPath(ksPropValue, tsFileBased);

        if ((ksProp != null && ksProp.equals(modifiedFile)) ||
            (tsProp != null && tsProp.equals(modifiedFile))) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "keyStoreModified true");
            return true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "keyStoreModified false");
        return false;
    }

    public String getSystemProperty(final String key) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
    }

    /**
     * Helper method to build the SSLConfig properties from the SecureSocketLayer
     * model object(s).
     *
     * @return SSLConfig
     */
    private SSLConfig parseDefaultSecureSocketLayer() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "parseDefaultSecureSocketLayer");

        SSLConfig sslprops = new SSLConfig();

        String defaultkeyManager = JSSEProviderFactory.getKeyManagerFactoryAlgorithm();
        if (defaultkeyManager != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting default KeyManager: " + defaultkeyManager);
            sslprops.setProperty(Constants.SSLPROP_KEY_MANAGER, defaultkeyManager);
        }

        // TRUST MANAGERS
        String defaultTrustManager = JSSEProviderFactory.getTrustManagerFactoryAlgorithm();
        if (defaultTrustManager != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting default TrustManager: " + defaultTrustManager);
            sslprops.setProperty(Constants.SSLPROP_TRUST_MANAGER, defaultTrustManager);
        }

        // Obtain miscellaneous attributes from system properties
        String sslProtocol = getSystemProperty(Constants.SSLPROP_PROTOCOL);
        if (sslProtocol != null && !sslProtocol.equals("")) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting default SSLProtocol: " + sslProtocol);

            // Use PROTOCOL_TLS_FIPS if FIPS 140-3 is enabled
            if (CryptoUtils.isFips140_3Enabled()) {
                sslprops.setProperty(Constants.SSLPROP_PROTOCOL, Constants.PROTOCOL_TLS_FIPS);

            } else {
                sslprops.setProperty(Constants.SSLPROP_PROTOCOL, sslProtocol);
            }
        }

        String contextProvider = getSystemProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
        if (contextProvider != null && !contextProvider.equals("")) {
            // setting IBMJSSE2 since IBMJSSE and IBMJSSEFIPS is not supported any
            // longer
            if (contextProvider.equalsIgnoreCase(Constants.IBMJSSE_NAME) || contextProvider.equalsIgnoreCase(Constants.IBMJSSEFIPS_NAME)) {
                contextProvider = Constants.IBMJSSE2_NAME;
            }

            sslprops.setProperty(Constants.SSLPROP_CONTEXT_PROVIDER, contextProvider);
        }

        String clientAuthentication = getSystemProperty("com.ibm.CSI.performTLClientAuthenticationRequired");
        if (clientAuthentication != null && !clientAuthentication.equals(""))
            sslprops.setProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION, clientAuthentication);

        String clientAuthenticationSupported = getSystemProperty("com.ibm.CSI.performTLClientAuthenticationSupported");
        if (clientAuthenticationSupported != null && !clientAuthenticationSupported.equals(""))
            sslprops.setProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED, clientAuthenticationSupported);

        String securityLevel = getSystemProperty(Constants.SSLPROP_SECURITY_LEVEL);
        if (securityLevel != null && !securityLevel.equals(""))
            sslprops.setProperty(Constants.SSLPROP_SECURITY_LEVEL, securityLevel);

        String clientKeyAlias = getSystemProperty(Constants.SSLPROP_KEY_STORE_CLIENT_ALIAS);
        if (clientKeyAlias != null && !clientKeyAlias.equals(""))
            sslprops.setProperty(Constants.SSLPROP_KEY_STORE_CLIENT_ALIAS, clientKeyAlias);

        String serverKeyAlias = getSystemProperty(Constants.SSLPROP_KEY_STORE_SERVER_ALIAS);
        if (serverKeyAlias != null && 0 < serverKeyAlias.length())
            sslprops.setProperty(Constants.SSLPROP_KEY_STORE_SERVER_ALIAS, serverKeyAlias);

        String enabledCiphers = getSystemProperty(Constants.SSLPROP_ENABLED_CIPHERS);
        if (enabledCiphers != null && 0 < enabledCiphers.length()) {
            //Removing extra white space
            StringBuffer buf = new StringBuffer();
            String[] ciphers = enabledCiphers.split("\\s+");
            for (int i = 0; i < ciphers.length; i++) {
                buf.append(ciphers[i]);
                buf.append(" ");
            }
            enabledCiphers = buf.toString().trim();
            sslprops.setProperty(Constants.SSLPROP_ENABLED_CIPHERS, enabledCiphers);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Saving SSLConfig." + sslprops.toString());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "parseDefaultSecureSocketLayer");
        return sslprops;
    }

    /**
     * Helper method to build the SSLConfig properties from the SecureSocketLayer
     * model object(s).
     *
     * @param map
     * @param reinitialize
     * @return SSLConfig
     * @throws Exception
     */
    private SSLConfig parseSecureSocketLayer(Map<String, Object> map, boolean reinitialize) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "parseSecureSocketLayer");

        SSLConfig sslprops = new SSLConfig();

        // READ KEYSTORE OBJECT(S)
        WSKeyStore wsks_key = null;
        String keyStoreName = null;
        String alias = null;
        String keyStoreRef = (String) map.get(LibertyConstants.KEY_KEYSTORE_REF);
        if (null != keyStoreRef) {
            wsks_key = KeyStoreManager.getInstance().getKeyStore(keyStoreRef);
        }

        if (wsks_key != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Adding keystore properties from KeyStore object.");
            sslprops.setProperty(Constants.SSLPROP_KEY_STORE_NAME, keyStoreRef);
            addSSLPropertiesFromKeyStore(wsks_key, sslprops);
            //addSSLPorpertiesFromKeyStore actually set the SSL_PROP_KEY_STORE_NAME
            keyStoreName = sslprops.getProperty(Constants.SSLPROP_KEY_STORE_NAME);
        }

        String trustStoreName = (String) map.get(LibertyConstants.KEY_TRUSTSTORE_REF);
        WSKeyStore wsks_trust = null;
        if (null != trustStoreName) {
            wsks_trust = KeyStoreManager.getInstance().getKeyStore(trustStoreName);
        } else {
            trustStoreName = (String) map.get(LibertyConstants.KEY_KEYSTORE_REF);
            wsks_trust = wsks_key;
        }

        if (wsks_trust != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Adding truststore properties from KeyStore object.");
            sslprops.setProperty(Constants.SSLPROP_TRUST_STORE_NAME, trustStoreName);
            addSSLPropertiesFromTrustStore(wsks_trust, sslprops);
        }

        // KEY MANAGER
        String defaultkeyManager = JSSEProviderFactory.getKeyManagerFactoryAlgorithm();
        if (defaultkeyManager != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting default KeyManager: " + defaultkeyManager);
            sslprops.setProperty(Constants.SSLPROP_KEY_MANAGER, defaultkeyManager);
        }

        // TRUST MANAGERS
        String defaultTrustManager = JSSEProviderFactory.getTrustManagerFactoryAlgorithm();

        if (defaultTrustManager != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Setting default TrustManager: " + defaultTrustManager);
            sslprops.setProperty(Constants.SSLPROP_TRUST_MANAGER, defaultTrustManager);
        }

        // MISCELLANEOUS ATTRIBUTES
        String sslProtocol = (String) map.get("sslProtocol");
        if (sslProtocol != null && !sslProtocol.isEmpty()) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "sslProtocol: " + sslProtocol);
                }
                //Print stack trace
                protocolHelper.checkProtocolValueGood(sslProtocol);
                sslprops.setProperty(Constants.SSLPROP_PROTOCOL, sslProtocol);
            } catch (Exception e) {
                throw e;
            }
        }

        String contextProvider = (String) map.get("jsseProvider");
        if (contextProvider != null && !contextProvider.isEmpty()) {
            // setting IBMJSSE2 since IBMJSSE and IBMJSSEFIPS is not supported any
            // longer
            if (contextProvider.equalsIgnoreCase(Constants.IBMJSSE_NAME) || contextProvider.equalsIgnoreCase(Constants.IBMJSSEFIPS_NAME)) {
                contextProvider = Constants.IBMJSSE2_NAME;
            }

            sslprops.setProperty(Constants.SSLPROP_CONTEXT_PROVIDER, contextProvider);
        }

        Boolean clientAuth = (Boolean) map.get("clientAuthentication");
        if (null != clientAuth) {
            sslprops.setProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION, clientAuth.toString());
        }

        Boolean clientAuthSup = (Boolean) map.get("clientAuthenticationSupported");
        if (null != clientAuthSup) {
            sslprops.setProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED, clientAuthSup.toString());
        }

        String prop = (String) map.get("securityLevel");
        if (null != prop && !prop.isEmpty()) {
            sslprops.setProperty(Constants.SSLPROP_SECURITY_LEVEL, prop);
        }

        prop = (String) map.get("clientKeyAlias");
        if (null != prop && !prop.isEmpty()) {
            sslprops.setProperty(Constants.SSLPROP_KEY_STORE_CLIENT_ALIAS, prop);
        }

        prop = (String) map.get("serverKeyAlias");
        if (null != prop && !prop.isEmpty()) {
            sslprops.setProperty(Constants.SSLPROP_KEY_STORE_SERVER_ALIAS, prop);
        }

        prop = (String) map.get("enabledCiphers");
        if (null != prop && !prop.isEmpty()) {
            sslprops.setProperty(Constants.SSLPROP_ENABLED_CIPHERS, prop);
        }

        prop = (String) map.get("id");
        if (null != prop && !prop.isEmpty()) {
            sslprops.setProperty(Constants.SSLPROP_ALIAS, prop);
            alias = prop;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(tc, "keyStoreRef: " + keyStoreRef);
            Tr.debug(tc, "keyStoreName: " + keyStoreName);
            Tr.debug(tc, "sslAlias: " + alias);
        }

        Boolean hostnameVerification = (Boolean) map.get("verifyHostname");
        if (hostnameVerification != null && hostnameVerification) {
            sslprops.setProperty(Constants.SSLPROP_HOSTNAME_VERIFICATION, "true");

            if (SERVER_IDENTITY.equalsIgnoreCase(keyStoreName) &&
                (CONTROLLER_SSL_CONFIG.equalsIgnoreCase(alias) || MEMBER_SSL_CONFIG.equalsIgnoreCase(alias))) {
                boolean isCollectiveCertSanExist = wsks_key.isCollectiveCertSubjectAltNamesExist(wsks_key, keyStoreName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.debug(tc, alias + " isCollectiveCertSanExist: " + isCollectiveCertSanExist);
                }
                if (!isCollectiveCertSanExist) { //collective certificates do not have SAN so disable hostnameVerification
                    sslprops.setProperty(Constants.SSLPROP_HOSTNAME_VERIFICATION, "false");
                    //Tr.warning(tc, "ssl.san.warning.CWPKI0050W", new Object[] { alias, ksName });
                }
            }

            String skipHostnameVerificationForHosts = (String) map.get("skipHostnameVerificationForHosts");
            if (skipHostnameVerificationForHosts != null && !skipHostnameVerificationForHosts.isEmpty())
                sslprops.setProperty(Constants.SSLPROP_SKIP_HOSTNAME_VERIFICATION_FOR_HOSTS, skipHostnameVerificationForHosts);

        } else {
            sslprops.setProperty(Constants.SSLPROP_HOSTNAME_VERIFICATION, "false");
            Tr.warning(tc, "ssl.hnv.disabled.warning.CWPKI0063W", new Object[] { alias });
        }

        String skipHostnameVerificationForHosts = (String) map.get("skipHostnameVerificationForHosts");
        if (skipHostnameVerificationForHosts != null && !skipHostnameVerificationForHosts.isEmpty())
            sslprops.setProperty(Constants.SSLPROP_SKIP_HOSTNAME_VERIFICATION_FOR_HOSTS, skipHostnameVerificationForHosts);

        Boolean useDefaultCerts = (Boolean) map.get("trustDefaultCerts");
        if (null != useDefaultCerts) {
            sslprops.setProperty(Constants.SSLPROP_USE_DEFAULTCERTS, useDefaultCerts.toString());
        }

        Boolean enforceCipherOrder = (Boolean) map.get("enforceCipherOrder");
        if (null != enforceCipherOrder) {
            sslprops.setProperty(Constants.SSLPROP_ENFORCE_CIPHER_ORDER, enforceCipherOrder.toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Saving SSLConfig: " + sslprops);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "parseSecureSocketLayer");
        return sslprops;
    }

    /**
     * Adds all the properties from a WSKeyStore to an SSLConfig.
     *
     * @param wsks
     * @param sslprops
     */
    public synchronized void addSSLPropertiesFromKeyStore(WSKeyStore wsks, SSLConfig sslprops) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "addSSLPropertiesFromKeyStore");

        for (Enumeration<?> e = wsks.propertyNames(); e.hasMoreElements();) {
            String property = (String) e.nextElement();
            String value = wsks.getProperty(property);
            sslprops.setProperty(property, value);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "addSSLPropertiesFromKeyStore: " + sslprops.toString());
    }

    /**
     * Adds all the properties from a WSKeyStore to an SSLConfig.
     *
     * @param wsts
     * @param sslprops
     */
    public synchronized void addSSLPropertiesFromTrustStore(WSKeyStore wsts, SSLConfig sslprops) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "addSSLPropertiesFromTrustStore");

        for (Enumeration<?> e = wsts.propertyNames(); e.hasMoreElements();) {
            String property = (String) e.nextElement();
            String value = wsts.getProperty(property);
            String trustManagerProperty = null;

            if (property.startsWith(Constants.SSLPROP_KEY_STORE)) {
                if (property.length() == Constants.SSLPROP_KEY_STORE.length()) {
                    trustManagerProperty = Constants.SSLPROP_TRUST_STORE;
                } else {
                    trustManagerProperty = Constants.SSLPROP_TRUST_STORE + property.substring(Constants.SSLPROP_KEY_STORE.length());
                }
            }

            if (trustManagerProperty != null && value != null)
                sslprops.setProperty(trustManagerProperty, value);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "addSSLPropertiesFromTrustStore");
    }

    /***
     * Returns a String[] of all SSLConfig aliases for this process.
     *
     * @return String[]
     */
    public synchronized String[] getSSLConfigAliases() {
        return sslConfigMap.keySet().toArray(new String[sslConfigMap.size()]);
    }

    /**
     * Finds an SSLConfig from the Map given an alias.
     *
     * @param alias
     * @return SSLConfig
     * @throws IllegalArgumentException
     */
    public synchronized SSLConfig getSSLConfig(String alias) throws IllegalArgumentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLConfig: " + alias);

        SSLConfig rc = null;
        if (alias == null || alias.equals("")) {
            rc = getDefaultSSLConfig();
        } else {
            // sslConfigMap is not yet populated!!!
            rc = sslConfigMap.get(alias);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getSSLConfig", rc);
        return rc;
    }

    /**
     * Helper method for loading global properties.
     *
     * @param map
     */
    public synchronized void loadGlobalProperties(Map<String, Object> map) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "loadGlobalProperties");

        // clear before reloading.
        globalConfigProperties.clear();

        for (Entry<String, Object> prop : map.entrySet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting global property: " + prop.getKey() + "=" + prop.getValue());
            }
            globalConfigProperties.setProperty(prop.getKey(), (String) prop.getValue());
        }

        // Check for dynamic outbound and default config conflicts
        String outboundDefaultAlias = getGlobalProperty(LibertyConstants.SSLPROP_OUTBOUND_DEFAULT_ALIAS);
        if (outboundDefaultAlias != null && isTransportSecurityEnabled())
            outboundSSL.checkDefaultConflict();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "loadGlobalProperties");
    }

    /**
     * Update SSL configuration with CSIv2 specific SSL settings if endpoint is
     * "ORB_SSL_LISTENER_ADDRESS". This is called by JSSEHelper to update the SSL
     * configuration for specific case.
     *
     * @param config
     * @param connectionInfo
     * @return Properties
     * @throws SSLException
     */
    public synchronized Properties determineIfCSIv2SettingsApply(Properties config, Map<String, Object> connectionInfo) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "determineIfCSIv2SettingsApply", new Object[] { connectionInfo });
        Properties newConfig = null;

        if (connectionInfo != null) {
            String endPointName = (String) connectionInfo.get(Constants.CONNECTION_INFO_ENDPOINT_NAME);
            String direction = (String) connectionInfo.get(Constants.CONNECTION_INFO_DIRECTION);

            if (endPointName != null
                && (endPointName.equals(Constants.ENDPOINT_ORB_SSL_LISTENER_ADDRESS) || endPointName.equals(Constants.ENDPOINT_CSIV2_SERVERAUTH)
                    || endPointName.equals(Constants.ENDPOINT_CSIV2_MUTUALAUTH))
                && direction != null && direction.equals(Constants.DIRECTION_INBOUND)) {
                String sslAlias = globalConfigProperties.getProperty("com.ibm.ssl.csi.inbound.alias");

                if (sslAlias != null && sslAlias.length() > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Getting inbound SSL config with alias: " + sslAlias);
                    newConfig = getProperties(sslAlias);
                }

                if (newConfig != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Cloning CSIv2 alias reference configuration.");
                    newConfig = (Properties) newConfig.clone();
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Cloning JSSEHelper configuration.");
                    newConfig = (Properties) config.clone();
                }

                if (newConfig != null) {
                    String claimSSLClientAuthSupported = globalConfigProperties.getProperty("com.ibm.CSI.claimTLClientAuthenticationSupported");
                    String claimSSLClientAuthRequired = globalConfigProperties.getProperty("com.ibm.CSI.claimTLClientAuthenticationRequired");

                    if (claimSSLClientAuthSupported != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Setting client auth supported: " + claimSSLClientAuthSupported);
                        newConfig.setProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED, claimSSLClientAuthSupported);
                    }

                    if (claimSSLClientAuthRequired != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Setting client auth required: " + claimSSLClientAuthRequired);
                        newConfig.setProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION, claimSSLClientAuthRequired);
                    }

                    /***
                     * UNCOMMENT IF INTEGRITY/CONFIDENTIALITY APPLY FROM CSIV2 CONFIG
                     * String claimSSLIntegritySupported =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageIntegritySupported"); String
                     * claimSSLConfidentialitySupported =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageConfidentialitySupported");
                     *
                     * boolean integrity = false; boolean confidentiality = false; if
                     * (claimSSLIntegritySupported != null &&
                     * claimSSLIntegritySupported.equals(Constants.TRUE)) integrity =
                     * true; if (claimSSLConfidentialitySupported != null &&
                     * claimSSLConfidentialitySupported.equals(Constants.TRUE))
                     * confidentiality = true;
                     *
                     * String claimSSLIntegrityRequired =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageIntegrityRequired"); String
                     * claimSSLConfidentialityRequired =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageConfidentialityRequired");
                     *
                     * if (claimSSLIntegrityRequired != null &&
                     * claimSSLIntegrityRequired.equals(Constants.TRUE)) integrity = true;
                     * if (claimSSLConfidentialityRequired != null &&
                     * claimSSLConfidentialityRequired.equals(Constants.TRUE))
                     * confidentiality = true;
                     *
                     * String securityLevel = Constants.SECURITY_LEVEL_HIGH; if (integrity
                     * && confidentiality) securityLevel = Constants.SECURITY_LEVEL_HIGH;
                     * else if (integrity || confidentiality) securityLevel =
                     * Constants.SECURITY_LEVEL_MEDIUM; else securityLevel =
                     * Constants.SECURITY_LEVEL_LOW;
                     *
                     * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                     * Tr.debug(tc,"Setting security level: " + securityLevel);
                     * newConfig.setProperty(Constants.SSLPROP_SECURITY_LEVEL,
                     * securityLevel);
                     ***/

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "determineIfCSIv2SettingsApply (settings applied)");
                    return newConfig;
                }
            } else if (Constants.ENDPOINT_IIOP.equals(endPointName) && Constants.DIRECTION_OUTBOUND.equals(direction)) {
                String sslAlias = globalConfigProperties.getProperty("com.ibm.ssl.csi.outbound.alias");

                if (sslAlias != null && sslAlias.length() > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Getting outbound SSL config with alias: " + sslAlias);
                    newConfig = getProperties(sslAlias);
                }

                if (newConfig != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Cloning CSIv2 alias reference configuration.");
                    newConfig = (Properties) newConfig.clone();
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Cloning JSSEHelper configuration.");
                    newConfig = (Properties) config.clone();
                }

                if (newConfig != null) {
                    /***
                     * UNCOMMENT IF INTEGRITY/CONFIDENTIALITY APPLY FROM CSIV2 CONFIG
                     * String claimSSLIntegritySupported =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageIntegritySupported"); String
                     * claimSSLConfidentialitySupported =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageConfidentialitySupported");
                     *
                     * boolean integrity = false; boolean confidentiality = false; if
                     * (claimSSLIntegritySupported != null &&
                     * claimSSLIntegritySupported.equals(Constants.TRUE)) integrity =
                     * true; if (claimSSLConfidentialitySupported != null &&
                     * claimSSLConfidentialitySupported.equals(Constants.TRUE))
                     * confidentiality = true;
                     *
                     * String claimSSLIntegrityRequired =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageIntegrityRequired"); String
                     * claimSSLConfidentialityRequired =
                     * globalConfigProperties.getProperty
                     * ("com.ibm.CSI.claimMessageConfidentialityRequired");
                     *
                     * if (claimSSLIntegrityRequired != null &&
                     * claimSSLIntegrityRequired.equals(Constants.TRUE)) integrity = true;
                     * if (claimSSLConfidentialityRequired != null &&
                     * claimSSLConfidentialityRequired.equals(Constants.TRUE))
                     * confidentiality = true;
                     *
                     * String securityLevel = Constants.SECURITY_LEVEL_HIGH; if (integrity
                     * && confidentiality) securityLevel = Constants.SECURITY_LEVEL_HIGH;
                     * else if (integrity || confidentiality) securityLevel =
                     * Constants.SECURITY_LEVEL_MEDIUM; else securityLevel =
                     * Constants.SECURITY_LEVEL_LOW;
                     *
                     * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                     * Tr.debug(tc,"Setting security level: " + securityLevel);
                     * newConfig.setProperty(Constants.SSLPROP_SECURITY_LEVEL,
                     * securityLevel);
                     ***/

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "determineIfCSIv2SettingsApply (settings applied)");
                    return newConfig;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "determineIfCSIv2SettingsApply (original settings)");
        return config;
    }

    /**
     * Method which checks for System properties as a default for the
     * SSLSocketFactory (among other things).
     *
     * @param reinitialize
     * @return Properties
     * @throws Exception
     */
    public synchronized Properties getDefaultSystemProperties(boolean reinitialize) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getDefaultSystemProperties");

        if (!reinitialize) {
            SSLConfig rc = sslConfigMap.get(Constants.DEFAULT_SYSTEM_ALIAS);
            if (null != rc) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getDefaultSystemProperties -> already present.");
                return rc;
            }
        }

        SSLConfig config = parseDefaultSecureSocketLayer();

        if (config != null && config.requiredPropertiesArePresent()) {
            config.setProperty(Constants.SSLPROP_ALIAS, Constants.DEFAULT_SYSTEM_ALIAS);
            config.setProperty(Constants.SSLPROP_CONFIGURL_LOADED_FROM, "System Properties");

            // in case the passwords are set encoded.
            config.decodePasswords();

            SSLConfig oldConfig = sslConfigMap.get(Constants.DEFAULT_SYSTEM_ALIAS);

            // if old SSLConfig does not exist for this alias, then just add it.
            if (oldConfig == null) {
                // loaded for the first time (new config added to file)
                addSSLConfigToMap(Constants.DEFAULT_SYSTEM_ALIAS, config);
            } else {
                if (!oldConfig.equals(config)) {
                    // remove old config
                    removeSSLConfigFromMap(Constants.DEFAULT_SYSTEM_ALIAS, oldConfig);

                    // add new config
                    addSSLConfigToMap(Constants.DEFAULT_SYSTEM_ALIAS, config);
                } else {
                    // do nothing
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "New SSL config equals old SSL config for alias: " + Constants.DEFAULT_SYSTEM_ALIAS);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getDefaultSystemProperties -> found valid system properties");
            return config;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getDefaultSystemProperties -> null");
        return null;
    }

    /***
     * This method returns a default SSL configuration based on the property
     * com.ibm.ssl.defaultAlias set in ssl.client.props or as a System property.
     * If a default alias cannot be found, we will return the first SSL config
     * from the list.
     *
     * @return SSLConfig
     * @throws IllegalArgumentException
     ***/
    public synchronized SSLConfig getDefaultSSLConfig() throws IllegalArgumentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getDefaultSSLConfig");

        SSLConfig defaultSSLConfig = null;
        String defaultAlias = getGlobalProperty(Constants.SSLPROP_DEFAULT_ALIAS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "defaultAlias: " + defaultAlias);

        if (defaultAlias != null) {
            defaultSSLConfig = sslConfigMap.get(defaultAlias);

            if (defaultSSLConfig != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "defaultAlias not null, getDefaultSSLConfig for: " + defaultAlias);
                return defaultSSLConfig;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "defaultAlias is null");
        return null;
    }

    /***
     * This method returns a default SSL configuration based on the property
     * com.ibm.ssl.defaultAlias set in ssl.client.props or as a System property.
     * If a default alias cannot be found, we will return the first SSL config
     * from the list.
     *
     * @return SSLConfig
     * @throws IllegalArgumentException
     ***/
    public synchronized SSLConfig getOutboundDefaultSSLConfig() throws IllegalArgumentException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getOutboundDefaultSSLConfig");

        SSLConfig outboundDefaultSSLConfig = null;
        String outboundDefaultAlias = getGlobalProperty(LibertyConstants.SSLPROP_OUTBOUND_DEFAULT_ALIAS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "outboundDefaultAlias: " + outboundDefaultAlias);

        if (outboundDefaultAlias != null) {
            outboundDefaultSSLConfig = sslConfigMap.get(outboundDefaultAlias);

            if (outboundDefaultSSLConfig != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "outboundDefaultAlias not null, getOutboundDefaultSSLConfig for: " + outboundDefaultAlias);
                return outboundDefaultSSLConfig;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "defaultAlias is null");
        return null;
    }

    /***
     * This method returns a Properties object for a given alias if it can be
     * found in the loaded SSL configurations.
     *
     * @param alias
     * @return Properties
     * @throws IllegalArgumentException
     ***/
    public synchronized Properties getProperties(String alias) throws IllegalArgumentException {
        return getSSLConfig(alias);
    }

    /***
     * This method returns any SSL property which is at the beginning of the
     * ssl.client.props file and thus not associated with a specific SSL config.
     * These properties are considered global properties. An example is
     * com.ibm.security.useFIPS=true.
     *
     * @param name
     * @return String
     ***/
    public synchronized String getGlobalProperty(String name) {
        String value = getSystemProperty(name);

        if (null == value && globalConfigProperties != null) {
            value = globalConfigProperties.getProperty(name);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && value != null)
            Tr.debug(tc, "getGlobalProperty -> " + name + "=" + value);

        return value;
    }

    /***
     * This method returns the value from the above getGlobalProperty call, if not
     * null. Otherwise it returns the default.
     *
     * @param name
     * @param defaultValue
     * @return String
     ***/
    public synchronized String getGlobalProperty(String name, String defaultValue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getGlobalProperty", new Object[] { name, defaultValue });

        String value = getGlobalProperty(name);
        if (value == null) {
            value = defaultValue;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getGlobalProperty -> " + value);
        return value;
    }

    /***
     * This method converts the enabled ciphers property into a String[].
     *
     * @param enabledCiphers
     * @return String[]
     ***/
    public synchronized String[] parseEnabledCiphers(String enabledCiphers) {
        if (enabledCiphers != null)
            return enabledCiphers.split("\\s");

        return null;
    }

    /***
     * This method adjusts the supported ciphers to include those appropriate to
     * the security level (HIGH, MEDIUM, LOW).
     *
     * @param supportedCiphers
     * @param securityLevel
     * @return String[]
     ***/
    public synchronized String[] adjustSupportedCiphersToSecurityLevel(String[] supportedCiphers, String securityLevel) {
        return (Constants.adjustSupportedCiphersToSecurityLevel(supportedCiphers, securityLevel));
    }

    /***
     * This method converts the cipher suite String[] to a space-delimited String.
     *
     * @param cipherList
     * @return String
     ***/
    public synchronized String convertCipherListToString(String[] cipherList) {
        if (cipherList == null || cipherList.length == 0) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < cipherList.length; i++) {
            if (0 < sb.length()) {
                sb.append(' ');
            }
            sb.append(cipherList[i]);
        }

        return sb.toString();
    }

    /***
     * This method masks passwords using asterisks instead of the real characters.
     *
     * @param inString
     * @return String
     ***/
    public synchronized static String mask(String inString) {
        String outString = null;

        if (inString != null) {
            char[] outStringBuffer = new char[inString.length()];

            for (int i = 0; i < inString.length(); i++) {
                outStringBuffer[i] = '*';
            }

            outString = new String(outStringBuffer);
        }

        return outString;
    }

    /***
     * This method adds an SSL config to the SSLConfigManager map and list.
     *
     * @param alias
     * @param sslConfig
     * @throws Exception
     ***/
    public synchronized void removeSSLConfigFromMap(String alias, SSLConfig sslConfig) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "removeSSLConfigFromMap", new Object[] { alias });

        sslConfigMap.remove(alias);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "removeSSLConfigFromMap");
    }

    /***
     * This method adds an SSL config to the SSLConfigManager map and list.
     *
     * @param alias
     * @param sslConfig
     * @throws Exception
     ***/
    public synchronized void clearSSLConfigMap() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "clearSSLConfigMap");

        sslConfigMap.clear();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "clearSSLConfigMap");
    }

    /***
     * This method adds an SSL config from the SSLConfigManager map and list.
     *
     * @param alias
     * @param sslConfig
     * @throws Exception
     ***/
    public synchronized void addSSLConfigToMap(String alias, SSLConfig sslConfig) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "addSSLConfigToMap: alias=" + alias);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, sslConfig.toString());
        }

        if (sslConfigMap.containsKey(alias)) {
            sslConfigMap.remove(alias);
            outboundSSL.removeDynamicSelectionsWithSSLConfig(alias);
        }

        if (validationEnabled())
            sslConfig.validateSSLConfig();

        sslConfigMap.put(alias, sslConfig);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "addSSLConfigToMap");

    }

    /***
     * This method prints all of the SSLConfigs held by the SSLConfigManager. The
     * SSLConfig toString() method does not print passwords.
     ***/
    @Override
    public synchronized String toString() {
        if (sslConfigMap.size() > 0) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("SSLConfigManager configuration: \n");
            for (Entry<String, SSLConfig> current : sslConfigMap.entrySet()) {
                sb.append(current.getKey());
                sb.append("===");
                sb.append(current.getValue().toString());
            }

            return sb.toString();
        }

        return "SSLConfigManager does not contain any SSL configurations.";
    }

    /***
     * This method installs a hostname verification checker that defaults to not
     * check the hostname. If it does not install this hostname verification
     * checker, then any URL connections must have a certificate that matches the
     * host that sent it.
     *
     * @return boolean
     ***/
    public synchronized boolean validationEnabled() {
        // enable/disable hostname verification
        String validate = getGlobalProperty(Constants.SSLPROP_VALIDATION_ENABLED);

        if (validate != null && (validate.equalsIgnoreCase("true") || validate.equalsIgnoreCase("yes"))) {
            return true;
        }

        return false;
    }

    /***
     * This method installs a hostname verification checker that defaults to not
     * check the hostname. If it does not install this hostname verification
     * checker, then any URL connections must have a certificate that matches the
     * host that sent it.
     *
     * @param reinitialize
     ***/
    public synchronized void checkURLHostNameVerificationProperty(boolean reinitialize) {
        // enable/disable hostname verification
        String urlHostNameVerification = getGlobalProperty(Constants.SSLPROP_URL_HOSTNAME_VERIFICATION);

        if (urlHostNameVerification == null || urlHostNameVerification.equalsIgnoreCase("false") || urlHostNameVerification.equalsIgnoreCase("no")) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "com.ibm.ssl.performURLHostNameVerification disabled, disabling HttpsURLConnection default verifier");
            HostnameVerifier verifier = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(verifier);

            if (!reinitialize) {
                Tr.info(tc, "ssl.disable.url.hostname.verification.CWPKI0027I");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "com.ibm.ssl.performURLHostNameVerification enabled");
        }
    }

    /***
     * This method is called to notify any registered listeners whenever an SSL
     * config is removed or changed.
     *
     * @param alias
     * @param state
     ***/
    public synchronized void notifySSLConfigChangeListener(String alias, String state) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "notifySSLConfigChangeListener", new Object[] { alias, state });

        if (alias != null) {
            List<SSLConfigChangeListener> listenerList = sslConfigListenerMap.get(alias);

            if (listenerList != null && listenerList.size() > 0) {
                SSLConfigChangeListener[] listenerArray = listenerList.toArray(new SSLConfigChangeListener[listenerList.size()]);

                for (int i = 0; i < listenerArray.length; i++) {
                    SSLConfigChangeEvent event = null;

                    // get the event associated with the listener
                    event = sslConfigListenerEventMap.get(listenerArray[i]);

                    if (event != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Notifying listener[" + i + "]: " + listenerArray[i].getClass().getName());
                        event.setState(state);
                        SSLConfig changedConfig = sslConfigMap.get(alias);
                        event.setChangedSSLConfig(changedConfig);
                        listenerArray[i].stateChanged(event);

                        if (state.equals(Constants.CONFIG_STATE_DELETED)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Deregistering event for listener.");
                            sslConfigListenerEventMap.remove(listenerArray[i]);
                        }
                    }
                }

                if (state.equals(Constants.CONFIG_STATE_DELETED)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Deregistering all listeners for this alias due to alias deletion.");
                    sslConfigListenerMap.remove(alias);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "notifySSLConfigChangeListener");
    }

    /***
     * This method is called by JSSEHelper to register new listeners for config
     * changes. Notifications get sent when the config changes or gets deleted.
     *
     * @param listener
     * @param event
     ***/
    public synchronized void registerSSLConfigChangeListener(SSLConfigChangeListener listener, SSLConfigChangeEvent event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "registerSSLConfigChangeListener", new Object[] { listener, event });

        List<SSLConfigChangeListener> listenerList = sslConfigListenerMap.get(event.getAlias());

        if (listenerList != null) {
            // used to hold sslconfig -> list of listener references
            listenerList.add(listener);
            sslConfigListenerMap.put(event.getAlias(), listenerList);
        } else {
            listenerList = new ArrayList<SSLConfigChangeListener>();
            listenerList.add(listener);
            sslConfigListenerMap.put(event.getAlias(), listenerList);
        }

        // used to hold listener -> listener event references
        sslConfigListenerEventMap.put(listener, event);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "registerSSLConfigChangeListener");
    }

    /***
     * This method is called by JSSEHelper to deregister listeners.
     *
     * @param listener
     ***/
    public synchronized void deregisterSSLConfigChangeListener(SSLConfigChangeListener listener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "deregisterSSLConfigChangeListener", new Object[] { listener });
        if (null == listener) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "deregisterSSLConfigChangeListener");
            return;
        }

        SSLConfigChangeEvent event = null;

        if (sslConfigListenerEventMap.containsKey(listener))
            event = sslConfigListenerEventMap.get(listener);

        if (event != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Removing listener: " + listener.getClass().getName());

            String alias = event.getAlias();
            if (sslConfigListenerMap.containsKey(alias)) {
                List<SSLConfigChangeListener> listenerList = sslConfigListenerMap.get(alias);
                if (listenerList != null) {
                    int index = listenerList.indexOf(listener);

                    if (index != -1)
                        listenerList.remove(index);
                }
            }

            sslConfigListenerEventMap.remove(listener);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "deregisterSSLConfigChangeListener");
    }

    /**
     * Query the flag on whether this is running in a server process, versus a
     * client process.
     *
     * @return boolean
     */
    public synchronized boolean isServerProcess() {
        return isServerProcess;
    }

    /***
     * This method checks if the client supports/requires SSL client
     * authentication. Returns false if both required and supported are false.
     *
     * @return boolean
     ***/
    public synchronized boolean isClientAuthenticationEnabled() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isClientAuthenticationEnabled");
        boolean auth = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isClientAuthenticationEnabled", Boolean.valueOf(auth));
        return auth;
    }

    /**
     * @param sslProps
     * @param socket
     * @return
     */
    public String[] getCipherList(java.util.Properties props, SSLSocket socket) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getCipherList");

        String ciphers[] = null;
        String cipherString = props.getProperty(Constants.SSLPROP_ENABLED_CIPHERS);

        try {

            if (cipherString != null) {
                ciphers = cipherString.split("\\s+");
            } else {
                String securityLevel = props.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "securityLevel from properties is " + securityLevel);
                if (securityLevel == null)
                    securityLevel = "HIGH";

                ciphers = adjustSupportedCiphersToSecurityLevel(socket.getSupportedCipherSuites(), securityLevel);

            }
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception setting ciphers in SSL Socket Factory.", new Object[] { e });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getCipherList");

        return ciphers;
    }

    /**
     * @param sslProps
     * @param socket
     * @return
     */
    public String[] getCipherList(java.util.Properties props, SSLServerSocket socket) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getCipherList");

        String ciphers[] = null;
        String cipherString = props.getProperty(Constants.SSLPROP_ENABLED_CIPHERS);

        try {

            if (cipherString != null) {
                ciphers = cipherString.split("\\s+");
            } else {
                String securityLevel = props.getProperty(Constants.SSLPROP_SECURITY_LEVEL);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "securityLevel from properties is " + securityLevel);
                if (securityLevel == null)
                    securityLevel = "HIGH";

                ciphers = adjustSupportedCiphersToSecurityLevel(socket.getSupportedCipherSuites(), securityLevel);

            }
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception setting ciphers in SSL Socket Factory.", new Object[] { e });
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getCipherList");

        return ciphers;
    }

    public boolean isTransportSecurityEnabled() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isTransportSecurityEnabled");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "isTransportSecurityEnabled " + transportSecuritySet);
        return transportSecuritySet;

    }

    public boolean isSocketFactorySet() {
        String defaultSSLSocketFactory = null;

        defaultSSLSocketFactory = (String) AccessController.doPrivileged(
                                                                         new java.security.PrivilegedAction<Object>() {
                                                                             @Override
                                                                             public Object run() {
                                                                                 return Security.getProperty(SOCKET_FACTORY_PROP);
                                                                             }
                                                                         });

        if (defaultSSLSocketFactory != null && defaultSSLSocketFactory.equals(SOCKET_FACTORY_CLASS))
            return true;

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.ssl.transport.TransportSecurityService#setSSLSocketFactory()
     */
    public void setSSLSocketFactory() {
        if (isClassAvailable(SOCKET_FACTORY_CLASS)) {
            AccessController.doPrivileged(
                                          new PrivilegedAction<Object>() {
                                              @Override
                                              public Object run() {
                                                  Security.setProperty(SOCKET_FACTORY_PROP,
                                                                       SOCKET_FACTORY_CLASS);
                                                  return null;
                                              }
                                          });
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Socket factory set:" +
                         "com.ibm.ws.kernel.boot.security.SSLSocketFactoryProxy");
    }

    private static boolean isClassAvailable(String ClassName) {
        boolean result = true;
        try {
            Thread.currentThread().getContextClassLoader().loadClass(ClassName);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isClassAvailable", "Unable to load class \"" + ClassName + "\".");
            }
            result = false;
        }
        return result;
    }

    /***
     * This method returns a Properties object where the
     * connection information from the server.xml is used to match to the
     * connectionInfo HashMap passed in as a parameter. The HashMap contains
     * information about the target host/port.
     *
     * @param connectionInfo
     * @return Properties
     ***/
    public Properties getPropertiesFromDynamicSelectionInfo(Map<String, Object> connectionInfo) {
        return outboundSSL.getPropertiesFromDynamicSelectionInfo(connectionInfo);
    }

    public String getPossibleActualID(String aliasPID) {
        return aliasPIDs == null ? null : aliasPIDs.get(aliasPID);
    }

}
