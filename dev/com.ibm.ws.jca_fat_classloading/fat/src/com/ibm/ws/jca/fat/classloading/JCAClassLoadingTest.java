/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.classloading;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class JCAClassLoadingTest extends FATServletClient {

    @Server("com.ibm.ws.jca.fat.classloading")
    public static LibertyServer server;

    private static final String WAR_NAME = "fvtweb";
    private static final String APP_NAME = "ClassLoadingApp";
    private static final String RAR_NAME = "fvtra";

    private void runTest() throws Exception {
        runTest(server, WAR_NAME, testName);
    }

    private void restartWithNewConfig(boolean validateAppStarted) throws Exception {
        if (server.isStarted())
            server.stopServer();

        server.setServerConfigurationFile(testName.getMethodName() + "_server.xml");
        if (validateAppStarted) {
            server.addInstalledAppForValidation(APP_NAME);
        } else {
            server.removeInstalledAppForValidation(APP_NAME);
        }
        server.startServer(testName.getMethodName() + ".log");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(WAR_NAME, "web");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(war);
        ShrinkHelper.addDirectory(ear, "test-applications/ClassLoadingApp/resources/");
        ShrinkHelper.exportAppToServer(server, ear);

        ShrinkHelper.defaultRar(server, RAR_NAME, "ra");
    }

    @After
    public void tearDownPerTest() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }

    @Test
    public void testLoadResourceAdapterClassFromSingleApp() throws Exception {
        restartWithNewConfig(true);
        runTest();
    }

    @Test
    @Mode(FULL)
    public void testApiTypeVisibilityNone() throws Exception {
        restartWithNewConfig(true);
        runTest();
    }

    @Test
    @Mode(FULL)
    public void testApiTypeVisibilityAll() throws Exception {
        restartWithNewConfig(true);
        runTest();
    }

    // This test passes because the resource adapter's class loader gateway is set to an invalid
    // apiTypeVisibility value, which the class loading service accepts and causes the loader to
    // lacks access to "spec" classes.  There is no error message for an invalid apiTypeVisbility
    // value.
    @ExpectedFFDC({ "java.lang.NoClassDefFoundError" })
    @Test
    @Mode(FULL)
    public void testApiTypeVisibilityInvalid() throws Exception {
        restartWithNewConfig(false);

        String msg = server
                        .waitForStringInLogUsingMark("J2CA7002E: An exception occurred while installing the resource adapter ATVInvalid_RA. The exception message is: java.lang.NoClassDefFoundError: javax.resource.spi.ResourceAdapter");
        assertNotNull("Resource adapter lacks [spec] class loading api type visibility", msg);

        // put the app back on the server when we are done
        server.stopServer("J2CA7002E");
        ShrinkHelper.defaultApp(server, APP_NAME, "web");
    }

    @Test
    @Mode(FULL)
    public void testApiTypeVisibilityMatch() throws Exception {
        restartWithNewConfig(true);
        runTest();
    }

    @Test
    @Mode(FULL)
    public void testInvalidClassProviderRef() throws Exception {
        restartWithNewConfig(true);

        String msg = server
                        .waitForStringInLogUsingMark("CWWKG0033W: The value \\[invalidRef\\] specified for the reference attribute \\[classProviderRef\\] was not found in the configuration");
        assertNotNull("Expected to see message 'CWWKG0033W' in logs but did not find it when using an invalid classProviderRef.", msg);

        // Manually do the stop here so we don't ignore this exception for all tests in the @After stop
        server.stopServer("CWWKG0033W");
    }

    @Test
    @Mode(FULL)
    public void testApiTypeVisibilityMismatch() throws Exception {
        restartWithNewConfig(false);

        String msg = server.waitForStringInLogUsingMark("CWWKL0033W");
        assertNotNull("Expected to see error message 'CWWKL0033W' when applicaiton and resource adapter apiTypeVisibility do not match.", msg);

        // put the app back on the server when we are done
        server.stopServer("CWWKL0033W");
        ShrinkHelper.defaultApp(server, APP_NAME, "web");
    }

    @Test
    @Mode(FULL)
    public void testClassSpaceRestriction() throws Exception {
        restartWithNewConfig(true);
        runTest();
    }
}
