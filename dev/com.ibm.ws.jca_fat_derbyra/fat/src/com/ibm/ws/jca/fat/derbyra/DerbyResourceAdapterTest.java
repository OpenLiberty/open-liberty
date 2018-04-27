/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.derbyra;

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

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class DerbyResourceAdapterTest extends FATServletClient {

    private static final String APP = "derbyRAApp";
    private static final String WAR_NAME = "fvtweb";

    private static final String derbyRAAppName = "derbyRAAppName";
    private static final String DerbyRAAnnoServlet = "fvtweb/DerbyRAAnnoServlet";
    private static final String DerbyRAServlet = "fvtweb/DerbyRAServlet";

    @Server("com.ibm.ws.jca.fat.derbyra")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_NAME + ".war");
        war.addPackage("web");
        war.addPackage("web.mdb");
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml"));
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/web.xml"));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP + ".ear");
        ear.addAsModule(war);
        ShrinkHelper.addDirectory(ear, "lib/LibertyFATTestFiles/derbyRAApp");
        ShrinkHelper.exportToServer(server, "apps", ear);

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "DerbyRA.rar");
        rar.as(JavaArchive.class).addPackage("fat.derbyra.resourceadapter");
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/wlp-ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/permissions.xml"));
        rar.addAsLibrary(new File("publish/shared/resources/derby/derby.jar"));

        ShrinkHelper.exportToServer(server, "connectors", rar);

        server.addInstalledAppForValidation(derbyRAAppName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("SRVE9967W", // The manifest class path derbyLocale_cs.jar can not be found in jar file wsjar:file:/C:/Users/IBM_ADMIN/Documents/workspace/build.image/wlp/usr/servers/com.ibm.ws.jca.fat.derbyra/connectors/DerbyRA.rar!/derby.jar or its parent.
                          // This may just be because we don't care about including manifest files in our test buckets, if that's the case, we can ignore this.
                          "J2CA0027E: .*eis/ds3", // Intentionally caused failure on XA.commit in order to cause in-doubt transaction
                          "J2CA0081E", //Expected due to simulated exception in testConnPoolStatsExceptionDestroy
                          "WTRN0048W: .*XAER_RMFAIL"); // Intentionally caused failure on XA.commit in order to cause in-doubt transaction
    }

    private void runTest(String servlet) throws Exception {
        FATServletClient.runTest(server, servlet, testName.getMethodName());
    }

    @Test
    public void testActivationSpec() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @ExpectedFFDC({ "javax.ejb.EJBException",
                    "javax.transaction.HeuristicMixedException",
                    "javax.transaction.xa.XAException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    @Test
    public void testActivationSpecXARecovery() throws Exception {
        server.setMarkToEndOfLog();
        runTest(DerbyRAAnnoServlet);
        // Wait for FFDC messages which can be reported asynchronously to the servlet thread
        server.waitForStringInLogUsingMark("FFDC1015I.*EJBException");
    }

    @Test
    public void testAdminObjectDirectLookup() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testAdminObjectInjected() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @Test
    public void testAdminObjectLookUpJavaApp() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testAdminObjectResourceEnvRef() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testExecutionContext() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testJCADataSourceDirectLookup() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testJCADataSourceInjected() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @Test
    public void testJCADataSourceInjectedAsCommonDataSource() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @Test
    public void testJCADataSourceLookUpJavaModule() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testJCADataSourceResourceRef() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testRecursiveTimer() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @Test
    public void testTransactionContext() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testTransactionSynchronizationRegistry() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @ExpectedFFDC("javax.transaction.xa.XAException") // intentionally caused failure to make the transaction in-doubt
    @Test
    public void testXARecovery() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @Test
    public void testXATerminator() throws Exception {
        runTest(DerbyRAAnnoServlet);
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" }) //simulated exception in destroy
    @Test
    public void testConnPoolStatsExceptionInDestroy() throws Exception {
        runTest(DerbyRAServlet);
    }

    @Test
    public void testErrorInFreeConn() throws Exception {
        runTest(DerbyRAServlet);
    }
}
