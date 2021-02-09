/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.tests;

import static junit.framework.Assert.assertNotNull;

import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.EJBAsynchronousElement;
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class AsyncConfigTests extends AbstractTest {
    private final static Logger logger = Logger.getLogger(AsyncConfigTests.class.getName());
    private static final String SERVLET = "AsyncConfigTestWeb/AsyncConfigServlet";

    @Server("com.ibm.ws.ejbcontainer.async.fat.AsyncConfigServer")
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncConfigServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncConfigServer")).andWith(new JakartaEE9Action().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncConfigServer"));

    private static Set<String> installedApps;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // cleanup from prior repeat actions
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the Ears & Wars

        //#################### AsyncConfigTestApp.ear
        JavaArchive AsyncConfigTestEJB = ShrinkHelper.buildJavaArchive("AsyncConfigTestEJB.jar", "com.ibm.ws.ejbcontainer.async.fat.config.ejb.");
        WebArchive AsyncConfigTestWeb = ShrinkHelper.buildDefaultApp("AsyncConfigTestWeb.war", "com.ibm.ws.ejbcontainer.async.fat.config.web.");
        EnterpriseArchive AsyncConfigTestApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncConfigTestApp.ear");
        AsyncConfigTestApp.addAsModule(AsyncConfigTestEJB).addAsModule(AsyncConfigTestWeb);
        AsyncConfigTestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(AsyncConfigTestApp, "test-applications/AsyncConfigTestApp.ear/resources");

        ShrinkHelper.exportAppToServer(server, AsyncConfigTestApp, DeployOptions.SERVER_ONLY);
        server.addInstalledAppForValidation("AsyncConfigTestApp");

        // Finally, start server
        server.startServer();

        // verify the appSecurity-2.0 feature is ready
        assertNotNull("Security service did not report it was ready", server.waitForStringInLogUsingMark("CWWKS0008I"));
        assertNotNull("LTPA configuration did not report it was ready", server.waitForStringInLogUsingMark("CWWKS4105I"));
        server.setMarkToEndOfLog();

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        // Only after the server has started and appSecurity-2.0 feature is ready,
        // then allow the @Startup InitTxRecoveryLog bean to start.
        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        // Save list of applications for server updates
        installedApps = server.listAllInstalledAppsForValidation();
        logger.info("Installed applications = " + installedApps);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // CWWKG0014E - intermittently caused by server.xml being momentarily missing during server reconfig
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKG0014E");
        }
    }

    /**
     * Test that setting asynchronous method ContextService to a custom ContextService
     * that propagates the security context will behave the same as the default
     * behavior when no ContextServcie is provided.
     */
    @Test
    public void testAsyncContextServiceConfigSameAsNoConfig() throws Exception {
        setAsyncMethodContextServiceConfigValue("SameAsNoConfigAsyncContextService");
        runTest(SERVLET);
    }

    /**
     * Test that setting asynchronous method ContextService to a custom ContextService
     * that propagates no contexts will result in the security context not
     * being propagated.
     */
    @Test
    public void testAsyncContextServiceConfigNoPropagation() throws Exception {
        setAsyncMethodContextServiceConfigValue("NoPropagationAsyncContextService");
        runTest(SERVLET);
    }

    /**
     * Test that an asynchronous configuration without any attributes will result
     * in the default behavior; the security context is propagated.
     */
    @Test
    public void testAsynchronousConfigWithNoContextService() throws Exception {
        setAsyncMethodContextServiceConfigValue("");
        runTest(SERVLET);
    }

    /**
     * Test that removing the setting for asynchronous configuration will result
     * in the default behavior; the security context is propagated.
     */
    @Test
    public void testAsynchronousConfigRemoval() throws Exception {
        setAsyncMethodContextServiceConfigValue(null);
        runTest(SERVLET);
    }

    /**
     * Change the setting of the asynchronous method ContextService to the specified value;
     * nothing is done if the specified value is the existing value.
     *
     * @param asyncContextService the name of the context service to use; null indicates no
     *            asynchronous element present; "" indicates no contextServiceRef attribute.
     */
    private void setAsyncMethodContextServiceConfigValue(String asyncContextService) throws Exception {

        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbContainer = config.getEJBContainer();
        EJBAsynchronousElement currentAsynchronous = ejbContainer.getAsynchronous();

        if (asyncContextService == null) {
            if (currentAsynchronous == null) {
                logger.info(testName.getMethodName() + " : asynchronous config already does not exist");
                return;
            }
            logger.info(testName.getMethodName() + " : removing asynchronous element");
            ejbContainer.setAsynchronous(null);
        } else {
            if (currentAsynchronous == null) {
                currentAsynchronous = new EJBAsynchronousElement();
                ejbContainer.setAsynchronous(currentAsynchronous);
            }

            String currentValue = currentAsynchronous.getContextServiceRef();
            String newValue = "".equals(asyncContextService) ? null : asyncContextService;

            if (currentValue == newValue ||
                (currentValue != null && currentValue.equals(newValue))) {
                logger.info(testName.getMethodName() + " : asynchronous:contextServiceRef config already set to " + newValue);
                return;
            } else {
                logger.info(testName.getMethodName() + " : setting asynchronous:contextServiceRef config to " + newValue);
                currentAsynchronous.setContextServiceRef(newValue);
            }
        }

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(installedApps);
    }
}