/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.fat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 *
 */
public class LocalConnector {

    private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";

    MBeanServerConnection mbsc = null;

    private File stateFile;
    private File workAreaFile;

    public LocalConnector(String serverRoot) throws IOException {
        if (serverRoot == null || serverRoot.length() == 0) {
            throw new RuntimeException("server.root property is not set");
        }
        serverRoot = serverRoot.replaceAll("\\\\", "/");
        String connectorFile = serverRoot + "/logs/state/" + CONNECTOR_ADDRESS_FILE_NAME;
        System.out.println(connectorFile);

        stateFile = new File(connectorFile);
        workAreaFile = new File(serverRoot, "workarea/" + CONNECTOR_ADDRESS_FILE_NAME);

        if (stateFile.exists()) {
            String connectorAddr = null;
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(stateFile), "UTF-8"));
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
                System.out.println("JMX connector address:  " + connectorAddr);
                JMXConnector connector = null;
                try {
                    JMXServiceURL url = new JMXServiceURL(connectorAddr);
                    connector = JMXConnectorFactory.connect(url);
                    System.out.println("JMX Connector: " + connector);
                    mbsc = connector.getMBeanServerConnection();
                    if (mbsc != null) {
                        return; //Successful, return.
                    }
                } catch (IOException ioe) {
                    if (connector != null) {
                        try {
                            connector.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    throw (ioe);
                }
            } else {
                System.out.println("JMXConnection: JMX connector address is null. The connector address file is " + stateFile.getAbsolutePath());
            }
        } else {
            System.out.println("JMXConnection: JMX address file doesn't exist. The connector address file is " + stateFile.getAbsolutePath());
        }
    }

    public MBeanServerConnection getMBeanServer() {
        return mbsc;
    }

    public File getStateFile() {
        return stateFile;
    }

    public File getWorkAreaFile() {
        return workAreaFile;
    }
}
