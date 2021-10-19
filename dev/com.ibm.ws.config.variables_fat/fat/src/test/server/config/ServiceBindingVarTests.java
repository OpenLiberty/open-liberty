/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class ServiceBindingVarTests extends ServletRunner {

    @Override
    protected String getContextRoot() {
        return "varmerge";
    }

    @Override
    protected String getServletMapping() {
        return "svcbindingvars";
    }

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.svcbinding");
    private static LibertyServer mbeanServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.svcbinding.mbean");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        //copy the config feature into the server features location
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/configfatlibertyinternals-1.0.mf");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/delayedVariable-1.0.mf");

        //copy the bundle into the server lib location
        server.copyFileToLibertyInstallRoot("lib", "bundles/test.config.variables.jar");

        server.copyFileToLibertyServerRoot("varfiles/variables");

        WebArchive varmergeApp = ShrinkHelper.buildDefaultApp("varmerge", "test.config.merged");
        ShrinkHelper.exportAppToServer(server, varmergeApp);
        ShrinkHelper.exportAppToServer(mbeanServer, varmergeApp);

        mbeanServer.setupForRestConnectorAccess();

        server.startServer("svcBindingVars.log");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server.stopServer("CWWKG0106E");

        server.deleteFileFromLibertyInstallRoot("lib/test.config.variables.jar");
        server.deleteFileFromLibertyInstallRoot("lib/features/configfatlibertyinternals-1.0.mf");
        server.deleteFileFromLibertyInstallRoot("lib/delayedVariable-1.0.mf");

    }

    @Test
    public void testNoVariables() throws Exception {
        // If other tests have cleaned up appropriately, there shouldn't be any variables
        test(server);
    }

    @Test
    public void testSimpleFileVariable() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "varfiles/simple");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            test(server);
        } finally {

            server.setMarkToEndOfLog();
            server.deleteFileFromLibertyServerRoot("variables/simple");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        }
    }

    @Test
    public void testUpdateSimpleFileVariable() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "varfiles/simple");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            this.testName = "testSimpleFileVariable";
            test(server);
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot("variables", "varfiles/updated/simple");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
            this.testName = "testUpdateSimpleFileVariable";
            test(server);
        } finally {

            server.setMarkToEndOfLog();
            server.deleteFileFromLibertyServerRoot("variables/simple");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        }
    }

    @Test
    public void testAddSimpleFileVariable() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "varfiles/simple");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            this.testName = "testSimpleFileVariable";
            test(server);
            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot("variables", "varfiles/updated/simpleTwo");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
            this.testName = "testAddSimpleFileVariable";
            test(server);
        } finally {

            server.setMarkToEndOfLog();
            server.deleteFileFromLibertyServerRoot("variables/simple");
            server.deleteFileFromLibertyServerRoot("variables/simpleTwo");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        }
    }

    @Test
    public void testSimpleVariablesWithDirectoryPrefix() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "varfiles/account_db");
        server.copyFileToLibertyServerRoot("variables/account_db", "varfiles/account_db/username");
        server.copyFileToLibertyServerRoot("variables/account_db", "varfiles/account_db/password");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDirectoryFromLibertyServerRoot("variables/account_db");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        }
    }

    @Test
    @ExpectedFFDC("java.nio.charset.MalformedInputException")
    public void testBinaryFile() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "bundles/test.config.variables.jar");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            test(server);
        } finally {
            server.waitForStringInLogUsingMark("CWWKG0106E");
            server.setMarkToEndOfLog();
            server.deleteDirectoryFromLibertyServerRoot("variables/test.config.variables.jar");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        }
    }

    // Restarts the server with a different server.env that modifies the variables directory
    @Test
    public void testAlternateBindingsDirectory() throws Exception {
        server.stopServer("CWWKG0106E");
        server.copyFileToLibertyServerRoot("varfiles/server.env");
        server.copyFileToLibertyServerRoot("altbindings", "varfiles/simple");
        server.startServer();

        try {
            test(server);
        } finally {
            server.stopServer();
            server.deleteFileFromLibertyServerRoot("server.env");
            server.deleteFileFromLibertyServerRoot("altbindings/simple");
            server.startServer(true);
        }
    }

    @Test
    public void testMultipleDirectories() throws Exception {
        server.stopServer("CWWKG0106E");

        if (server.getMachine().getOperatingSystem() == OperatingSystem.WINDOWS)
            server.copyFileToLibertyServerRoot("varfiles/multiple/win/server.env");
        else
            server.copyFileToLibertyServerRoot("varfiles/multiple/unix/server.env");

        server.copyFileToLibertyServerRoot("altbindings", "varfiles/simple");
        server.copyFileToLibertyServerRoot("altbindings2", "varfiles/account_db");
        server.copyFileToLibertyServerRoot("altbindings2/account_db", "varfiles/account_db/username");
        server.copyFileToLibertyServerRoot("altbindings2/account_db", "varfiles/account_db/password");
        server.startServer();

        try {
            test(server);
        } finally {
            server.stopServer();
            server.deleteFileFromLibertyServerRoot("server.env");
            server.deleteFileFromLibertyServerRoot("altbindings/simple");
            server.deleteDirectoryFromLibertyServerRoot("altbindings2");
            server.startServer(true);
        }
    }

    // Tests override behavior wrt server.xml
    @Test
    public void testServerXMLOverrides() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "varfiles/conflicts");
        server.copyFileToLibertyServerRoot("variables/conflicts", "varfiles/conflicts/var1");
        server.copyFileToLibertyServerRoot("variables/conflicts", "varfiles/conflicts/var2");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteDirectoryFromLibertyServerRoot("variables/conflicts");
            // there are no functional changes, but we need to wait because other tests directly reference variables in the
            // registry rather than using them through config
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        }
    }

    // Test an empty file
    @Test
    public void testEmptyFile() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "varfiles/empty");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            test(server);
        } finally {
            server.setMarkToEndOfLog();
            server.deleteFileFromLibertyServerRoot("variables/empty");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        }
    }

    @Test
    public void testPropertiesFile() throws Exception {
        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot("variables", "varfiles/vars.properties");
        server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

        try {
            test(server);

            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot("variables", "varfiles/updated/vars.properties");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
            testName = "testPropertiesFileAfterUpdated";
            test(server);

            server.setMarkToEndOfLog();
            server.deleteFileFromLibertyServerRoot("variables/vars.properties");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
            testName = "testPropertiesFileAfterRemove";
            test(server);

            server.setMarkToEndOfLog();
            server.copyFileToLibertyServerRoot("variables", "varfiles/updated/vars.properties");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
            testName = "testPropertiesFileAfterUpdated";
            test(server);

        } finally {
            server.setMarkToEndOfLog();
            server.deleteFileFromLibertyServerRoot("variables/vars.properties");
            server.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());
        }
    }

    private final static String[] MBEAN_METHOD_SIGNATURE = new String[] { Collection.class.getName(),
                                                                          Collection.class.getName(),
                                                                          Collection.class.getName() };

    @Test
    public void testMbeanUpdate() throws Exception {

        try {
            server.stopServer("CWWKG0106E");
            mbeanServer.startServer("svcBindingVarsMBean.log");
            JMXConnector connector = mbeanServer.getJMXRestConnector();
            MBeanServerConnection mbeanConn = connector.getMBeanServerConnection();
            final ObjectName fileMonitorMBeanName = new ObjectName("WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean");

            test(mbeanServer);

            mbeanServer.copyFileToLibertyServerRoot("variables", "varfiles/simple");
            mbeanServer.copyFileToLibertyServerRoot("variables", "varfiles/account_db");
            mbeanServer.copyFileToLibertyServerRoot("variables/account_db", "varfiles/account_db/username");
            mbeanServer.copyFileToLibertyServerRoot("variables/account_db", "varfiles/account_db/password");
            // Copy "added" but don't notify
            mbeanServer.copyFileToLibertyServerRoot("variables", "varfiles/added");

            List<String> updates = Arrays.asList("variables/simple", "variables/account_db/username", "variables/account_db/password");
            Object[] params = new Object[] { null, updates, null };
            mbeanConn.invoke(fileMonitorMBeanName, "notifyFileChanges", params, MBEAN_METHOD_SIGNATURE);

            testName = "testMBeanUpdateAfterModify";
            test(mbeanServer);

            // Now notify about "added"
            params = new Object[] { Collections.singletonList("variables/added"), null, null };
            mbeanConn.invoke(fileMonitorMBeanName, "notifyFileChanges", params, MBEAN_METHOD_SIGNATURE);

            testName = "testMBeanUpdateAfterAdd";
            test(mbeanServer);

            mbeanServer.deleteFileFromLibertyServerRoot("variables/simple");
            mbeanServer.deleteFileFromLibertyServerRoot("variables/account_db/password");
            List<String> deletes = Arrays.asList("variables/simple", "variables/account_db/password");
            params = new Object[] { null, null, deletes };
            mbeanConn.invoke(fileMonitorMBeanName, "notifyFileChanges", params, MBEAN_METHOD_SIGNATURE);

            testName = "testMBeanUpdateAfterDelete";
            test(mbeanServer);

            // Disable monitoring
            mbeanServer.setMarkToEndOfLog();
            ServerConfiguration serverConfig = mbeanServer.getServerConfiguration();
            serverConfig.getConfig().setUpdateTrigger("disabled");
            mbeanServer.updateServerConfiguration(serverConfig);
            params = new Object[] { null, Collections.singletonList("server.xml"), null };
            mbeanConn.invoke(fileMonitorMBeanName, "notifyFileChanges", params, MBEAN_METHOD_SIGNATURE);
            mbeanServer.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

            // Copy in a file that shouldn't be noticed. MBean update should fail
            mbeanServer.copyFileToLibertyServerRoot("variables", "varfiles/simple");
            params = new Object[] { Collections.singletonList("variables/simple"), null, null };
            mbeanConn.invoke(fileMonitorMBeanName, "notifyFileChanges", params, MBEAN_METHOD_SIGNATURE);

            testName = "testMBeanUpdateAfterDisabled";
            test(mbeanServer);

            // Enable polling

            serverConfig = mbeanServer.getServerConfiguration();
            serverConfig.getConfig().setUpdateTrigger("polled");
            mbeanServer.updateServerConfiguration(serverConfig);
            // Need to restart the server because the config updateTrigger is disabled
            mbeanServer.restartServer();

            testName = "testMBeanUpdateAfterPolling";
            test(mbeanServer);

            // Enable mbean again, delete a file and update
            mbeanServer.deleteFileFromLibertyServerRoot("variables/simple");

            mbeanServer.setMarkToEndOfLog();
            serverConfig = mbeanServer.getServerConfiguration();
            serverConfig.getConfig().setUpdateTrigger("mbean");
            mbeanServer.updateServerConfiguration(serverConfig);
            mbeanServer.waitForConfigUpdateInLogUsingMark(Collections.<String> emptySet());

            params = new Object[] { null, null, Collections.singletonList("variables/simple") };
            mbeanConn.invoke(fileMonitorMBeanName, "notifyFileChanges", params, MBEAN_METHOD_SIGNATURE);

            testName = "testMBeanUpdateAfterReenabling";
            test(mbeanServer);

        } finally {
            // Done with this server -- will need to change this to delete files if tests are added in the future
            mbeanServer.stopServer();
            server.startServer();
        }
    }
}
