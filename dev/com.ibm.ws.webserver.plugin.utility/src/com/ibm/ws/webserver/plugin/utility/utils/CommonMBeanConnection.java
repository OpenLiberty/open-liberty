/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.webserver.plugin.utility.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.ibm.websphere.jmx.connector.rest.ConnectorSettings;
import com.ibm.ws.webserver.plugin.utility.ICommonMBeanConnection;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;

/**
 *
 */
public class CommonMBeanConnection implements ICommonMBeanConnection {

    private final ConsoleWrapper stdin;
    private final PrintStream stdout;

    /**
     * 
     */
    public CommonMBeanConnection(final ConsoleWrapper stdin, final PrintStream stdout) {
        this.stdin = stdin;
        this.stdout = stdout;
    }

    /**
     * Create a custom trust manager which will prompt for trust acceptance.
     * 
     * @return
     */
    private X509TrustManager createPromptingTrustManager() {
        TrustManager[] trustManagers = null;
        try {
            String defaultAlg = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(defaultAlg);
            tmf.init((KeyStore) null);
            trustManagers = tmf.getTrustManagers();
        } catch (KeyStoreException e) {
            // Unable to initialize the default trust managers.
            // This is not a problem as PromptX509rustManager can handle null.
        } catch (NoSuchAlgorithmException e) {
            // Unable to get default trust managers.
            // This is not a problem as PromptX509rustManager can handle null.
        }

        boolean autoAccept = Boolean.valueOf(System.getProperty(SYS_PROP_AUTO_ACCEPT, "false"));
        return new PromptX509TrustManager(stdin, stdout, trustManagers, autoAccept);
    }

    /**
     * Set up the common SSL context for the outbound connection.
     * 
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private SSLSocketFactory setUpSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("SSL");
        ctx.init(null, new TrustManager[] { createPromptingTrustManager() }, null);
        return ctx.getSocketFactory();
    }

    /**
     * Creates the common JMX environment used to connect to the controller.
     * 
     * @param user
     * @param password
     * @return
     */
    private HashMap<String, Object> createJMXEnvironment(final String user, final String password, final SSLSocketFactory sslSF) {
        HashMap<String, Object> environment = new HashMap<String, Object>();
        environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
        environment.put(JMXConnector.CREDENTIALS, new String[] { user, password });
        environment.put(ClientProvider.READ_TIMEOUT, 2 * 60 * 1000);
        environment.put(ConnectorSettings.DISABLE_HOSTNAME_VERIFICATION, Boolean.FALSE);
        environment.put(ConnectorSettings.CUSTOM_SSLSOCKETFACTORY, sslSF);
        environment.put("isCollectiveUtil", Boolean.TRUE);
        return environment;
    }

    /**
     * Get the MBeanServerConnection for the target controller host and port.
     * 
     * @param controllerHost
     * @param controllerPort
     * @param environment
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private JMXConnector getMBeanServerConnection(String controllerHost, int controllerPort, HashMap<String, Object> environment) throws MalformedURLException, IOException {
        JMXServiceURL serviceURL = new JMXServiceURL("REST", controllerHost, controllerPort, "/IBMJMXConnectorREST");
        return new ClientProvider().newJMXConnector(serviceURL, environment);
    }

    /**
     * Returns a connected JMXConnector.
     * 
     * @param controllerHost
     * @param controllerPort
     * @param user
     * @param password
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws MalformedURLException
     * @throws IOException
     */
    public JMXConnector getJMXConnector(String controllerHost, int controllerPort, String user, String password) throws NoSuchAlgorithmException, KeyManagementException, MalformedURLException, IOException {
        HashMap<String, Object> environment = createJMXEnvironment(user, password, setUpSSLContext());
        JMXConnector connector = getMBeanServerConnection(controllerHost, controllerPort, environment);
        connector.connect();
        return connector;
    }

}