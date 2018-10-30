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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.kernel.server.ServerEndpointControlMBean;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * This test exercises the basic functionality of the ServerEndpointControlMBean by
 * performing tests similar to those of the PauseResumeCommandTest except through the
 * mbean interface instead of the server command. It includes tests that make updates to the server
 * configuration while the server is running.
 * A setUpPerTest method runs before each test to restore to the original configuration,
 * so that tests do not interfere with each other.
 *
 */
@RunWith(FATRunner.class)
public class ServerEndpointControlMBeanTest {

    private static final String CONNECTOR_ADDRESS_FILE_NAME = "com.ibm.ws.jmx.local.address";
    private static final String PAUSEABLE_EXCEPTION_CLASS = "com.ibm.ws.kernel.launch.service.PauseableComponentControllerRequestFailedException";
    private static final Class<?> c = ServerEndpointControlMBeanTest.class;
    private static final String PORT_IN_USE = "CWWKO0221E";

    private static JMXConnector connector = null;

    @Rule
    public TestName testName = new TestName();

    @Server("com.ibm.ws.kernel.service")
    public static LibertyServer server;

    private static ServerEndpointControlMBean mbean = null;
    // a copy of the unmodified server configuration
    private static ServerConfiguration savedConfig;
    // Tests can use this to indicate they don't make any config updates and so don't need to have the original config restored
    // false at first because the testcase starts with the baseline config
    private static boolean restoreSavedConfig = false;

    @BeforeClass
    public static void setup() throws Exception {
        final String METHOD_NAME = "setup";
        Log.entering(c, METHOD_NAME);
        server.startServer();
        // set up local connector
        MBeanServerConnection mbsc = getMBeanServer(server.getServerRoot());
        // get ON of the desired mbean
        ObjectName on = getMBeanObjectInstance(mbsc).getObjectName();
        // create an proxy for the mbean
        mbean = JMX.newMBeanProxy(mbsc, on, ServerEndpointControlMBean.class);
        // clone the config so it can be restored after each test that dynamically changes config
        savedConfig = server.getServerConfiguration().clone();
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * After completing all tests, close the local connector and stop the server.
     */
    @AfterClass
    public static void teardown() throws Exception {
        final String METHOD_NAME = "teardown";
        Log.entering(c, METHOD_NAME);
        if (connector != null) {
            connector.close();
        }
        if (server != null && server.isStarted()) {
            // Ignore port conflict errors -- we don't really care for our purposes here.
            server.stopServer("CWWKE093*", PORT_IN_USE);
            server.updateServerConfiguration(savedConfig);
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Before running each test, check to see whether to restore the original server configuration
     */
    @Before
    public void setUpPerTest() throws Exception {
        final String METHOD_NAME = "setUpPerTest";
        Log.entering(c, METHOD_NAME);

        // restore cloned config
        if (restoreSavedConfig) {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(savedConfig);
            assertNotNull("Didn't get expected config update log messages", server.waitForConfigUpdateInLogUsingMark(null));

        }
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
        restoreSavedConfig = false;

        try {
            server.setMarkToEndOfLog();
            mbean.pause("inValidTargetName!");
            fail("Didn't get expected exception from attempting to pause an endpoint with an invalid target 'inValidTargetName!'");

        } catch (MBeanException e) {
        }
        assertNotNull("Expected CWWKE0935W log message wasn't found", server.waitForStringInLog("CWWKE0935W"));

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
        restoreSavedConfig = false;

        try {
            server.setMarkToEndOfLog();
            mbean.resume("inValidTargetName!");
            fail("Didn't get expected exception from attempting to resume an endpoint with an invalid target 'inValidTargetName!'");

        } catch (MBeanException e) {
        }
        assertNotNull("Expected CWWKE0936W log message wasn't found", server.waitForStringInLog("CWWKE0936W"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that if the MBean pause method is issued with an valid target that the endpoint is paused
     *
     * @throws Exception
     */
    @Test
    public void testPauseValidTarget() throws Exception {
        final String METHOD_NAME = "testPauseValidTarget";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = false;

        try {
            server.setMarkToEndOfLog();
            mbean.pause("defaultHttpEndpoint");
        } catch (MBeanException e) {
            fail("Got unexpected exception from attempting to pause a endpoint with a valid target 'defaultHttpEndpoint':" + e.getCause());
        }
        assertNotNull("Expected CWWKE0938I log message wasn't found", server.waitForStringInLog("CWWKE0938I"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the MBean pause method will pause specified endpoints
     *
     * @throws Exception
     */
    @Test
    public void testPauseValidTargets() throws Exception {
        final String METHOD_NAME = "testPauseValidTargets";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = true;

        String targets = createHttpEndpoints(8);
        Log.info(c, METHOD_NAME, "targets:" + targets);
        try {
            server.setMarkToEndOfLog();
            mbean.pause(targets);
        } catch (MBeanException e) {
            fail("Got unexpected exception from attempting to pause a endpoint with valid targets:'" + targets + "'" + e.getCause());
        }
        // check request completed
        int numMessages = server.waitForMultipleStringsInLog(8, "CWWKO0220I: TCP Channel httpEndpoint[0-7]*");
        assertEquals("Expected 8 CWWKO0220I log messages, got " + numMessages, 8, numMessages);
        assertNotNull("Expected CWWKE0938I log message wasn't found", server.waitForStringInLog("CWWKE0938I"));
        // check the specified targets are paused
        assertTrue("isPaused unexpectedly returned false", mbean.isPaused(targets));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests the MBean pause method will pause all valid specified endpoints and warn about any invalid targets
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testPauseInvalidTargets() throws Exception {
        final String METHOD_NAME = "testPauseInvalidTargets";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = true;

        String targets = createHttpEndpoints(3);
        targets += "1bad_target, bad_target2";
        Log.info(c, METHOD_NAME, "targets:" + targets);
        try {
            server.setMarkToEndOfLog();
            mbean.pause(targets);
            fail("Failed to get expected exception from attempting to pause a mix of invalid and valid targets:'" + targets + "'");
        } catch (MBeanException e) {
        }
        // check request completed
        int numMessages = server.waitForMultipleStringsInLog(3, "CWWKO0220I: TCP Channel httpEndpoint[0-2]*");
        assertEquals("Expected 3 CWWKO0220I log messages, got " + numMessages, 3, numMessages);
        assertNotNull("Expected CWWKE0935W log message wasn't found", server.waitForStringInLog("CWWKE0935W"));
        assertNotNull("Expected CWWKE0938I log message wasn't found", server.waitForStringInLog("CWWKE0938I"));
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is emitted if the MBean pause all method is issued when no pauseable components exist
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testPauseAllNoPauseableComponents() throws Exception {
        final String METHOD_NAME = "testPauseAllNoPauseableComponents";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = true;

        // remove the servlet feature so that all endpoints are deactivated including the default instance of httpEndpoint
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("localConnector-1.0"));
        assertNotNull("Didn't get expected config update log messages", server.waitForConfigUpdateInLogUsingMark(null, true));
        try {
            server.setMarkToEndOfLog();
            mbean.pause();
            fail("Failed to get expected exception from attempting to pause all endpoints with no pauseable components");
        } catch (MBeanException e) {
            // expected
        }
        assertNotNull("Didn't find CWWKE0933W in logs as expected.", server.waitForStringInLog("CWWKE0933W"));
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean pause method is issued with invalid target list
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testPauseNullTarget() throws Exception {
        final String METHOD_NAME = "testPauseNullTarget";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = false;

        try {
            server.setMarkToEndOfLog();
            mbean.pause(null);
            fail("Didn't get expected exception from attempting to pause a server with an invalid target list");

        } catch (MBeanException e) {
        }
        assertNotNull("Didn't find CWWKE0931W in logs as expected.", server.waitForStringInLog("CWWKE0931W"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean resume all method is issued when no pauseable components exist
     *
     * @throws Exception
     */

    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testResumeAllNoPauseableComponents() throws Exception {
        final String METHOD_NAME = "testResumeAllNoPauseableComponents";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = true;

        // remove the servlet feature so that all endpoints are deactivated including the default instance of httpEndpoint
        server.setMarkToEndOfLog();
        server.changeFeatures(Arrays.asList("localConnector-1.0"));
        assertNotNull("Didn't get expected config update log messages", server.waitForConfigUpdateInLogUsingMark(null, true));
        try {
            server.setMarkToEndOfLog();
            mbean.resume();
            fail("Failed to get expected exception from attempting to resume all endpoints with no pauseable components");
        } catch (MBeanException e) {
            // expected
        }
        assertNotNull("Didn't find CWWKE0934W in logs as expected.", server.waitForStringInLog("CWWKE0934W"));
        Log.exiting(c, METHOD_NAME);
    }

    //Test that an exception is thrown when isPaused(String targets) is supplied with an empty string
    //or null for the target
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testIsPausedInvalidArgs() throws Exception {
        final String METHOD_NAME = "testIsPausedInvalidArgs";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = false;

        try {
            mbean.isPaused("");
            fail("Failed to get expected exception from calling isPaused(\"\")");
        } catch (MBeanException e) {
            // expected
        }

        try {
            mbean.isPaused(null);
            fail("Failed to get expected exception from calling isPaused(null)");
        } catch (MBeanException e) {
            // expected
        }

        assertNotNull("Didn't find CWWKE0946W in logs as expected.", server.waitForStringInLog("CWWKE0946W"));
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected exception is thrown if the MBean resume method is issued with empty target list
     *
     * @throws Exception
     */
    @ExpectedFFDC(PAUSEABLE_EXCEPTION_CLASS)
    @Test
    public void testResumeEmptyTarget() throws Exception {
        final String METHOD_NAME = "testResumeEmptyTarget";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = false;

        try {
            server.setMarkToEndOfLog();
            mbean.resume("");
            fail("Didn't get expected exception from attempting to resume a server with an invalid target list");

        } catch (MBeanException e) {
        }
        assertNotNull("Didn't find CWWKE0932W in logs as expected.", server.waitForStringInLog("CWWKE0932W"));

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Tests that the expected number of endpoints is received from the MBean listEndpoints method.
     *
     * @throws Exception
     */
    @Test
    public void testListEndpoints() throws Exception {
        final String METHOD_NAME = "testListEndpoints";
        Log.entering(c, METHOD_NAME);
        restoreSavedConfig = true;

        int numInstances = 14;
        // the default instance needs to be accounted for
        createHttpEndpoints(numInstances - 1);
        server.setMarkToEndOfLog();
        List<String> eps = mbean.listEndpoints();
        assertTrue("Expected " + numInstances + " endpoints from " + METHOD_NAME + ", got " + eps.size(), eps.size() == numInstances);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Create the specified number of httpEndpoint config elements
     *
     * @param numberOfEndpoints
     * @return a list of targets that can be used with pause and resume operations
     * @throws Exception
     */
    public String createHttpEndpoints(int numberOfEndpoints) throws Exception {
        final String METHOD_NAME = "createHttpEndpoints";
        Log.entering(c, METHOD_NAME);

        final String httpEndpointBaseName = "httpEndpoint";
        String targets = "";
        int httpPortBase = 9012;
        int httpsPortBase = 9052;
        ServerConfiguration config = server.getServerConfiguration();
        for (int i = 0; i < numberOfEndpoints; i++) {
            // Add specified number of <httpEndpoint httpPort="9012" httpsPort="9415" id="httpEndpointN"/> elements to server config
            HttpEndpoint httpEndpoint = new HttpEndpoint();
            httpEndpoint.setId(httpEndpointBaseName + i);
            httpEndpoint.setHttpPort(Integer.toString(httpPortBase + i));
            httpEndpoint.setHttpsPort(Integer.toString(httpsPortBase + i));
            config.getHttpEndpoints().add(httpEndpoint);
            targets += httpEndpointBaseName + i + ",";
        }
        // save
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        assertNotNull("Didn't get expected config update log messages", server.waitForConfigUpdateInLogUsingMark(null));

        Log.exiting(c, METHOD_NAME);
        return targets;
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
                Log.info(c, "getMBeanServer", "JMX connector address:  " + connectorAddr);
                JMXServiceURL url = new JMXServiceURL(connectorAddr);
                connector = JMXConnectorFactory.connect(url);
                Log.info(c, "getMBeanServer", "JMX Connector: " + connector);
                mbsc = connector.getMBeanServerConnection();
            } else {
                throw new Exception("JMX connector address is null. The connector address file is " + workAreaFile.getAbsolutePath());
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
            assertEquals("Incorrect number of MBeans found, expected 1, found " + s.size(), 1);
        }
        return s.iterator().next();
    }
}
