/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.bvt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

import com.ibm.websphere.config.mbeans.ServerXMLConfigurationMBean;
import com.ibm.websphere.filetransfer.FileTransferMBean;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * This test tests the basic functionality of the com.ibm.websphere.config.mbeans.ServerXMLConfigurationMBean.
 * It verifies that fetchConfigurationFilePaths() returns the expected collection of file paths and then checks
 * that the files can be downloaded using the FileTransferMBean from those paths.
 */
public class ServerXMLConfigurationMBeanTest {

    private static String outputDir;
    private static MBeanServerConnection connection;
    private static JMXConnector jmxConnector;
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        String installDir = System.getProperty("install.dir");
        WsLocationAdmin locMgr = (WsLocationAdmin) SharedLocationManager.createDefaultLocations(installDir, getProfile());

        outputDir = locMgr.resolveString("${server.output.dir}");

        // Set up the trust store
        System.setProperty("javax.net.ssl.trustStore", outputDir + "/resources/security/key.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "Liberty");

        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
        environment.put(JMXConnector.CREDENTIALS, new String[] { "theUser", "thePassword" });
        environment.put(ClientProvider.DISABLE_HOSTNAME_VERIFICATION, true);
        environment.put(ClientProvider.READ_TIMEOUT, 2 * 60 * 1000);
        JMXServiceURL url = new JMXServiceURL("REST", "localhost", getSSLPort(), "/IBMJMXConnectorREST");
        jmxConnector = JMXConnectorFactory.connect(url, environment);
        connection = jmxConnector.getMBeanServerConnection();
    }

    @Test
    public void testServerXMLConfigurationMBean() throws Exception {
        ObjectName name = new ObjectName(ServerXMLConfigurationMBean.OBJECT_NAME);
        @SuppressWarnings("unchecked")
        Collection<String> configFilePaths = (Collection<String>) connection.invoke(name, "fetchConfigurationFilePaths", new Object[] {}, new String[] {});

        // Check that the collection contains the expected file paths.
        assertEquals("Config file path collection size is not 2.", 2, configFilePaths.size());
        assertTrue("server.xml is missing from the collection.",
                   configFilePaths.contains("${server.config.dir}/server.xml"));
        assertTrue("bvtTestPorts.xml is missing from the collection.",
                   configFilePaths.contains("${shared.config.dir}/bvtTestPorts.xml"));

        // Download configuration files.
        for (String sourcePath : configFilePaths) {
            String targetPath = outputDir + "/download_target/" +
                                sourcePath.substring(sourcePath.lastIndexOf('/') + 1);

            String[] signature = new String[] { "java.lang.String", "java.lang.String" };
            Object[] params = new Object[] { sourcePath, targetPath };
            ObjectName objName = new ObjectName(FileTransferMBean.OBJECT_NAME);
            connection.invoke(objName, "downloadFile", params, signature);

            // Check that file is not empty
            File file = new File(targetPath);
            assertTrue("Returned file is empty", file.length() > 0);
        }
    }

    private static int getSSLPort() {
        return Integer.valueOf(System.getProperty("HTTP_default.secure", "8020"));
    }

    private static String getProfile() {
        return System.getProperty("profile", "com.ibm.ws.config.bvt.schema");
    }
}
