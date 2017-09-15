/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ssl.core;

import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.config.KeyStoreManager;
import com.ibm.ws.ssl.config.SSLConfigManager;
import com.ibm.ws.ssl.config.WSKeyStore;
import com.ibm.wsspi.ssl.KeyManagerExtendedInfo;

/**
 * WebSphere extensions on an X509ExtendedKeyManager.
 * <p>
 * This class is the KeyManager wrapper that delegates to the "real" KeyManager
 * configured for the system.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public final class WSX509KeyManager extends X509ExtendedKeyManager implements X509KeyManager {
    private static final TraceComponent tc = Tr.register(WSX509KeyManager.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private SSLConfig config = null;
    private KeyStore ks = null;
    private KeyManager kmList[] = null;
    private X509KeyManager km = null;
    private X509KeyManager customKM = null;
    private CertMappingKeyManager certMappingKeyManager = null;
    private String clientAlias = null;
    private String serverAlias = null;
    private WSKeyStore wsks = null;
    private String keyStoreName = null;

    /**
     * Constructor.
     * 
     * @param keystore
     * @param passPhrase
     * @param kmf
     * @param sslConfig
     * @param customKeyManager
     */
    public WSX509KeyManager(KeyStore keystore, char passPhrase[], KeyManagerFactory kmf, SSLConfig sslConfig, X509KeyManager customKeyManager) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "WSX509KeyManager");
        ks = keystore;
        kmList = kmf.getKeyManagers();
        certMappingKeyManager = new CertMappingKeyManager();

        if (kmList != null) {
            km = (X509KeyManager) kmList[0];
        }

        config = sslConfig;
        customKM = customKeyManager;

        // get WSKeyStore for this keystore
        keyStoreName = config.getProperty(Constants.SSLPROP_KEY_STORE_NAME);
        if (keyStoreName != null) {
            wsks = KeyStoreManager.getInstance().getKeyStore(keyStoreName);
        }

        if (customKM != null && customKM instanceof KeyManagerExtendedInfo) {
            ((KeyManagerExtendedInfo) customKM).setSSLConfig(config);

            KeyManager[] mgrs = kmf.getKeyManagers();
            X509KeyManager defaultX509keyManager = null;

            if (mgrs != null && mgrs[0] != null)
                defaultX509keyManager = (X509KeyManager) mgrs[0];

            if (defaultX509keyManager != null)
                ((KeyManagerExtendedInfo) customKM).setDefaultX509KeyManager(defaultX509keyManager);

            if (keystore != null)
                ((KeyManagerExtendedInfo) customKM).setKeyStore(keystore);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "WSX509KeyManager");
    }

    /**
     * Set the client alias value for the given slot number.
     * 
     * @param alias
     * @param slotnum
     * @throws Exception
     */
    public void setClientAlias(String alias) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setClientAlias", new Object[] { alias });
        if (!ks.containsAlias(alias)) {
            String keyFileName = config.getProperty(Constants.SSLPROP_KEY_STORE);
            String tokenLibraryFile = config.getProperty(Constants.SSLPROP_TOKEN_LIBRARY);
            String location = keyFileName != null ? keyFileName : tokenLibraryFile;

            String message = TraceNLSHelper.getInstance().getFormattedMessage("ssl.client.alias.not.found.CWPKI0023E", new Object[] { alias, location },
                                                                              "Client alias " + alias + " not found in keystore.");
            Tr.error(tc, "ssl.client.alias.not.found.CWPKI0023E", new Object[] { alias, location });
            throw new IllegalArgumentException(message);
        }
        this.clientAlias = alias;

        if (customKM != null && customKM instanceof KeyManagerExtendedInfo)
            ((KeyManagerExtendedInfo) customKM).setKeyStoreClientAlias(alias);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setClientAlias");
    }

    /**
     * Set the server alias for the given slot number.
     * 
     * @param alias
     * @param slotnum
     * @throws Exception
     */
    public void setServerAlias(String alias) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setServerAlias", new Object[] { alias });
        if (!ks.containsAlias(alias)) {
            String keyFileName = config.getProperty(Constants.SSLPROP_KEY_STORE);
            String tokenLibraryFile = config.getProperty(Constants.SSLPROP_TOKEN_LIBRARY);
            String location = keyFileName != null ? keyFileName : tokenLibraryFile;

            String message = TraceNLSHelper.getInstance().getFormattedMessage("ssl.server.alias.not.found.CWPKI0024E", new Object[] { alias, location },
                                                                              "Server alias " + alias + " not found in keystore.");
            Tr.error(tc, "ssl.server.alias.not.found.CWPKI0024E", new Object[] { alias, location });
            throw new IllegalArgumentException(message);
        }
        this.serverAlias = alias;

        if (customKM != null && customKM instanceof KeyManagerExtendedInfo)
            ((KeyManagerExtendedInfo) customKM).setKeyStoreServerAlias(alias);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setServerAlias");
    }

    /*
     * @see javax.net.ssl.X509KeyManager#chooseClientAlias(java.lang.String[],
     * java.security.Principal[], java.net.Socket)
     */
    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "chooseClientAlias", new Object[] { keyType, issuers, socket });

        String alias = null;
        try {
            if (customKM != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "chooseClientAlias -> " + customKM.getClass().getName());
                return customKM.chooseClientAlias(keyType, issuers, socket);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "chooseClientAlias");
            for (String type : keyType) {
                alias = chooseClientAlias(type, issuers);
                if (alias != null) {
                    break;
                }
            }
            return alias;
        } catch (Throwable t) {
            // here we want to try to catch any runtime exceptions that might occur,
            // print a message and rethrow
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception in chooseClientAlias.", new Object[] { t });
            FFDCFilter.processException(t, getClass().getName(), "chooseClientAlias", this);

            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    /*
     * @see javax.net.ssl.X509KeyManager#chooseServerAlias(java.lang.String,
     * java.security.Principal[], java.net.Socket)
     */
    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "chooseServerAlias", new Object[] { keyType, issuers, socket });

        try {

            if (customKM != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "chooseServerAlias -> " + customKM.getClass().getName());
                return customKM.chooseServerAlias(keyType, issuers, socket);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "chooseServerAlias");
            return chooseServerAlias(keyType, issuers);
        } catch (Throwable t) {
            // here we want to try to catch any runtime exceptions that might occur,
            // print a message and rethrow
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception in chooseServerAlias.", new Object[] { t });
            FFDCFilter.processException(t, getClass().getName(), "chooseServerAlias", this);

            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    /**
     * Choose a client alias.
     * 
     * @param keyType
     * @param issuers
     * @return String
     */
    public String chooseClientAlias(String keyType, Principal[] issuers) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "chooseClientAlias", new Object[] { keyType, issuers });

        Map<String, Object> connectionInfo = JSSEHelper.getInstance().getOutboundConnectionInfo();

        // if SSL client auth is disabled do not return a client alias
        if (connectionInfo != null && Constants.ENDPOINT_IIOP.equals(connectionInfo.get(Constants.CONNECTION_INFO_ENDPOINT_NAME))
            && !SSLConfigManager.getInstance().isClientAuthenticationEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "chooseClientAlias: null");
            return null;
        } else if (clientAlias != null && !clientAlias.equals("")) {
            String[] list = km.getClientAliases(keyType, issuers);

            if (list != null) {
                boolean found = false;
                for (int i = 0; i < list.length && !found; i++) {
                    if (clientAlias.equalsIgnoreCase(list[i]))
                        found = true;
                }

                if (found) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "chooseClientAlias", new Object[] { clientAlias });

                    // JCERACFKS and JCECCARACFKS support mixed case, if we see that type,
                    // do not lowercase the alias
                    if (ks.getType() != null
                        && (ks.getType().equals(Constants.KEYSTORE_TYPE_JCERACFKS) || ks.getType().equals(Constants.KEYSTORE_TYPE_JCECCARACFKS) || ks.getType().equals(Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS))) {
                        return clientAlias;
                    }
                    return clientAlias.toLowerCase();
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "chooseClientAlias (default)", new Object[] { clientAlias });
            // error case, alias not found in the list.
            return clientAlias;
        } else {
            String[] keyArray = new String[] { keyType };
            String alias = km.chooseClientAlias(keyArray, issuers, null);

            // JCERACFKS and JCECCARACFKS support mixed case, if we see that type, do
            // not lowercase the alias
            if (ks.getType() != null
                && (!ks.getType().equals(Constants.KEYSTORE_TYPE_JCERACFKS) && !ks.getType().equals(Constants.KEYSTORE_TYPE_JCECCARACFKS) && !ks.getType().equals(Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS))) {
                if (alias != null) {
                    alias = alias.toLowerCase();
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "chooseClientAlias (from JSSE)", new Object[] { alias });
            return alias;
        }
    }

    /**
     * Handshakes that use the SSLEngine and not an SSLSocket require this method
     * from the extended X509KeyManager.
     * 
     * @see javax.net.ssl.X509ExtendedKeyManager#chooseEngineServerAlias(java.lang.String, java.security.Principal[], javax.net.ssl.SSLEngine)
     */
    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "chooseEngineServerAlias", new Object[] { keyType, issuers, engine });

        String rc = null;
        if (null != customKM && customKM instanceof X509ExtendedKeyManager) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "chooseEngineServerAlias, using customKM -> " + customKM.getClass().getName());
            rc = ((X509ExtendedKeyManager) customKM).chooseEngineServerAlias(keyType, issuers, engine);
        } else {
            rc = chooseServerAlias(keyType, issuers);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "chooseEngineServerAlias: " + rc);
        return rc;
    }

    /**
     * Handshakes that use the SSLEngine and not an SSLSocket require this method
     * from the extended X509KeyManager.
     * 
     * @see javax.net.ssl.X509ExtendedKeyManager#chooseEngineClientAlias(java.lang.String[], java.security.Principal[], javax.net.ssl.SSLEngine)
     */
    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "chooseEngineClientAlias", new Object[] { keyType, issuers, engine });

        String rc = null;
        if (null != customKM && customKM instanceof X509ExtendedKeyManager) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "chooseEngineClientAlias, using customKM -> " + customKM.getClass().getName());
            rc = ((X509ExtendedKeyManager) customKM).chooseEngineClientAlias(keyType, issuers, engine);
        } else {
            rc = chooseClientAlias(keyType[0], issuers);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "chooseEngineClientAlias");
        return rc;
    }

    /**
     * Choose a server alias.
     * 
     * @param keyType
     * @param issuers
     * @return String
     */
    public String chooseServerAlias(String keyType, Principal[] issuers) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "chooseServerAlias", new Object[] { keyType, issuers });

        Map<String, Object> connectionInfo = JSSEHelper.getInstance().getInboundConnectionInfo();
        String certMappingFile = certMappingKeyManager.getProperty(CertMappingKeyManager.PROTOCOL_HTTPS_CERT_MAPPING_FILE);
        String mappedAlias = null;
        Boolean webContainerInbound = null;
        if (connectionInfo != null)
            webContainerInbound = (Boolean) connectionInfo.get(JSSEHelper.CONNECTION_INFO_IS_WEB_CONTAINER_INBOUND);

        if (webContainerInbound != null && webContainerInbound.booleanValue() && certMappingFile != null) {
            mappedAlias = certMappingKeyManager.chooseServerAlias(keyType, issuers, null);
        }

        if (mappedAlias == null) {
            if (serverAlias != null && !serverAlias.equals("")) {
                String[] list = km.getServerAliases(keyType, issuers);

                if (list != null) {
                    boolean found = false;
                    for (int i = 0; i < list.length && !found; i++) {
                        if (serverAlias.equalsIgnoreCase(list[i]))
                            found = true;

                    }

                    if (found) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            Tr.exit(tc, "chooseServerAlias", new Object[] { serverAlias });

                        // JCERACFKS and JCECCARACFKS support mixed case, if we see that
                        // type, do not lowercase the alias
                        if (ks.getType() != null
                            && (ks.getType().equals(Constants.KEYSTORE_TYPE_JCERACFKS) || ks.getType().equals(Constants.KEYSTORE_TYPE_JCECCARACFKS) || ks.getType().equals(Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS))) {
                            return serverAlias;
                        }
                        return serverAlias.toLowerCase();
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "chooseServerAlias (default)", new Object[] { serverAlias });
                return serverAlias;
            }
            String alias = km.chooseServerAlias(keyType, issuers, null);

            // JCERACFKS and JCECCARACFKS support mixed case, if we see that type, do
            // not lowercase the alias
            if (ks.getType() != null
                && (!ks.getType().equals(Constants.KEYSTORE_TYPE_JCERACFKS) && !ks.getType().equals(Constants.KEYSTORE_TYPE_JCECCARACFKS) && !ks.getType().equals(Constants.KEYSTORE_TYPE_JCEHYBRIDRACFKS))) {
                if (alias != null) {
                    alias = alias.toLowerCase();
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "chooseServerAlias (from JSSE)", new Object[] { alias });
            return alias;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "chooseServerAlias", new Object[] { mappedAlias });
        return mappedAlias;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getClientAliases(java.lang.String,
     * java.security.Principal[])
     */
    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getClientAliases", new Object[] { keyType, issuers });

        String[] rc = getX509KeyManager().getClientAliases(keyType, issuers);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getClientAliases", rc);
        return rc;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getServerAliases(java.lang.String,
     * java.security.Principal[])
     */
    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getServerAliases", new Object[] { keyType, issuers });

        String[] rc = getX509KeyManager().getServerAliases(keyType, issuers);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getServerAliases", rc);
        return rc;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getPrivateKey(java.lang.String)
     */
    @Override
    public PrivateKey getPrivateKey(String s) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getPrivateKey", new Object[] { s });

        PrivateKey rc = getX509KeyManager().getPrivateKey(s);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getPrivateKey -> " + (null != rc));
        return rc;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getCertificateChain(java.lang.String)
     */
    @Override
    public X509Certificate[] getCertificateChain(String s) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getCertificateChain: " + s);

        X509Certificate[] rc = getX509KeyManager().getCertificateChain(s);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getCertificateChain", rc);
        return rc;
    }

    /**
     * Get the appropriate X509KeyManager for this instance.
     * 
     * @return X509KeyManager
     */
    public X509KeyManager getX509KeyManager() {
        if (customKM != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getX509KeyManager -> " + customKM.getClass().getName());
            return customKM;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getX509KeyManager -> " + km.getClass().getName());
        return km;
    }

}