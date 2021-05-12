/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jca.fat.enterpriseApp;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EnterpriseAppTest extends FATServletClient {
    private final Class<? extends EnterpriseAppTest> c = this.getClass();
    private static final String appName = "enterpriseApp";
    private static final String fvtweb = "fvtweb";

    @Server("com.ibm.ws.jca.fat.enterpriseApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, fvtweb + ".war");
        war.addPackage("web");
        war.addPackage("web.mdb");
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml")); // includes login properties

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "enterpriseRA.rar");
        rar.as(JavaArchive.class).addPackage("com.ibm.test.jca.enterprisera");
        rar.addAsManifestResource(new File("test-resourceadapters/enterpriseAppRA/resources/META-INF/ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/enterpriseAppRA/resources/META-INF/wlp-ra.xml"));
        rar.addAsLibrary(new File("publish/shared/resources/derby/derby.jar"));

        ResourceAdapterArchive lmrar = ShrinkWrap.create(ResourceAdapterArchive.class, "loginModRA.rar");
        lmrar.as(JavaArchive.class).addPackage("com.ibm.test.jca.loginmodra");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear");
        ear.addAsModule(war);
        ear.addAsModule(rar);
        ear.addAsModule(lmrar);
        ShrinkHelper.addDirectory(ear, "lib/LibertyFATTestFiles/enterpriseApp");
        ShrinkHelper.exportToServer(server, "apps", ear);

        server.addEnvVar("PERMISSION", JakartaEE9Action.isActive() ? "jakarta.resource.spi.security.PasswordCredential" : "javax.resource.spi.security.PasswordCredential");
        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W");
    }

    private void runTest() throws Exception {
        runTest(server, fvtweb, getTestMethodSimpleName());
    }

    private StringBuilder runTestWithResponse(String query) throws Exception {
        return runTestWithResponse(server, fvtweb, getTestMethodSimpleName() + "&" + query);
    }

    @Test
    public void checkSetupTest() throws Exception {
        runTest();
    }

    @Test
    public void testAdminObjectInjected() throws Exception {
        runTest();
    }

    @Test
    public void testAdminObjectLookup() throws Exception {
        runTest();
    }

    @Test
    public void testConnectionFactoryUsesLoginModule() throws Exception {
        runTest();
    }

    @Test
    public void testDataSourceInjected() throws Exception {
        runTest();
    }

    @Test
    public void testDataSourceLookup() throws Exception {
        runTest();
    }

    @Test
    public void testDataSourceUsingLoginModule() throws Exception {
        runTest();
    }

    @Test
    public void testSimpleMBeanCreation() throws Exception {
        runTest();
    }

    @Test
    public void testPrizeWinner() throws Exception {
        runTestWithResponse("username=user1");

        runTestWithResponse("username=user2");

        if (runTestWithResponse("username=user1").indexOf("PRIZE!") > 0)
            throw new Exception("The 1st visitor should not have won a prize, but they did.");

        if (runTestWithResponse("testPrizeWinner&username=user3").indexOf("PRIZE!") < 0)
            throw new Exception("The 3rd visitor should have won a prize, but they did not.");
    }

    @Test
    public void testCheckoutLine() throws Exception {
        runTestWithResponse("testCheckoutLine&customer=cust0&function=ADD");
        runTestWithResponse("testCheckoutLine&customer=cust1&function=ADD");
        if (runTestWithResponse("testCheckoutLine&customer=cust2&function=ADD").indexOf("size=3") < 0)
            throw new Exception("Expected queue size to be 3 but it was not.");

        if (runTestWithResponse("testCheckoutLine&function=REMOVE").indexOf("size=2") < 0)
            throw new Exception("Expected queue size to be 2 but it was not.");

        if (runTestWithResponse("testCheckoutLine&customer=cust2&function=CONTAINS").indexOf("cust2 is in line") < 0)
            throw new Exception("Customer cust2 should be in line, but they were not.");
    }
}
