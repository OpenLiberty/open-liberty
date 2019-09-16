/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.jmx.test.fat.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.websphere.simplicity.log.Log;

public class ClientConnector {

    private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";

    private MBeanServerConnection mbsc = null;

    private final static Class<?> thisClass = ClientConnector.class;

    private JMXConnector connector = null;
    private JMXConnector connector2 = null;

    public ClientConnector(String serverRoot, String serverHost, int securePort) throws IOException {
        String thisMethod = "ClientConnector(String, String, int)";

        System.setProperty("javax.net.ssl.trustStore", serverRoot + "/resources/security/serverKey.jks");
        Log.info(thisClass, thisMethod, "@TJJ trustStore location: " + serverRoot + "/resources/security/serverKey.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "passw0rd");

        try {
            HashMap<String, Object> environment = new HashMap<String, Object>();
            environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
            environment.put(JMXConnector.CREDENTIALS, new String[] { "admin", "password" });

            JMXServiceURL url = new JMXServiceURL("REST", serverHost, securePort, "/IBMJMXConnectorREST");
            Log.info(thisClass, thisMethod, "@TJJ URL: " + url.toString());
            connector2 = JMXConnectorFactory.connect(url, environment);
            mbsc = connector2.getMBeanServerConnection();
        } catch (Throwable t) {
            Log.error(thisClass, thisMethod, t);
        }

    }

    public ClientConnector(String serverRoot) throws IOException {

        if (serverRoot == null || serverRoot.length() == 0) {
            throw new RuntimeException("invalid server root");
        }

        serverRoot = serverRoot.replaceAll("\\\\", "/");
        String connectorFile = serverRoot + "/workarea/" + CONNECTOR_ADDRESS_FILE_NAME;

        File file = new File(connectorFile);
        if (!file.exists()) {
            return;
        }

        String connectorAddr = null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            connectorAddr = br.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        if (connectorAddr != null) {
            JMXServiceURL url = new JMXServiceURL(connectorAddr);
            connector = JMXConnectorFactory.connect(url);
            mbsc = connector.getMBeanServerConnection();
        }

    }

    public MBeanServerConnection getMBeanServer() {
        return mbsc;
    }
}
