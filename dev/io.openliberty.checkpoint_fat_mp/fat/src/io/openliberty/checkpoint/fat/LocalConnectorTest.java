/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.fat.jmx.LocalConnector;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class LocalConnectorTest {

    @Server("checkpointLocalConnector")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Install test feature and bundle
        server.installSystemFeature("jmxtest-1.0");
        server.copyFileToLibertyInstallRoot("lib", "bundles/com.ibm.ws.jmx.fat.jmxtest.jar");

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START);
        server.startServer();
        assertNotNull("JMXConnectorServer started not found", server.waitForStringInLog("JMXConnectorServer is ready"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        server.uninstallSystemFeature("jmxtest-1.0");
    }

    @Test
    public void testMBeanLocalConnector() throws Exception {
        String serverRoot = server.getServerRoot();
        LocalConnector lc = new LocalConnector(serverRoot);
        checkMBeanServerConnection(lc.getMBeanServer());

        compareFileContent(lc.getStateFile(), lc.getWorkAreaFile());
    }

    private void compareFileContent(File stateFile, File workAreaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(stateFile));
        String stateLine = reader.readLine();
        reader.close();
        reader = new BufferedReader(new FileReader(workAreaFile));
        String workAreaLine = reader.readLine();
        reader.close();

        assertEquals("The content of the local address file in the logs/state and workarea files should be the same", stateLine, workAreaLine);
    }

    private void checkMBeanServerConnection(MBeanServerConnection server) throws Exception {
        assertNotNull("Server must not be null", server);

        int numBeans = server.getMBeanCount();
        assertTrue("Number of beans shoud be greater than or equal to 2, numBeans=" + numBeans, numBeans >= 2);

        Set<ObjectName> set = server.queryNames(null, null);

        ObjectName on1 = new ObjectName("WebSphere", "name", "com.ibm.ws.jmx.test.fat.testBean1");
        ObjectName on2 = new ObjectName("WebSphere", "name", "com.ibm.ws.jmx.test.fat.testBean2");

        assertTrue("Set must contain object name '" + on1 + "': found " + set, set.contains(on1));
        assertTrue("Set must contain object name '" + on1 + "': found " + set, set.contains(on2));

        MBeanInfo info = server.getMBeanInfo(on1);
        assertNotNull("MBeanInfo for object name 1 must not be null", info);

        info = server.getMBeanInfo(on2);
        assertNotNull("MBeanInfo for object name 2 must not be null", info);

        MBeanServerDelegateMBean serverDelegate = JMX.newMBeanProxy(server, MBeanServerDelegate.DELEGATE_NAME, MBeanServerDelegateMBean.class);
        System.out.println("Server delegate: " + serverDelegate);
        System.out.println("Mbean server id: " + serverDelegate.getMBeanServerId());
        assertTrue("Expected that server ID starts with WebSphere",
                   serverDelegate.getMBeanServerId().startsWith("WebSphere"));
    }
}
