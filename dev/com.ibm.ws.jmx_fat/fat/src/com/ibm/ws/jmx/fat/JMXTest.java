/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JMXTest {

    @Server("com.ibm.ws.jmx.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Install test feature and bundle
        server.installSystemFeature("jmxtest-1.0");
        server.copyFileToLibertyInstallRoot("lib", "bundles/com.ibm.ws.jmx.fat.jmxtest.jar");

        server.startServer();
        assertNotNull("JMXConnectorServer started not found", server.waitForStringInLog("JMXConnectorServer is ready"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        server.uninstallSystemFeature("jmxtest-1.0");
    }

    @Test
    public void testMBeanConnector() throws Exception {
        ClientConnector cc = new ClientConnector();
        checkMBeanServerConnection(cc.getMBeanServer());
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

    @Test
    @MinimumJavaLevel(javaLevel = 11)
    // Historically this test was disabled because the Attach API is unstable.
    // With Java 11 there is more of a common impl now so we will start running this test above Java 8
    public void testMBeanAttachAPI() throws Exception {
        AttachSupport as = new AttachSupport();
        checkMBeanServerConnection(as.getMBeanServer());
    }

    @Test
    public void testMBeanNotLoaded() throws Exception {
        ClientConnector cc = new ClientConnector();
        MBeanServerConnection server = cc.getMBeanServer();
        ObjectName beanCounterName = new ObjectName("WebSphere", "name", "com.ibm.ws.jmx.test.fat.beanCounter");

        int before = (Integer) server.getAttribute(beanCounterName, "beanCount");

        ObjectName on = new ObjectName("WebSphere", "name", "com.ibm.ws.jmx.test.fat.testBean3");
        @SuppressWarnings("unused")
        MBeanInfo info = server.getMBeanInfo(on);

        int after = (Integer) server.getAttribute(beanCounterName, "beanCount");

        assertTrue(after == before + 1);
    }

    // Simple method that demands the pattern provided matches exactly one mbean
    private MBeanInfo getMBeanInfo(MBeanServerConnection connection, ObjectName pattern) throws Exception {
        Set<ObjectName> mbeans = connection.queryNames(pattern, null);
        assertTrue("Expected one mbean matching pattern " + pattern + ", found: " + mbeans.size(), mbeans.size() == 1);
        ObjectName name = mbeans.iterator().next();
        MBeanInfo result = connection.getMBeanInfo(name);
        assertTrue("No mbean info for " + name, result != null);
        return result;
    }

    @Test
    public void testOSGiMBeans() throws Exception {
        ClientConnector cc = new ClientConnector();
        final MBeanServerConnection server = cc.getMBeanServer();

        //this ObjectName is also ObjectNameConstants.OSGI_CONFIGURATION_ADMIN_MBEAN_NAME
        MBeanInfo info = getMBeanInfo(server, new ObjectName("osgi.compendium:service=cm,version=1.3,*"));
        MBeanOperationInfo[] ops = info.getOperations();

        // Check that only read-only operations are available from ConfigurationAdmin.
        assertTrue(containsOperation(ops, "getProperties"));
        assertTrue(!containsOperation(ops, "deleteForLocation"));
        final String mbeanClassName = info.getClassName();
        assertTrue("com.ibm.ws.jmx.internal.ReadOnlyConfigurationAdmin".equals(mbeanClassName));

        // Sanity check with a few other OSGi JMX MBeans.
        // Ensures the delayed registration is working correctly.
        //this ObjectName is also ObjectNameConstants.OSGI_FRAMEWORK_MBEAN_NAME
        info = getMBeanInfo(server, new ObjectName("osgi.core:type=framework,version=1.7,*"));
        assertTrue(info != null);
        assertTrue(containsOperation(info.getOperations(), "restartFramework"));
        assertTrue(containsOperation(info.getOperations(), "shutdownFramework"));

        //this ObjectName is also ObjectNameConstants.OSGI_BUNDLE_STATE_MBEAN_NAME
        info = getMBeanInfo(server, new ObjectName("osgi.core:type=bundleState,version=1.7,*"));
        assertTrue(info != null);
        assertTrue(containsOperation(info.getOperations(), "listBundles"));

    }

    @Test
    public void testDelayedOSGiMBeans() throws Exception {
        ClientConnector cc = new ClientConnector();
        final MBeanServerConnection server = cc.getMBeanServer();

        ObjectName tester = new ObjectName("WebSphere", "name", "com.ibm.ws.jmx.test.fat.delayedMbeanTester");
        ObjectName bean1 = new ObjectName("WebSphere", "name", "com.ibm.ws.jmx.test.fat.delayedMbeanTester.Bean1");
        ObjectName bean2 = new ObjectName("WebSphere", "name", "com.ibm.ws.jmx.test.fat.delayedMbeanTester.Bean2");
        server.invoke(tester, "register", null, null);

        try {
            server.getMBeanInfo(bean1);
            fail("Expected InstanceNotFoundException");
        } catch (InstanceNotFoundException e) {
            //expected
        }
        server.invoke(tester, "allow1Service", null, null);
        //this should work if delayed mbeans are implemented correctly
        assertNotNull("Expected successful delayed mbean instantiation", server.getMBeanInfo(bean1));

        try {
            server.getMBeanInfo(bean2);
            fail("Expected InstanceNotFoundException");
        } catch (InstanceNotFoundException e) {
            //expected
        }

        server.invoke(tester, "unregister", null, null);
    }

    private boolean containsOperation(MBeanOperationInfo[] ops, String name) {
        for (MBeanOperationInfo op : ops) {
            if (name.equals(op.getName())) {
                return true;
            }
        }
        return false;
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
