package com.ibm.ws.jmx.fat;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

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

/**
 *
 */
public class ClientConnector {

    MBeanServerConnection mbsc = null;

    public ClientConnector() {
        int port = Integer.valueOf(System.getProperty("JMXTest", "8999"));
        String URL = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/server";
        System.out.println("JMX ClientConnector URL " + URL);

        JMXServiceURL url;
        try {
            url = new JMXServiceURL(URL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        JMXConnector jmxc;
        try {
            jmxc = JMXConnectorFactory.connect(url, null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            mbsc = jmxc.getMBeanServerConnection();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public MBeanServerConnection getMBeanServer() {
        return mbsc;
    }
}
