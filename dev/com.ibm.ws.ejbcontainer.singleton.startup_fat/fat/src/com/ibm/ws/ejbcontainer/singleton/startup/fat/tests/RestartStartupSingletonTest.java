/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.singleton.startup.fat.tests;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that an application which fails in a startup singleton post construct is properly
 * cleaned up and may be started successfully after correcting the error, without restarting
 * the server.
 *
 * The following four scenarios are covered:
 * <ol>
 * <li>Failure occurs in standalone Web (WAR) application</li>
 * <li>Failure occurs in standalone EJB (JAR) application</li>
 * <li>Failure occurs in Web (WAR) module in an EE application (EAR)</li>
 * <li>Failure occurs in EJB (JAR) module in an EE application (EAR)<li>
 * </ol>
 *
 * Verification for each scenario will not only check for messages in the log to ensure
 * the application started properly after the error, but also that the EJBs may be looked
 * up and accessed using various JNDI bindings, to ensure the JNDI bindings are properly
 * removed when the application fails to start.
 */
@RunWith(FATRunner.class)
public class RestartStartupSingletonTest extends FATServletClient {

    @Server("RestartStartupSingletonServer")
    public static LibertyServer server;

    private static WebArchive RestartStartupSingletonWeb = null;
    private static WebArchive RestartStartupSingletonWeb_Fail = null;
    private static JavaArchive RestartStartupSingletonEjb = null;
    private static JavaArchive RestartStartupSingletonEjb_Fail = null;
    private static EnterpriseArchive RestartStartupSingletonApp = null;
    private static EnterpriseArchive RestartStartupSingletonApp_Fail_War = null;
    private static EnterpriseArchive RestartStartupSingletonApp_Fail_Jar = null;

    @BeforeClass
    public static void setUp() throws Exception {

        // Use ShrinkHelper to build the applications, exporting passing ones to server

        // -------------- RestartStartupSingletonIntf.jar ------------
        JavaArchive RestartStartupSingletonIntf = ShrinkHelper.buildJavaArchive("RestartStartupSingletonIntf.jar", "test.ejbcontainer.restart.singleton.shared.");
        ShrinkHelper.exportToServer(server, "lib", RestartStartupSingletonIntf, DeployOptions.SERVER_ONLY);

        // -------------- RestartStartupSingletonWeb.war ------------
        RestartStartupSingletonWeb = ShrinkHelper.buildDefaultApp("RestartStartupSingletonWeb.war", "test.ejbcontainer.restart.singleton.web.");
        ShrinkHelper.exportAppToServer(server, RestartStartupSingletonWeb, DeployOptions.SERVER_ONLY);

        // -------------- RestartStartupSingletonWeb.war with env-entry to fail on start ------------
        RestartStartupSingletonWeb_Fail = ShrinkHelper.buildDefaultApp("RestartStartupSingletonWeb.war", "test.ejbcontainer.restart.singleton.web.");
        ShrinkHelper.addDirectory(RestartStartupSingletonWeb_Fail, "test-applications/RestartStartupSingletonWeb.war/resources.fail");

        // -------------- RestartStartupSingletonEjb.jar ------------
        RestartStartupSingletonEjb = ShrinkHelper.buildJavaArchive("RestartStartupSingletonEjb.jar", "test.ejbcontainer.restart.singleton.jar.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEjb, "test-applications/RestartStartupSingletonEjb.jar/resources");
        ShrinkHelper.exportAppToServer(server, RestartStartupSingletonEjb, DeployOptions.SERVER_ONLY);

        // -------------- RestartStartupSingletonEjb.jar with env-entry to fail on start ------------
        RestartStartupSingletonEjb_Fail = ShrinkHelper.buildJavaArchive("RestartStartupSingletonEjb.jar", "test.ejbcontainer.restart.singleton.jar.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEjb_Fail, "test-applications/RestartStartupSingletonEjb.jar/resources.fail");

        // -------------- RestartStartupSingletonApp.ear ------------
        JavaArchive RestartStartupSingletonEarEjb = ShrinkHelper.buildJavaArchive("RestartStartupSingletonEarEjb.jar", "test.ejbcontainer.restart.singleton.ear.jar.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEarEjb, "test-applications/RestartStartupSingletonApp.ear/resources.jar");
        WebArchive RestartStartupSingletonEarWeb = ShrinkHelper.buildDefaultApp("RestartStartupSingletonEarWeb.war", "test.ejbcontainer.restart.singleton.ear.war.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEarWeb, "test-applications/RestartStartupSingletonApp.ear/resources.war");
        RestartStartupSingletonApp = ShrinkWrap.create(EnterpriseArchive.class, "RestartStartupSingletonApp.ear");
        RestartStartupSingletonApp.addAsModules(RestartStartupSingletonEarEjb, RestartStartupSingletonEarWeb);
        ShrinkHelper.addDirectory(RestartStartupSingletonApp, "test-applications/RestartStartupSingletonApp.ear/resources");
        ShrinkHelper.exportAppToServer(server, RestartStartupSingletonApp, DeployOptions.SERVER_ONLY);

        // -------------- RestartStartupSingletonApp.jar with env-entry in war to fail on start ------------
        RestartStartupSingletonEarEjb = ShrinkHelper.buildJavaArchive("RestartStartupSingletonEarEjb.jar", "test.ejbcontainer.restart.singleton.ear.jar.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEarEjb, "test-applications/RestartStartupSingletonApp.ear/resources.jar");
        RestartStartupSingletonEarWeb = ShrinkHelper.buildDefaultApp("RestartStartupSingletonEarWeb.war", "test.ejbcontainer.restart.singleton.ear.war.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEarWeb, "test-applications/RestartStartupSingletonApp.ear/resources.war.fail");
        RestartStartupSingletonApp_Fail_War = ShrinkWrap.create(EnterpriseArchive.class, "RestartStartupSingletonApp.ear");
        RestartStartupSingletonApp_Fail_War.addAsModules(RestartStartupSingletonEarEjb, RestartStartupSingletonEarWeb);
        ShrinkHelper.addDirectory(RestartStartupSingletonApp_Fail_War, "test-applications/RestartStartupSingletonApp.ear/resources");

        // -------------- RestartStartupSingletonApp.jar with env-entry in jar to fail on start ------------
        RestartStartupSingletonEarEjb = ShrinkHelper.buildJavaArchive("RestartStartupSingletonEarEjb.jar", "test.ejbcontainer.restart.singleton.ear.jar.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEarEjb, "test-applications/RestartStartupSingletonApp.ear/resources.jar.fail");
        RestartStartupSingletonEarWeb = ShrinkHelper.buildDefaultApp("RestartStartupSingletonEarWeb.war", "test.ejbcontainer.restart.singleton.ear.war.");
        ShrinkHelper.addDirectory(RestartStartupSingletonEarWeb, "test-applications/RestartStartupSingletonApp.ear/resources.war");
        RestartStartupSingletonApp_Fail_Jar = ShrinkWrap.create(EnterpriseArchive.class, "RestartStartupSingletonApp.ear");
        RestartStartupSingletonApp_Fail_Jar.addAsModules(RestartStartupSingletonEarEjb, RestartStartupSingletonEarWeb);
        ShrinkHelper.addDirectory(RestartStartupSingletonApp_Fail_Jar, "test-applications/RestartStartupSingletonApp.ear/resources");

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        // CNTR0020E: EJB threw an unexpected (non-declared) exception during invocation of method "postConstruct"
        // CNTR0190E: The * startup singleton session bean in the * module failed initialization
        // CNTR4002E: The * EJB module in the * application failed to start
        // CNTR0201E: The * startup singleton session bean in the * module failed initialization
        // CWWKZ0106E: Could not start web application RestartStartupSingletonApp
        // CWWKZ0059E: The * application installed from * has been deleted while it is still configured
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0020E", "CNTR0190E", "CNTR4002E", "CNTR0201E", "CWWKZ0106E", "CWWKZ0059E");
        }
    }

    /**
     * Test that a standalone Web (WAR) application which fails in a startup singleton post construct
     * is properly cleaned up and may be started successfully after correcting the error, without restarting
     * the server.
     *
     * Verifies that after the application starts successfully, EJBs may be looked up in JNDI with various
     * bindings and accessed.
     **/
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "javax.ejb.NoSuchEJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    @AllowedFFDC("org.omg.CosNaming.NamingContextPackage.NotFound")
    public void testRestartSingletonWarAfterStartupError() throws Exception {

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");

        Thread.sleep(1500); // ensure application updates do not occur to quickly
        server.setMarkToEndOfLog();

        // Publish the application with env-entry to fail on startup
        ShrinkHelper.exportAppToServer(server, RestartStartupSingletonWeb_Fail, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        try {
            System.out.println("Waiting for RestartStartupSingletonWeb to stop, then fail on restart");
            assertNotNull("Did not report EJB module RestartStartupSingletonWeb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonWeb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report application RestartStartupSingletonWeb stopped, CWWKZ0009I",
                          server.waitForStringInLogUsingMark("CWWKZ0009I.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report application RestartStartupSingletonWeb starting, CWWKZ0018I",
                          server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonWeb starting, CNTR4000I",
                          server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report exception from RestartStartupSingletonWarBean, CNTR0020E",
                          server.waitForStringInLogUsingMark("CNTR0020E.*RestartStartupSingletonWarBean"));
            assertNotNull("Did not report startup singleton failed initialization, CNTR0190E",
                          server.waitForStringInLogUsingMark("CNTR0190E.* RestartStartupSingletonWarBean"));
            assertNotNull("Did not report EJB module RestartStartupSingletonWeb failed, CNTR4002E",
                          server.waitForStringInLogUsingMark("CNTR4002E.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonWeb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonWeb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonWeb"));
            assertNotNull("Did not report exception starting application RestartStartupSingletonWeb, CWWKZ0004E",
                          server.waitForStringInLogUsingMark("CWWKZ0004E.* RestartStartupSingletonWeb"));
        } finally {
            Thread.sleep(1500); // ensure application updates do not occur to quickly
            server.setMarkToEndOfLog();

            // Restore the application with env-entry to pass on startup
            ShrinkHelper.exportAppToServer(server, RestartStartupSingletonWeb, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);
        }

        System.out.println("Waiting for RestartStartupSingletonWeb to restart successfully");
        assertNotNull("Did not report application RestartStartupSingletonWeb starting, CWWKZ0018I",
                      server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonWeb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonWeb starting, CNTR4000I",
                      server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonWeb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonWeb started, CNTR4001I",
                      server.waitForStringInLogUsingMark("CNTR4001I.* RestartStartupSingletonWeb"));
        assertNotNull("Did not report application RestartStartupSingletonWeb updated, CWWKZ0003I",
                      server.waitForStringInLogUsingMark("CWWKZ0003I.* RestartStartupSingletonWeb"));

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");
    }

    /**
     * Test that a standalone EJB (JAR) application which fails in a startup singleton post construct
     * is properly cleaned up and may be started successfully after correcting the error, without restarting
     * the server.
     *
     * Verifies that after the application starts successfully, EJBs may be looked up in JNDI with various
     * bindings and accessed.
     **/
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "javax.ejb.NoSuchEJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    @AllowedFFDC("org.omg.CosNaming.NamingContextPackage.NotFound")
    public void testRestartSingletonJarAfterStartupError() throws Exception {

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");

        Thread.sleep(1500); // ensure application updates do not occur to quickly
        server.setMarkToEndOfLog();

        // Publish the application with env-entry to fail on startup
        ShrinkHelper.exportAppToServer(server, RestartStartupSingletonEjb_Fail, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        try {
            System.out.println("Waiting for RestartStartupSingletonEjb to stop, then fail on restart");
            assertNotNull("Did not report EJB module RestartStartupSingletonEjb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEjb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEjb"));
            assertNotNull("Did not report application RestartStartupSingletonEjb stopped, CWWKZ0009I",
                          server.waitForStringInLogUsingMark("CWWKZ0009I.* RestartStartupSingletonEjb"));
            assertNotNull("Did not report application RestartStartupSingletonEjb starting, CWWKZ0018I",
                          server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEjb starting, CNTR4000I",
                          server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEjb"));
            assertNotNull("Did not report exception from RestartStartupSingletonJarBean, CNTR0020E",
                          server.waitForStringInLogUsingMark("CNTR0020E.*RestartStartupSingletonJarBean"));
            assertNotNull("Did not report startup singleton failed initialization, CNTR0190E",
                          server.waitForStringInLogUsingMark("CNTR0190E.* RestartStartupSingletonJarBean"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEjb failed, CNTR4002E",
                          server.waitForStringInLogUsingMark("CNTR4002E.* RestartStartupSingletonEjb"));
            assertNotNull("Did not report exception starting application RestartStartupSingletonEjb, CWWKZ0004E",
                          server.waitForStringInLogUsingMark("CWWKZ0004E.* RestartStartupSingletonEjb"));
        } finally {
            Thread.sleep(1500); // ensure application updates do not occur to quickly
            server.setMarkToEndOfLog();

            // Restore the application with env-entry to pass on startup
            ShrinkHelper.exportAppToServer(server, RestartStartupSingletonEjb, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);
        }

        System.out.println("Waiting for RestartStartupSingletonEjb to restart successfully");
        assertNotNull("Did not report application RestartStartupSingletonEjb starting, CWWKZ0018I",
                      server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonEjb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEjb starting, CNTR4000I",
                      server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEjb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEjb started, CNTR4001I",
                      server.waitForStringInLogUsingMark("CNTR4001I.* RestartStartupSingletonEjb"));
        assertNotNull("Did not report application RestartStartupSingletonEjb updated, CWWKZ0003I",
                      server.waitForStringInLogUsingMark("CWWKZ0003I.* RestartStartupSingletonEjb"));

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");
    }

    /**
     * Test that an EE application (EAR) which fails in a startup singleton post construct in a Web (WAR)
     * module is properly cleaned up and may be started successfully after correcting the error, without
     * restarting the server.
     *
     * Verifies that after the application starts successfully, EJBs may be looked up in JNDI with various
     * bindings and accessed.
     **/
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "javax.ejb.NoSuchEJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    @AllowedFFDC("org.omg.CosNaming.NamingContextPackage.NotFound")
    public void testRestartSingletonEarWarAfterStartupError() throws Exception {

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");

        Thread.sleep(1500); // ensure application updates do not occur to quickly
        server.setMarkToEndOfLog();

        // Publish the application with env-entry to fail war on startup
        server.deleteFileFromLibertyServerRoot("apps/RestartStartupSingletonApp.ear");
        ShrinkHelper.exportAppToServer(server, RestartStartupSingletonApp_Fail_War, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        try {
            System.out.println("Waiting for RestartStartupSingletonApp to stop, then fail on restart");
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report application RestartStartupSingletonApp stopped, CWWKZ0009I",
                          server.waitForStringInLogUsingMark("CWWKZ0009I.* RestartStartupSingletonApp"));
            assertNotNull("Did not report application RestartStartupSingletonApp starting, CWWKZ0018I",
                          server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonApp"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb starting, CNTR4000I",
                          server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb started, CNTR4001I",
                          server.waitForStringInLogUsingMark("CNTR4001I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb starting, CNTR4000I",
                          server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report exception from RestartStartupSingletonEarWarBean, CNTR0020E",
                          server.waitForStringInLogUsingMark("CNTR0020E.*RestartStartupSingletonEarWarBean"));
            assertNotNull("Did not report startup singleton failed initialization, CNTR0190E",
                          server.waitForStringInLogUsingMark("CNTR0190E.* RestartStartupSingletonEarWarBean"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb failed, CNTR4002E",
                          server.waitForStringInLogUsingMark("CNTR4002E.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report could not start web application RestartStartupSingletonApp, CWWKZ0106E",
                          server.waitForStringInLogUsingMark("CWWKZ0106E.* RestartStartupSingletonApp"));
            assertNotNull("Did not report exception starting application RestartStartupSingletonApp, CWWKZ0004E",
                          server.waitForStringInLogUsingMark("CWWKZ0004E.* RestartStartupSingletonApp"));
        } finally {
            Thread.sleep(1500); // ensure application updates do not occur to quickly
            server.setMarkToEndOfLog();

            // Restore the application with env-entry to pass on startup
            server.deleteFileFromLibertyServerRoot("apps/RestartStartupSingletonApp.ear");
            ShrinkHelper.exportAppToServer(server, RestartStartupSingletonApp, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);
        }

        System.out.println("Waiting for RestartStartupSingletonApp to restart successfully");
        assertNotNull("Did not report application RestartStartupSingletonApp starting, CWWKZ0018I",
                      server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonApp"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb starting, CNTR4000I",
                      server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEarEjb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb started, CNTR4001I",
                      server.waitForStringInLogUsingMark("CNTR4001I.* RestartStartupSingletonEarEjb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb starting, CNTR4000I",
                      server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEarWeb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb started, CNTR4001I",
                      server.waitForStringInLogUsingMark("CNTR4001I.* RestartStartupSingletonEarWeb"));
        assertNotNull("Did not report application RestartStartupSingletonApp updated, CWWKZ0003I",
                      server.waitForStringInLogUsingMark("CWWKZ0003I.* RestartStartupSingletonApp"));

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");
    }

    /**
     * Test that an EE application (EAR) which fails in a startup singleton post construct in an EJB (JAR)
     * module is properly cleaned up and may be started successfully after correcting the error, without
     * restarting the server.
     *
     * Verifies that after the application starts successfully, EJBs may be looked up in JNDI with various
     * bindings and accessed.
     **/
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "javax.ejb.NoSuchEJBException", "com.ibm.ws.container.service.state.StateChangeException" })
    @AllowedFFDC("org.omg.CosNaming.NamingContextPackage.NotFound")
    public void testRestartSingletonEarJarAfterStartupError() throws Exception {

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");

        Thread.sleep(1500); // ensure application updates do not occur to quickly
        server.setMarkToEndOfLog();

        // Publish the application with env-entry to fail jar on startup
        server.deleteFileFromLibertyServerRoot("apps/RestartStartupSingletonApp.ear");
        ShrinkHelper.exportAppToServer(server, RestartStartupSingletonApp_Fail_Jar, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        try {
            System.out.println("Waiting for RestartStartupSingletonApp to stop, then fail on restart");
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEarWeb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report application RestartStartupSingletonApp stopped, CWWKZ0009I",
                          server.waitForStringInLogUsingMark("CWWKZ0009I.* RestartStartupSingletonApp"));
            assertNotNull("Did not report application RestartStartupSingletonApp starting, CWWKZ0018I",
                          server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonApp"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb starting, CNTR4000I",
                          server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report exception from RestartStartupSingletonEarJarBean, CNTR0020E",
                          server.waitForStringInLogUsingMark("CNTR0020E.*RestartStartupSingletonEarJarBean"));
            assertNotNull("Did not report startup singleton failed initialization, CNTR0190E",
                          server.waitForStringInLogUsingMark("CNTR0190E.* RestartStartupSingletonEarJarBean"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb failed, CNTR4002E",
                          server.waitForStringInLogUsingMark("CNTR4002E.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopping, CNTR4003I",
                          server.waitForStringInLogUsingMark("CNTR4003I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb stopped, CNTR4004I",
                          server.waitForStringInLogUsingMark("CNTR4004I.* RestartStartupSingletonEarEjb"));
            assertNotNull("Did not report could not start web application RestartStartupSingletonApp, CWWKZ0106E",
                          server.waitForStringInLogUsingMark("CWWKZ0106E.* RestartStartupSingletonApp"));
            assertNotNull("Did not report exception starting application RestartStartupSingletonApp, CWWKZ0004E",
                          server.waitForStringInLogUsingMark("CWWKZ0004E.* RestartStartupSingletonApp"));
        } finally {
            Thread.sleep(1500); // ensure application updates do not occur to quickly
            server.setMarkToEndOfLog();

            // Restore the application with env-entry to pass on startup
            server.deleteFileFromLibertyServerRoot("apps/RestartStartupSingletonApp.ear");
            ShrinkHelper.exportAppToServer(server, RestartStartupSingletonApp, DeployOptions.DISABLE_VALIDATION, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);
        }

        System.out.println("Waiting for RestartStartupSingletonApp to restart successfully");
        assertNotNull("Did not report application RestartStartupSingletonApp starting, CWWKZ0018I",
                      server.waitForStringInLogUsingMark("CWWKZ0018I.* RestartStartupSingletonApp"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb starting, CNTR4000I",
                      server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEarEjb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarEjb started, CNTR4001I",
                      server.waitForStringInLogUsingMark("CNTR4001I.* RestartStartupSingletonEarEjb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb starting, CNTR4000I",
                      server.waitForStringInLogUsingMark("CNTR4000I.* RestartStartupSingletonEarWeb"));
        assertNotNull("Did not report EJB module RestartStartupSingletonEarWeb started, CNTR4001I",
                      server.waitForStringInLogUsingMark("CNTR4001I.* RestartStartupSingletonEarWeb"));
        assertNotNull("Did not report application RestartStartupSingletonApp updated, CWWKZ0003I",
                      server.waitForStringInLogUsingMark("CWWKZ0003I.* RestartStartupSingletonApp"));

        runTest(server, "RestartStartupSingletonWeb/RestartStartupSingletonServlet", "verify");
    }

}
