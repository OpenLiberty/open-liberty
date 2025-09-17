/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.management.j2ee.fat.tests;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class EJBJ2EEManagementTest {
    private static final String SERVER_NAME = "com.ibm.ws.ejbcontainer.management.j2ee.fat";
    private static final String APP_NAME = "EJBJ2EEManagement";
    private static final String MODULE_NAME = "EJBJ2EEManagement.jar";
    // SLSB, SFSB, SGSB, MDB
    private static final int NUM_BEANS = 4;

    @Server("com.ibm.ws.ejbcontainer.management.j2ee.fat")
    public static LibertyServer server;

    public static RepeatTests r = RepeatTests //
                    .with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.management.j2ee.fat")) //
                    .andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.management.j2ee.fat"));

    public static CheckpointRule checkpointRule = new CheckpointRule() //
                    .setConsoleLogName(EJBJ2EEManagementTest.class.getSimpleName() + ".log") //
                    .addUnsupportedRepeatIDs(EE7FeatureReplacementAction.ID, EmptyAction.ID) //
                    .setServerSetup(EJBJ2EEManagementTest::serverSetUp)//
                    .setServerStart(EJBJ2EEManagementTest::serverStart)//
                    .setServerTearDown(EJBJ2EEManagementTest::serverTearDown)//
                    .setAssertNoAppRestartOnRestore(false);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(r).around(checkpointRule);

    private static JMXConnector jmxConnector;
    private static MBeanServerConnection mbsc;

    public static LibertyServer serverSetUp(ServerMode mode) throws Exception {
        // Use ShrinkHelper to build the EJBJ2EEManagement ear
        JavaArchive ExcEJBJ2EEManagementJar = ShrinkHelper.buildJavaArchive(MODULE_NAME, "com.ibm.ws.ejbcontainer.management.j2ee.fat.ejb.");
        ExcEJBJ2EEManagementJar = (JavaArchive) ShrinkHelper.addDirectory(ExcEJBJ2EEManagementJar, "test-applications/EJBJ2EEManagement.jar/resources");

        EnterpriseArchive EJBJ2EEManagementApp = ShrinkWrap.create(EnterpriseArchive.class, "EJBJ2EEManagement.ear");
        EJBJ2EEManagementApp.addAsModule(ExcEJBJ2EEManagementJar);

        ShrinkHelper.exportAppToServer(server, EJBJ2EEManagementApp, DeployOptions.SERVER_ONLY);

        server.setupForRestConnectorAccess();

        return server;
    }

    public static void serverStart(ServerMode mode, LibertyServer server) throws Exception {
        server.startServer();
        jmxConnector = server.getJMXRestConnector();
        mbsc = jmxConnector.getMBeanServerConnection();
    }

    public static void serverTearDown(ServerMode mode, LibertyServer server) throws Exception {
        if (server != null && server.isStarted()) {
            if (jmxConnector != null) {
                jmxConnector.close();
            }
            server.stopServer();
        }
    }

    private void assertValidObjectName(String onString, String expectedType) throws Exception {
        ObjectName onQuery = new ObjectName(onString);
        if (expectedType != null) {
            Assert.assertEquals(expectedType, onQuery.getKeyProperty("j2eeType"));
        }

        Set<ObjectName> ons = mbsc.queryNames(onQuery, null);
        Assert.assertEquals(ons.toString(), 1, ons.size());
    }

    private void testJ2EEManagedObject(ObjectName on) throws Exception {
        Assert.assertEquals(on, new ObjectName((String) mbsc.getAttribute(on, "objectName")));
        Assert.assertEquals(false, mbsc.getAttribute(on, "stateManageable"));
        Assert.assertEquals(false, mbsc.getAttribute(on, "statisticsProvider"));
        Assert.assertEquals(false, mbsc.getAttribute(on, "eventProvider"));
    }

    @Test
    public void testEJBModule() throws Exception {
        // Specify all the required attributes.
        ObjectName onQuery = new ObjectName("WebSphere:" +
                                            "j2eeType=EJBModule," +
                                            "name=" + MODULE_NAME + "," +
                                            "J2EEApplication=" + APP_NAME + "," +
                                            "J2EEServer=" + SERVER_NAME + "," +
                                            "*");
        Set<ObjectName> ons = mbsc.queryNames(onQuery, null);
        Assert.assertEquals(ons.toString(), 1, ons.size());
        ObjectName on = ons.iterator().next();

        testJ2EEManagedObject(on);

        String ddString = (String) mbsc.getAttribute(on, "deploymentDescriptor");

        Assert.assertNotNull("descriptor not expected to be null", ddString);

        Assert.assertTrue("descriptor did not contain expected ejb-jar tag", ddString.contains("ejb-jar"));

        assertValidObjectName((String) mbsc.getAttribute(on, "server"), "J2EEServer");

        String[] javaVMONStrings = (String[]) mbsc.getAttribute(on, "javaVMs");
        Assert.assertEquals(Arrays.toString(javaVMONStrings), 1, javaVMONStrings.length);
        assertValidObjectName(javaVMONStrings[0], "JVM");

        String[] ejbONStrings = (String[]) mbsc.getAttribute(on, "ejbs");
        Assert.assertEquals(Arrays.toString(ejbONStrings), NUM_BEANS, ejbONStrings.length);
        Assert.assertEquals(Arrays.toString(ejbONStrings), NUM_BEANS, new HashSet<String>(Arrays.asList(ejbONStrings)).size());
        for (String ejbONString : ejbONStrings) {
            assertValidObjectName(ejbONString, null);
        }
    }

    private void testBean(String type, String name) throws Exception {
        ObjectName onQuery = new ObjectName("WebSphere:" +
                                            "j2eeType=" + type + "," +
                                            "name=" + name + "," +
                                            "EJBModule=" + MODULE_NAME + "," +
                                            "J2EEApplication=" + APP_NAME + "," +
                                            "J2EEServer=" + SERVER_NAME + "," +
                                            "*");
        Set<ObjectName> ons = mbsc.queryNames(onQuery, null);
        Assert.assertEquals(ons.toString(), 1, ons.size());
        ObjectName on = ons.iterator().next();

        testJ2EEManagedObject(on);
    }

    @Test
    public void testStatelessSessionBean() throws Exception {
        testBean("StatelessSessionBean", "EJBJ2EEManagementSLSB");
    }

    @Test
    public void testStatefulSessionBean() throws Exception {
        testBean("StatefulSessionBean", "EJBJ2EEManagementSFSB");
    }

    @Test
    public void testSingletonSessionBean() throws Exception {
        testBean("SingletonSessionBean", "EJBJ2EEManagementSGSB");
    }

    @Test
    public void testMessageDrivenBean() throws Exception {
        testBean("MessageDrivenBean", "EJBJ2EEManagementMDB");
    }

    @Test
    public void testStopped() throws Exception {
        // Ensure there are MBeans for the application.
        ObjectName onQuery = new ObjectName("WebSphere:J2EEApplication=" + APP_NAME + ",*");
        Set<ObjectName> ons = mbsc.queryNames(onQuery, null);
        Assert.assertFalse(ons.toString(), ons.isEmpty());

        // Stop the application.
        ServerConfiguration oldConfig = server.getServerConfiguration();
        ServerConfiguration newConfig = oldConfig.clone();
        newConfig.getApplications().clear();
        server.updateServerConfiguration(newConfig);
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet(), true, "CWWKZ0009I");

        try {
            // Ensure there are no MBeans for the application.
            ons = mbsc.queryNames(onQuery, null);
            Assert.assertTrue(ons.toString(), ons.isEmpty());
        } finally {
            // Restart the application.
            server.updateServerConfiguration(oldConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
        }
    }
}
