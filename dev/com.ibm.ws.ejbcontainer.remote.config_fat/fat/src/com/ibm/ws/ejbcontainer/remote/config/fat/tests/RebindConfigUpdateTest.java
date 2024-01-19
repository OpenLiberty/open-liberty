/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.config.fat.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.ORB;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that EJB remote bindings in JNDI are properly updated when the EJB Remote feature
 * and ORB component are dynamically updated.
 */
@RunWith(FATRunner.class)
public class RebindConfigUpdateTest extends FATServletClient {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                System.runFinalization();
                System.gc();
                server.serverDump("heap");
            } catch (Exception e1) {
                System.out.println("Failed to dump server");
                e1.printStackTrace();
            }
        }
    };

    private static final Class<?> c = RebindConfigUpdateTest.class;
    private static HashSet<String> apps = new HashSet<String>();
    private static String servlet = "ConfigTestsWeb/RebindConfigUpdateServlet";

    private static int ALL_LOCAL_BINDINGS = 15;
    private static int ALL_REMOTE_BINDINGS = 12;

    @Server("com.ibm.ws.ejbcontainer.remote.config.fat.server")
    public static LibertyServer server;

    @Server("com.ibm.ws.ejbcontainer.remote.config.fat.server.ssl")
    public static LibertyServer server_ssl;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.config.fat.server",
                                                                                                                    "com.ibm.ws.ejbcontainer.remote.config.fat.server.ssl")) //
                    .andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.config.fat.server",
                                                                                "com.ibm.ws.ejbcontainer.remote.config.fat.server.ssl")) //
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17).forServers("com.ibm.ws.ejbcontainer.remote.config.fat.server",
                                                                                                                                                               "com.ibm.ws.ejbcontainer.remote.config.fat.server.ssl")) //
                    .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.config.fat.server",
                                                                                               "com.ibm.ws.ejbcontainer.remote.config.fat.server.ssl")) //
                    .andWith(FeatureReplacementAction.EE11_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.config.fat.server",
                                                                                 "com.ibm.ws.ejbcontainer.remote.config.fat.server.ssl"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();
        server_ssl.deleteAllDropinApplications();
        server_ssl.removeAllInstalledAppsForValidation();

        apps.add("ConfigTestsTestApp");

        // Use ShrinkHelper to build the ears

        // -------------- ConfigTestsTestApp ------------
        JavaArchive ConfigTestsEJB = ShrinkHelper.buildJavaArchive("ConfigTestsEJB.jar", "com.ibm.ws.ejbcontainer.remote.configtests.ejb.");

        WebArchive ConfigTestsWeb = ShrinkHelper.buildDefaultApp("ConfigTestsWeb.war", "com.ibm.ws.ejbcontainer.remote.configtests.web.",
                                                                 "com.ibm.ws.ejbcontainer.remote.configtests.web.ejb.");

        EnterpriseArchive ConfigTestsTestApp = ShrinkWrap.create(EnterpriseArchive.class, "ConfigTestsTestApp.ear");
        ConfigTestsTestApp.addAsModules(ConfigTestsEJB, ConfigTestsWeb);
        ShrinkHelper.addDirectory(ConfigTestsTestApp, "test-applications/ConfigTestsTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, ConfigTestsTestApp, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server_ssl, ConfigTestsTestApp, DeployOptions.SERVER_ONLY);

        // servers will be started in each test
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
        if (server_ssl != null && server_ssl.isStarted()) {
            server_ssl.stopServer();
        }
    }

    @Before
    public void testSetUp() throws Exception {
        // save the original server configuration
        server.saveServerConfiguration();
        server_ssl.saveServerConfiguration();
    }

    @After
    public void testCleanUp() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
            if (server_ssl != null && server_ssl.isStarted()) {
                server_ssl.stopServer();
            }
        } finally {
            // Restore the original server configuration
            server.restoreServerConfiguration();
            server_ssl.restoreServerConfiguration();
        }
    }

    private String removeRemoteFeature() throws Exception {
        String m = "removeRemoteFeature";

        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        for (String feature : features) {
            if (feature.startsWith("ejbRemote-") || feature.startsWith("enterpriseBeansRemote-")) {
                Log.info(c, m, "removing feature " + feature + " from server config");
                features.remove(feature);
                if (server.isStarted()) {
                    server.setMarkToEndOfLog();
                }
                server.updateServerConfiguration(config);
                if (server.isStarted()) {
                    server.waitForConfigUpdateInLogUsingMark(apps);
                }
                return feature;
            }
        }
        Log.info(c, m, "remote feature not found");
        return null;
    }

    private void addRemoteFeature(String remoteFeatureName) throws Exception {
        String m = "addRemoteFeature";

        if (remoteFeatureName != null) {
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            if (!features.contains(remoteFeatureName)) {
                Log.info(c, m, "adding feature " + remoteFeatureName + " to server config");
                features.add(remoteFeatureName);
                server.setMarkToEndOfLog();
                server.updateServerConfiguration(config);
                server.waitForConfigUpdateInLogUsingMark(apps);
            } else {
                Log.info(c, m, "feature already in server configuration: " + remoteFeatureName);

            }
        }
    }

    private void updateConfigElement(Boolean bindToJavaGlobal) throws Exception {
        String m = "updateConfigElement";

        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbElement = config.getEJBContainer();
        if (ejbElement.getBindToJavaGlobal() != bindToJavaGlobal) {
            Log.info(c, m, "adding bindToJavaGlobal = " + bindToJavaGlobal + " to server config");

            ejbElement.setBindToJavaGlobal(bindToJavaGlobal);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(apps);
        } else {
            Log.info(c, m, "config element already set to desired value");
        }
    }

    private void updateOrbConfig() throws Exception {
        String m = "updateOrbConfig";

        ServerConfiguration config = server.getServerConfiguration();
        ORB orb = config.getOrb();
        if (!"30".equals(orb.getOrbSSLInitTimeout())) {
            Log.info(c, m, "updating orb configuration; setting sslInitTimeout=30");
            if (orb.getId() == null) {
                orb.setId("defaultOrb");
            }
            orb.setOrbSSLInitTimeout("30");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null);
        } else {
            Log.info(c, m, "config element already set to desired value");
        }
    }

    private List<Element> removeQuickStartConfig() throws Exception {
        String m = "removeQuickStartConfig";

        ServerConfiguration config = server_ssl.getServerConfiguration();
        List<Element> removed = config.removeUnknownElement("quickStartSecurity");
        Log.info(c, m, "removing quickStartSecurity : " + removed);
        if (server_ssl.isStarted()) {
            server_ssl.setMarkToEndOfLog();
        }
        server_ssl.updateServerConfiguration(config);
        if (server_ssl.isStarted()) {
            server_ssl.waitForConfigUpdateInLogUsingMark(apps);
        }
        return removed;
    }

    private void restoreQuickStartConfig(List<Element> removed) throws Exception {
        String m = "restoreQuickStartConfig";

        ServerConfiguration config = server_ssl.getServerConfiguration();
        config.addUnknownElements(removed);
        Log.info(c, m, "restoring quickStartSecurity : " + removed);
        server_ssl.setMarkToEndOfLog();
        server_ssl.updateServerConfiguration(config);
        server_ssl.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * Test that the Enterprise Beans remote feature (ejbRemote or enterpriseBeansRemote) works properly
     * after being dynamically added to an active server. The remote bean interfaces
     * should become available after the remote feature has been added.
     *
     * Note: For this scenario, the application should be restarted after each configuration change.
     */
    @Test
    @ExpectedFFDC("com.ibm.wsspi.injectionengine.InjectionException")
    public void testBindWhenEjbRemoteAdded() throws Exception {
        String remoteFeatureName = removeRemoteFeature();
        server.startServer();
        verifyBindings(server, ALL_LOCAL_BINDINGS, 0);
        FATServletClient.runTest(server, servlet, "testBindWhenEjbRemoteAdded_Initial");

        addRemoteFeature(remoteFeatureName);
        verifyBindings(server, ALL_LOCAL_BINDINGS, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server, servlet, "testBindWhenEjbRemoteAdded_Added");
    }

    /**
     * Test that the Enterprise Beans remote feature (ejbRemote or enterpriseBeansRemote) works properly
     * after being dynamically removed and then added to an active server. The remote bean interfaces
     * should no longer be available in JNDI after the feature has been removed, and then become
     * available again after the remote feature has been added.
     *
     * Note: For this scenario, the application should be restarted after each configuration change.
     */
    // @Test - issue with yoko ORB
    @ExpectedFFDC("com.ibm.wsspi.injectionengine.InjectionException")
    public void testRebindWhenEjbRemoteRemovedAdded() throws Exception {
        server.startServer();
        verifyBindings(server, ALL_LOCAL_BINDINGS, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server, servlet, "testRebindWhenEjbRemoteRemovedAdded_Initial");

        String remoteFeatureName = removeRemoteFeature();
        verifyBindings(server, ALL_LOCAL_BINDINGS, 0);
        FATServletClient.runTest(server, servlet, "testRebindWhenEjbRemoteRemovedAdded_Removed");

        addRemoteFeature(remoteFeatureName);
        verifyBindings(server, ALL_LOCAL_BINDINGS, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server, servlet, "testRebindWhenEjbRemoteRemovedAdded_Added");
    }

    /**
     * Test that the Enterprise Beans remote feature (ejbRemote or enterpriseBeansRemote) works properly
     * after the remote feature configuration is dynamically updated in an active server. The remote bean
     * interfaces should be available in JNDI and work properly after the server configuration update.
     *
     * Note: For this scenario, the application should not be restarted after the configuration update.
     */
    // @Test
    public void testRebindAfterEjbContainerUpdated() throws Exception {
        server.startServer();
        verifyBindings(server, ALL_LOCAL_BINDINGS, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server, servlet, "testRebindAfterEjbContainerUpdated_Initial");

        // Note: setting to false will not change behavior since app is not restarted
        updateConfigElement(false);
        verifyBindings(server, 0, 0);
        FATServletClient.runTest(server, servlet, "testRebindAfterEjbContainerUpdated_Updated");
    }

    /**
     * Test that the Enterprise Beans remote feature (ejbRemote or enterpriseBeansRemote) works properly
     * after the ORB feature configuration is dynamically updated in an active server. The remote bean
     * interfaces should be available in JNDI and work properly after the server configuration update.
     *
     * Note: For this scenario, the application should not be restarted after the configuration update.
     */
    // @Test
    public void testRebindAfterOrbUpdated() throws Exception {
        server.startServer();
        verifyBindings(server, ALL_LOCAL_BINDINGS, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server, servlet, "testRebindAfterOrbUpdated_Initial");

        updateOrbConfig();
        waitForBindings(server, 0, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server, servlet, "testRebindAfterOrbUpdated_Updated");
    }

    /**
     * Test that the Enterprise Beans remote feature (ejbRemote or enterpriseBeansRemote) works properly
     * after the ORB feature fails to start until the configuration is dynamically updated in an active
     * server. The remote bean interfaces should be available in JNDI and work properly after the server
     * configuration update.
     *
     * Note: For this scenario, the application should not be restarted after the configuration update.
     */
    // @Test
    @ExpectedFFDC("com.ibm.wsspi.injectionengine.InjectionException")
    public void testRebindAfterOrbLateStart() throws Exception {
        List<Element> removed = removeQuickStartConfig();
        server_ssl.startServer();
        verifyBindings(server_ssl, ALL_LOCAL_BINDINGS, 0);
        FATServletClient.runTest(server_ssl, servlet, "testRebindAfterOrbLateStart_Initial");

        restoreQuickStartConfig(removed);
        waitForBindings(server_ssl, 0, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server_ssl, servlet, "testRebindAfterOrbLateStart_Started");
    }

    /**
     * Test that the Enterprise Beans remote feature (ejbRemote or enterpriseBeansRemote) works properly
     * after the ORB feature is dynamically stopped and then started in an active server. The remote bean
     * interfaces should be available in JNDI and work properly after the server configuration update.
     *
     * Note: For this scenario, the application should not be restarted after the configuration update.
     */
    // @Test
    @ExpectedFFDC("com.ibm.wsspi.injectionengine.InjectionException")
    public void testRebindAfterOrbStoppedStarted() throws Exception {
        server_ssl.startServer();
        verifyBindings(server_ssl, ALL_LOCAL_BINDINGS, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server_ssl, servlet, "testRebindAfterOrbStoppedStarted_Initial");

        List<Element> removed = removeQuickStartConfig();
        verifyBindings(server_ssl, 0, 0);
        FATServletClient.runTest(server_ssl, servlet, "testRebindAfterOrbStoppedStarted_Stopped");

        restoreQuickStartConfig(removed);
        waitForBindings(server_ssl, 0, ALL_REMOTE_BINDINGS);
        FATServletClient.runTest(server_ssl, servlet, "testRebindAfterOrbStoppedStarted_Started");
    }

    /**
     * Finds the expected enterprise bean binding messages in the log. <p>
     *
     * This method should be used after waiting for the application started messages, as all
     * binding messages should be present.
     */
    private void verifyBindings(LibertyServer verifyServer, int expectedLocal, int expectedRemote) throws Exception {
        List<String> bindings = verifyServer.findStringsInLogsUsingMark("CNTR0167I:.*ConfigTestsTestApp", verifyServer.getDefaultLogFile());
        assertEquals("binding messages not found", expectedLocal + expectedRemote, bindings.size());
    }

    /**
     * Waits for the expected enterprise bean binding messages in the log. <p>
     *
     * This method should be used after updating configuration that does not result in an application restart,
     * as the bindings will occur after the configuration update message.
     */
    private void waitForBindings(LibertyServer verifyServer, int expectedLocal, int expectedRemote) throws Exception {
        int bindingsFound = verifyServer.waitForMultipleStringsInLogUsingMark(expectedLocal + expectedRemote, "CNTR0167I:.*ConfigTestsTestApp");
        assertEquals("binding messages not found", expectedLocal + expectedRemote, bindingsFound);
    }

}
