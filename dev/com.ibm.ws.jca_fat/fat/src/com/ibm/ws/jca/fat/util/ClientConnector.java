/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ClientConnector {

    private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";

    MBeanServerConnection mbsc = null;

    public ClientConnector(String serverRoot) throws IOException {
        if (serverRoot == null || serverRoot.length() == 0) {
            throw new RuntimeException("invalid server root");
        }
        serverRoot = serverRoot.replaceAll("\\\\", "/");
        String connectorFile = serverRoot + "/workarea/" + CONNECTOR_ADDRESS_FILE_NAME;
        System.out.println(connectorFile);

        File file = new File(connectorFile);
        if (file.exists()) {
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
                System.out.println("JMX connector address:  " + connectorAddr);
                JMXConnector connector = null;

                JMXServiceURL url = new JMXServiceURL(connectorAddr);
                connector = JMXConnectorFactory.connect(url);
                System.out.println("JMX Connector: " + connector);
                mbsc = connector.getMBeanServerConnection();
                if (mbsc != null) {
                    return; //Successful, return.
                }

            } else {
                System.out.println("JMXConnection: JMX connector address is null. The connector address file is " + file.getAbsolutePath());
            }
        } else {
            System.out.println("JMXConnection: JMX address file doesn't exist. The connector address file is " + file.getAbsolutePath());
        }

    }

    public MBeanServerConnection getMBeanServer() {
        return mbsc;
    }
}
