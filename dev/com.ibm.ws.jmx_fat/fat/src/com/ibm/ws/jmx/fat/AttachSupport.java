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

import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.ws.jmx.fat.attach.VirtualMachineDescriptorProxy;
import com.ibm.ws.jmx.fat.attach.VirtualMachineProxy;
import com.ibm.ws.jmx.fat.attach.VirtualMachineProxyHelper;

/**
 *
 */
public class AttachSupport {

    private static String LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    MBeanServerConnection mbsc = null;

    public AttachSupport() throws Exception {
        System.setProperty("com.ibm.tools.attach.timeout", "5000");
        List<VirtualMachineDescriptorProxy> vms = VirtualMachineProxyHelper.list();
        System.out.println("Found the following vms: " + vms);

        for (VirtualMachineDescriptorProxy vmd : vms) {
            VirtualMachineProxy vm = null;
            try {
                vm = VirtualMachineProxyHelper.attach(vmd);
            } catch (Exception e) {
                continue;
            }

            System.out.println("Working with vm " + vm);

            try {
                Properties props = vm.getSystemProperties();
                Object bvtServer = props.getProperty("com.ibm.ws.jmx.test.fat");
                System.out.println("Found system property " + bvtServer);

                if (bvtServer != null) {
                    // Search for the value of "localConnectorAddress" within the VM's system properties and agent properties.
                    // Oracle's examples check the agent properties but have been finding in practice that the value is found
                    // from a system property. Trying both in case agent properties are used on some platforms to expose
                    // the value of "localConnectorAddress".
                    String connectorAddr = vm.getSystemProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
                    System.out.println("Local connector address (system property): " + connectorAddr);

                    if (connectorAddr == null) {
                        connectorAddr = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
                        System.out.println("Local connector address (agent property): " + connectorAddr);

                        // It looks like the 'management' agent hasn't been loaded. Try to load it and read the system property again.
                        if (connectorAddr == null) {
                            long start = System.currentTimeMillis();
                            System.out.println("Starting the management agent ..." + start);

                            final String javaHome = vm.getSystemProperties().getProperty("java.home");
                            // Try to load the agent jar from "java.home"/lib. Assumes that "java.home" points to JDK_BASE_DIR/jre.
                            String agent = javaHome + File.separator + "lib" + File.separator + "management-agent.jar";
                            try {
                                vm.loadAgent(agent);
                            } catch (UndeclaredThrowableException e) {
                                Throwable t = e.getCause();
                                if (t != null && "AgentLoadException".equals(t.getClass().getSimpleName())) {
                                    // The agent wasn't found. Perhaps "java.home" is pointing to the JDK_BASE_DIR. Try again with "java.home"/jre/lib.
                                    agent = javaHome + File.separator + "jre" + File.separator + "lib" + File.separator + "management-agent.jar";
                                    vm.loadAgent(agent);
                                } else {
                                    throw e;
                                }
                            }
                            long end = System.currentTimeMillis();
                            System.out.println("Management agent started... " + end + ", took ~" + ((end - start) / 1000) + " seconds");

                            connectorAddr = vm.getSystemProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
                            System.out.println("Local connector address (system property): " + connectorAddr);

                            if (connectorAddr == null) {
                                connectorAddr = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
                                System.out.println("Local connector address (agent property): " + connectorAddr);
                            }
                        }
                    }

                    if (connectorAddr != null) {
                        JMXServiceURL url = new JMXServiceURL(connectorAddr);
                        System.out.println("JMXServiceURL: " + url);

                        JMXConnector connector = JMXConnectorFactory.connect(url);
                        System.out.println("JMXConnector: " + connector);

                        mbsc = connector.getMBeanServerConnection();
                        System.out.println("MBeanServerConnection: " + mbsc);
                    }
                    return;
                }
            } finally {
                try {
                    vm.detach();
                } catch (Exception e) {
                    // Detach failed. Ignore and move on to the next VM.
                }
            }
        }
        throw new RuntimeException("Could not find the server VM");
    }

    public MBeanServerConnection getMBeanServer() {
        return mbsc;
    }
}
