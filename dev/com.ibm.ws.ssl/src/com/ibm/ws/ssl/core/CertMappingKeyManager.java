/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.X509KeyManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.config.SSLConfigManager;
import com.ibm.wsspi.ssl.KeyManagerExtendedInfo;

/**
 * <p>
 * Custom X509KeyManager that provides the capability to handle IP address to
 * Cert Label mapping.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 6.1
 * @since WAS 6.1
 */
public class CertMappingKeyManager implements X509KeyManager, KeyManagerExtendedInfo {
    private static final TraceComponent tc = Tr.register(CertMappingKeyManager.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    public final static String PROTOCOL_HTTPS_CERT_MAPPING_FILE = "com.ibm.ssl.cert.mapping.file";

    public final static String PROTOCOL_HTTPS_CERT_DEFAULT_LABEL = "com.ibm.ssl.cert.default.label";

    private final static String PROTOCOL_HTTPS_CERT_TAG = "SSLServerCert";

    private final static String SINGLE_QUOTE_STRING = "'";
    private final static String DOUBLE_QUOTE_STRING = "\"";

    private Properties certMapping;

    private Properties customProperties;

    private String certDefaultLabel;

    private X509KeyManager defaultX509KeyManager;

    /**
     * Default constructor.
     */
    public CertMappingKeyManager() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "<init>");
        }

        certMapping = new Properties();
        certDefaultLabel = null;
        defaultX509KeyManager = null;
        parseSSLCertFile();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "<init>");
        }
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getClientAliases(java.lang.String,
     * java.security.Principal[])
     */
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return null;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#chooseClientAlias(java.lang.String[],
     * java.security.Principal[], java.net.Socket)
     */
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return null;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getServerAliases(java.lang.String,
     * java.security.Principal[])
     */
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return certMapping.values().toArray(new String[certMapping.size()]);
    }

    /*
     * @see javax.net.ssl.X509KeyManager#chooseServerAlias(java.lang.String,
     * java.security.Principal[], java.net.Socket)
     */
    public String chooseServerAlias(String ipAddress, Principal[] issuers, Socket socket) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "chooseServerAlias", new Object[] { ipAddress, issuers, socket });
        }

        String alias = null;

        // Use connection info
        Map<String, Object> connectionInfo = JSSEHelper.getInstance().getInboundConnectionInfo();
        if (connectionInfo != null) {
            String tmpIpAddress = (String) connectionInfo.get(JSSEHelper.CONNECTION_INFO_CERT_MAPPING_HOST);
            alias = certMapping.getProperty(tmpIpAddress, certDefaultLabel);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "chooseServerAlias: " + alias);
        }
        return alias;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getCertificateChain(java.lang.String)
     */
    public X509Certificate[] getCertificateChain(String alias) {
        return null;
    }

    /*
     * @see javax.net.ssl.X509KeyManager#getPrivateKey(java.lang.String)
     */
    public PrivateKey getPrivateKey(String alias) {
        return null;
    }

    /**
     * Method called by WebSphere Application Server runtime to set the custom
     * properties configured for the custom KeyManager.
     * 
     * @param customProperties
     *            - contains the custom properties configured.
     */
    public void setCustomProperties(Properties customProperties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setCustomProperties", customProperties);

        this.customProperties = customProperties;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setCustomProperties");
    }

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * configuration properties being used for this connection.
     * 
     * @param config
     *            - contains a property for the SSL configuration.
     */
    public void setSSLConfig(Properties config) {
        // do nothing
    }

    /**
     * Method called by WebSphere Application Server runtime to set the default
     * X509KeyManager created by the IbmX509 KeyManagerFactory using the KeyStore
     * information present in this SSL configuration. This allows some delegation
     * to the default IbmX509 KeyManager to occur.
     * 
     * @param defaultX509KeyManager
     *            - default IbmX509 key manager for delegation
     */
    public void setDefaultX509KeyManager(X509KeyManager defaultX509KeyManager) {
        this.defaultX509KeyManager = defaultX509KeyManager;
    }

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * KeyStore used for this connection.
     * 
     * @param keyStore
     *            - the KeyStore currently configured
     */
    public void setKeyStore(KeyStore keyStore) {
        // do nothing
    }

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * KeyStore certificate alias configured for use by server configurations.
     * This method is only called when the alias is configured using the
     * com.ibm.ssl.keyStoreServerAlias property.
     * 
     * @param serverAlias
     *            - the KeyStore server certificate alias currently configured
     */
    public void setKeyStoreServerAlias(String serverAlias) {
        // do nothing
    }

    /**
     * Method called by WebSphere Application Server runtime to set the SSL
     * KeyStore certificate alias configured for use by client configurations.
     * This method is only called when the alias is configured using the
     * com.ibm.ssl.keyStoreClientAlias property.
     * 
     * @param clientAlias
     *            - the KeyStore client certificate alias currently configured
     */
    public void setKeyStoreClientAlias(String clientAlias) {
        // do nothing
    }

    /**
     * Obtain the value for a named property. Search first the custom properties,
     * then the System properties, and then the global properties
     * 
     * @param name
     * @return String
     */
    public String getProperty(final String name) {
        String property = null;
        if (customProperties != null) {
            property = customProperties.getProperty(name);
        }

        if (property == null) {
            property = SSLConfigManager.getInstance().getGlobalProperty(name);
        }

        return property;
    }

    /**
     * Parse Server SSL Certificate Mapping File.
     */
    private void parseSSLCertFile() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "parseSSLCertFile");
        }

        String fileName = null;
        String lineRead = null;
        BufferedReader bufferReader = null;

        // Obtain Default Cert Label
        certDefaultLabel = getProperty(PROTOCOL_HTTPS_CERT_DEFAULT_LABEL);

        // Obtain Cert Mapping File
        try {
            fileName = getProperty(PROTOCOL_HTTPS_CERT_MAPPING_FILE);

            if (fileName != null && 0 < fileName.length()) {
                bufferReader = new BufferedReader(new FileReader(fileName));

                // For each entry, parse the label and ip address
                lineRead = bufferReader.readLine();
                while (lineRead != null) {
                    extractSSLServerCert(lineRead.trim());
                    lineRead = bufferReader.readLine();
                }
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "parseSSLCertFile", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Error parsing file; " + e);
        } finally {
            if (null != bufferReader) {
                try {
                    bufferReader.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "parseSSLCertFile");
        }
    }

    /**
     * This method finds the certificate and associated ip and save cache this
     * pair.
     */
    private void extractSSLServerCert(String lineRead) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "extractSSLServerCert: " + lineRead);
        }

        String tmpString;
        String certLabelString;
        String ipAddressString;

        // Verify if string starts with the SSLServerCert tag
        if (lineRead.startsWith(PROTOCOL_HTTPS_CERT_TAG)) {
            // Obtain cert label
            tmpString = lineRead.substring(PROTOCOL_HTTPS_CERT_TAG.length()).trim();
            if (tmpString.startsWith(SINGLE_QUOTE_STRING) || tmpString.startsWith(DOUBLE_QUOTE_STRING)) {
                // Obtain delimeter
                char delimeterChar = tmpString.charAt(0);
                certLabelString = tmpString.substring(1, tmpString.lastIndexOf(delimeterChar));

                // Obtain ip address
                if (certLabelString != null && certLabelString.length() != 0) {
                    // Move past delimeter to obtain ip address
                    ipAddressString = tmpString.substring(tmpString.lastIndexOf(delimeterChar) + 1).trim();

                    if (ipAddressString != null && ipAddressString.length() != 0) {
                        // Add cert label to properties using ip address as key
                        certMapping.setProperty(ipAddressString, certLabelString);
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "extractSSLServerCert");
        }
    }
}
