/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.simplicity.log.Log;

/**
 * The basic test class for all the transport security test cases classes
 */
abstract public class AbstractJaxWsTransportSecuritySSLTest extends AbstractJaxWsTransportSecurityBaseTest {
    private static final int REQUEST_TIMEOUT = 10;

    private static final int WAIT_TIME_OUT = 10 * 1000;

    private static final Set<String> SERVER_CONFIG_WITH_SSL = new HashSet<String>();

    protected static final List<String> invalidSSLHandshakeResps = new ArrayList<String>(2);

    protected static final String NO_VALID_TRUST_CERT_IN_CUSTOMIZE_SSL_CONFIG = "noValidTrustCertInCustomizeSSLConfiguration.xml";

    static {
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/basicAuthWithSSL.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/clientCertFailOverToBasicAuthConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/customizeSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/defaultClientCertConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/defaultSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/noTrustStoreInCustomizeSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/noTrustStoreInDefaultSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/noValidTrustCertInCustomizeSSLConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/patchyServerTrustStoreConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/sslRefInSSLDefaultConfiguration.xml");
        SERVER_CONFIG_WITH_SSL.add("serverConfigs/withAliasClientCertConfiguration.xml");

    }

    protected void prepareForTest(String serverConfigFile, String providerWebXMLFile, String clientBindingFile) throws Exception {
        if (dynamicUpdate) {

            updateServerConfigFile(serverConfigFile);
            updateProviderWEBXMLFile(providerWebXMLFile);
            updateClientBndFile(clientBindingFile);
        } else {
            startServer(testName.getMethodName() + ".log", serverConfigFile, providerWebXMLFile, clientBindingFile);
            // check the started of the applications
            server.waitForStringInLog("CWWKZ0001I.*TransportSecurityProvider");
            server.waitForStringInLog("CWWKZ0001I.*TransportSecurityClient");
            if (SERVER_CONFIG_WITH_SSL.contains(serverConfigFile)) {
                server.waitForStringInLog("CWWKO0219I:.*-ssl");
            }
        }
    }

    /**
     * Update the server.xml
     *
     * @param newServerConfigFile the relative path to
     *            publish/files/JaxWsTransportSecurityServer/
     * @throws Exception
     */
    protected static void updateServerConfigFile(String newServerConfigFile) throws Exception {
        updateServerConfigFile(newServerConfigFile, true);

    }

    /**
     * Update the server.xml
     *
     * @param newServerConfigFile
     * @param checkAppUpdate only check for Provider app update when specified
     * @throws Exception
     */
    protected static void updateServerConfigFile(String newServerConfigFile, boolean checkAppUpdate) throws Exception {
        // just log the warning message when the update operation is failed
        boolean warningWhenFail = true;

        if (null == newServerConfigFile) {
            Log.warning(AbstractJaxWsTransportSecuritySSLTest.class,
                        "The server configuration could not be updated as the new configuration file is Null.");
            return;
        }
        try {
            updateSingleFileInServerRoot("server.xml", newServerConfigFile);

            boolean isFound = null != server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*");
            if (!isFound) {
                if (warningWhenFail) {
                    Log.warning(AbstractJaxWsTransportSecuritySSLTest.class, "The server configuration does not update.");
                } else {
                    fail("The server configuration does not update.");
                }
            }

            if (null == lastServerConfig) {// The first time to update server config file
                // Make sure the TransportSecurityProvider app is started/updated
                server.waitForStringInLogUsingMark(
                                                   "CWWKZ0001I.*TransportSecurityProvider | CWWKZ0003I.*TransportSecurityProvider", WAIT_TIME_OUT);

                // Current configuration has SSL feature, check if the ssl port is opened
                if (SERVER_CONFIG_WITH_SSL.contains(newServerConfigFile)) {
                    Log.info(AbstractJaxWsTransportSecuritySSLTest.class, "updateServerConfigFile",
                             "Wait for ssl port open.");
                    isFound = null != server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl");
                }
            } else if (!newServerConfigFile.equals(lastServerConfig)) {// The last server config file is not the same as
                                                                       // the current one
                Log.info(AbstractJaxWsTransportSecuritySSLTest.class, "updateServerConfigFile",
                         "old: -" + lastServerConfig + "-, new: -" + newServerConfigFile + "-");
                if (checkAppUpdate) {
                    // Make sure the TransportSecurityProvider app is started/updated when changing
                    // config
                    server.waitForStringInLogUsingMark(
                                                       "CWWKZ0001I.*TransportSecurityProvider | CWWKZ0003I.*TransportSecurityProvider",
                                                       WAIT_TIME_OUT);
                }

                // Current configuration has SSL feature, but last configuration has no SSL
                // feature, should check if the ssl port is opened.
                if (SERVER_CONFIG_WITH_SSL.contains(newServerConfigFile)) {
                    Log.info(AbstractJaxWsTransportSecuritySSLTest.class, "updateServerConfigFile",
                             "Wait for ssl port open.");
                    isFound = null != server.waitForStringInLogUsingMark("CWWKO0219I:.*-ssl");
                }
            }

            if (!isFound) {
                if (warningWhenFail) {
                    Log.warning(AbstractJaxWsTransportSecuritySSLTest.class, "The SSL port is not opened!");
                } else {
                    fail("The SSL port is not opened!");
                }
            }
        } finally {
            Log.info(AbstractJaxWsTransportSecuritySSLTest.class, "updateServerConfigFile",
                     "The last sever configuration file is: " + lastServerConfig);
            lastServerConfig = newServerConfigFile;
        }
    }
}
