/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.kernel.server.ServerEndpointControlMBean;
//import com.ibm.websphere.kernel.server.ServerEndpointControlMBean;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * This test exercises the basic functionality of the ServerEndpointControlMBean by
 * performing tests similar to those of the PauseResumeCommandTest except through the
 * mbean interface instead of the server command
 *
 */
@RunWith(FATRunner.class)
public class ServerEndpointControlMBeanTest {

    private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";
    private static final String PAUSEABLE_EXCEPTION_CLASS = "com.ibm.ws.kernel.launch.service.PauseableComponentControllerRequestFailedException";
    private static final Class<?> c = ServerEndpointControlMBeanTest.class;

    private static JMXConnector connector = null;
    private final Long logWaitTimeout = 10000L;

    @Rule
    public TestName testName = new TestName();

    @Server("com.ibm.ws.kernel.service")
    public static LibertyServer server;

    private static ServerEndpointControlMBean mbean = null;

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
        // set up local connector
        MBeanServerConnection mbsc = getMBeanServer(server.getServerRoot()); //mbsc = getMBeanServer(server.getServerRoot());
        // get ON of the desired mbean
        ObjectName on = getMBeanObjectInstance(mbsc).getObjectName(); //on = getMBeanObjectInstance(mbsc).getObjectName();
        // create an proxy for the mbean
        mbean = JMX.newMBeanProxy(mbsc, on, ServerEndpointControlMBean.class);
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (connector != null) {
            connector.close();
        }
        server.stopServer("CWWKE093*");
    }

    /**
     * Tests that the expected exception is thrown if the MBean pause method is issued with no targets when no pauseable components exist
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testPauseAllNoPauseableComponents() throws Exception {
        final String METHOD_NAME = "testPauseAllNoPauseableComponents";
        Log.entering(c, METHOD_NAME);

        try {
            mbean.pause();
            fail("Didn't get expected exception from attempting to pause a server with no pauseable components");

        } catch (MBeanException e) {
        }
        assertNotNull("Didn't find CWWKE0933W in logs as expected.", server.waitForStringInLog("CWWKE0933W", logWaitTimeout));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean pause method is issued with invalid target list when no pauseable components exist
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testPauseInvalidTargetList() throws Exception {
        final String METHOD_NAME = "testPauseInvalidTargetList";
        Log.entering(c, METHOD_NAME);

        try {
            mbean.pause(null);
            fail("Didn't get expected exception from attempting to pause a server with an invalid target list");

        } catch (MBeanException e) {
        }
        assertNotNull("Didn't find CWWKE0931W in logs as expected.", server.waitForStringInLog("CWWKE0931W", logWaitTimeout));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean resume method is issued when no pauseable components exist
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testResumeAllNoPauseableComponents() throws Exception {
        final String METHOD_NAME = "testResumeAllNoPauseableComponents";
        Log.entering(c, METHOD_NAME);

        try {
            mbean.resume();
            fail("Didn't get expected exception from attempting to resume a server with no pauseable components");

        } catch (MBeanException e) {
        }
        assertNotNull("Didn't find CWWKE0934W in logs as expected.", server.waitForStringInLog("CWWKE0934W", logWaitTimeout));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean pause method is issued with invalid target list when no pauseable components exist
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testResumeInvalidTargetList() throws Exception {
        final String METHOD_NAME = "testResumeInvalidTargetList";
        Log.entering(c, METHOD_NAME);

        try {
            mbean.resume("");
            fail("Didn't get expected exception from attempting to resume a server with an invalid target list");

        } catch (MBeanException e) {
        }
        assertNotNull("Didn't find CWWKE0932W in logs as expected.", server.waitForStringInLog("CWWKE0932W", logWaitTimeout));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean pause method is issued with an invalid target.
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testPauseInvalidTarget() throws Exception {
        final String METHOD_NAME = "testPauseInvalidTarget";
        Log.entering(c, METHOD_NAME);

        try {
            mbean.pause("inValidTargetName!");
            fail("Didn't get expected exception from attempting to pause a server with an invalid target");

        } catch (MBeanException e) {
        }
        assertNotNull(server.waitForStringInLog("CWWKE0935W", logWaitTimeout));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean resume method is issued with an invalid target.
     *
     * @throws Exception
     */
    @Test
    public void testListEndpoints() throws Exception {
        final String METHOD_NAME = "testListEndpoints";
        Log.entering(c, METHOD_NAME);

        List<String> eps = mbean.listEndpoints();
        assertTrue("Expected 0 endpoints from listEndpoints, got " + eps.size(), eps.size() == 0);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean resume method is issued with an invalid target.
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testResumeInvalidTarget() throws Exception {
        final String METHOD_NAME = "testResumeInvalidTarget";
        Log.entering(c, METHOD_NAME);

        try {
            mbean.resume("inValidTargetName!");
            fail("Didn't get expected exception from attempting to resume a server with an invalid target");

        } catch (MBeanException e) {
        }
        assertNotNull(server.waitForStringInLog("CWWKE0936W", logWaitTimeout));

        Log.exiting(c, METHOD_NAME);
    }

    private static MBeanServerConnection getMBeanServer(String serverRoot) throws Exception {
        Assert.assertFalse("server.root property is not set", (serverRoot == null || serverRoot.length() == 0));
        final File workAreaFile = new File(serverRoot, "workarea/" + CONNECTOR_ADDRESS_FILE_NAME);
        MBeanServerConnection mbsc = null;

        serverRoot = serverRoot.replaceAll("\\\\", "/");
        if (workAreaFile.exists()) {
            String connectorAddr = null;
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(workAreaFile), "UTF-8"));
            connectorAddr = br.readLine();
            if (br != null) {
                br.close();
            }
            if (connectorAddr != null) {
                System.out.println("JMX connector address:  " + connectorAddr);
                JMXServiceURL url = new JMXServiceURL(connectorAddr);
                connector = JMXConnectorFactory.connect(url);
                System.out.println("JMX Connector: " + connector);
                mbsc = connector.getMBeanServerConnection();
            } else {
                throw new Exception("JMXConnection: JMX connector address is null. The connector address file is " + workAreaFile.getAbsolutePath());
            }
        } else {
            throw new Exception("workAreaFile doesn't exist:" + workAreaFile.getAbsolutePath());
        }
        return mbsc;
    }

    private static ObjectInstance getMBeanObjectInstance(MBeanServerConnection mbsc) throws Exception {
        ObjectName obn = new ObjectName(ServerEndpointControlMBean.OBJECT_NAME);
        Set<ObjectInstance> s = mbsc.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }
}
