/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.jca.fat.derbyra;

import static org.junit.Assert.assertEquals;

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
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckpointTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

//import static com.ibm.ws.jca.fat.derbyra.FATSuite.setMockCheckpoint;

@CheckpointTest
@RunWith(FATRunner.class)
public class DerbyRACheckpointTest extends FATServletClient {

    private static final String SERVER_NAME = "com.ibm.ws.jca.fat.derbyra.checkpoint";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    static final String APP = "derbyRAApp";
    static final String WAR_NAME = "fvtweb";
    static final String RAR_NAME = "DerbyRA";

    static final String DerbyRAAnnoServlet = "fvtweb/DerbyRAAnnoServlet";
    static final String DerbyRACheckpointServlet = "fvtweb/DerbyRACheckpointServlet";
    static final String DerbyRACFDServlet = "fvtweb/DerbyRACFDServlet";
    static final String DerbyRAServlet = "fvtweb/DerbyRAServlet";

    static final String[] IGNORE_REGEX = new String[] { "SRVE9967W", // The manifest class path derbyLocale_cs.jar can not be found in jar file wsjar:file:/C:/Users/IBM_ADMIN/Documents/workspace/build.image/wlp/usr/servers/com.ibm.ws.jca.fat.derbyra/connectors/DerbyRA.rar!/derby.jar or its parent.
                                                        // This may just be because we don't care about including manifest files in our test buckets, if that's the case, we can ignore this.
                                                        "J2CA0027E: .*eis/ds3", // Intentionally caused failure on XA.commit in order to cause in-doubt transaction
                                                        "J2CA0081E", //Expected due to simulated exception in testConnPoolStatsExceptionDestroy
                                                        "WTRN0048W: .*XAER_RMFAIL" };

    @BeforeClass
    public static void setUpClass() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, WAR_NAME + ".war");
        war.addPackage("web");
        war.addPackage("web.cfd");
        war.addPackage("web.mdb");
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/ibm-web-bnd.xml"));
        war.addAsWebInfResource(new File("test-applications/fvtweb/resources/WEB-INF/web.xml"));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP + ".ear");
        ear.addAsModule(war);
        ShrinkHelper.addDirectory(ear, "lib/LibertyFATTestFiles/derbyRAApp");
        ShrinkHelper.exportAppToServer(server, ear, DeployOptions.SERVER_ONLY, DeployOptions.OVERWRITE);

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, RAR_NAME + ".rar");
        rar.as(JavaArchive.class).addPackage("fat.derbyra.resourceadapter");
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/wlp-ra.xml"));
        rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/permissions.xml"));
        rar.addAsLibrary(new File("publish/shared/resources/derby/derby.jar"));

        ShrinkHelper.exportToServer(server, "connectors", rar);

        server.addEnvVar("PERMISSION",
                         (JakartaEEAction.isEE9OrLaterActive()) ? "jakarta.resource.spi.security.PasswordCredential" : "javax.resource.spi.security.PasswordCredential");

        // Checkpoint and restore the server
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.addCheckpointRegexIgnoreMessages(IGNORE_REGEX);
        server.startServer();
        server.checkpointRestore();

        // Mock checkpoint and restore the server
        //setCheckpointPhase(server, CheckpointPhase.AFTER_APP_START);
        //server.startServer();

        FATServletClient.runTest(server, DerbyRAServlet, "initDatabaseTables");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (server.isStarted()) {
            server.stopServer(IGNORE_REGEX);
        }
    }

    @Test
    public void testActivationSpecAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testActivationSpec");
    }

    @ExpectedFFDC({ "javax.ejb.EJBException",
                    "javax.transaction.HeuristicMixedException",
                    "javax.transaction.xa.XAException",
                    "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    @AllowedFFDC({ "java.lang.RuntimeException" }) // Wraps expected exception;  msg delivers within inbound security context in EE10
    @Test
    public void testActivationSpecXARecoveryAAS() throws Exception {
        server.setMarkToEndOfLog();
        runTest(server, DerbyRAAnnoServlet, "testActivationSpecXARecovery");
        // Wait for FFDC messages which can be reported asynchronously to the servlet thread
        server.waitForStringInLogUsingMark("FFDC1015I.*EJBException");
    }

    @Test
    public void testAdminObjectDirectLookupAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testAdminObjectDirectLookup");
    }

    @Test
    public void testAdminObjectInjectedAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testAdminObjectInjected");
    }

    @Test
    public void testAdminObjectLookUpJavaAppAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testAdminObjectLookUpJavaApp");
    }

    @Test
    public void testAdminObjectResourceEnvRefAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testAdminObjectResourceEnvRef");
    }

    @Test
    public void testConnectionFactoryDefinitionLeakedConnectionWithAutoCloseEnabledAAS() throws Exception {
        runTest(server, DerbyRACFDServlet, "testConnectionFactoryDefinitionLeakConnectionWithAutoCloseEnabled");
        runTest(server, DerbyRACFDServlet, "testConnectionFactoryDefinitionLeakedConnectionWithAutoCloseEnabledClosed");
    }

    @Test
    public void testConnectionFactoryDefinitionLeakedConnectionWithAutoCloseDisabledAAS() throws Exception {
        runTest(server, DerbyRACFDServlet, "testConnectionFactoryDefinitionLeakConnectionWithAutoCloseDisabled");
        runTest(server, DerbyRACFDServlet, "testConnectionFactoryDefinitionLeakedConnectionWithAutoCloseDisabledNotClosed");
    }

    @Test
    public void testExecutionContextAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testExecutionContext");
    }

    @Test
    public void testHandleListClosesParkedHandleWhenMDBTransactionEndsAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testHandleListClosesParkedHandleWhenMDBTransactionEnds");
    }

    @Test
    public void testJCADataSourceDirectLookupAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testJCADataSourceDirectLookup");
    }

    @Test
    public void testJCADataSourceInjectedAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testJCADataSourceInjected");
    }

    @Test
    public void testJCADataSourceInjectedAsCommonDataSourceAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testJCADataSourceInjectedAsCommonDataSource");
    }

    @Test
    public void testJCADataSourceLookUpJavaModuleAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testJCADataSourceLookUpJavaModule");
    }

    @Test
    public void testJCADataSourceResourceRefAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testJCADataSourceResourceRef");
    }

    @Test
    public void testNonDissociatableHandlesCannotBeParkedAcrossTransactionScopesAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testNonDissociatableHandlesCannotBeParkedAcrossTransactionScopes");
    }

    @Test
    public void testNonDissociatableHandlesParkedAcrossEJBMethodsAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testNonDissociatableHandlesParkedAcrossEJBMethods");
    }

    @Test
    public void testNonDissociatableSharableHandleIsClosedAcrossServletMethodsAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testNonDissociatableSharableHandleLeftOpenAfterServletMethod");
        runTest(server, DerbyRAServlet, "testNonDissociatableSharableHandleIsClosed");
    }

    @Test
    public void testParkNonDissociatableSharableHandleAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testParkNonDissociatableSharableHandle");
    }

    @Test
    public void testRecursiveTimerAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testRecursiveTimer");
    }

    @Test
    public void testTransactionContextAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testTransactionContext");
    }

    @Test
    public void testTransactionSynchronizationRegistryAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testTransactionSynchronizationRegistry");
    }

    @Test
    public void testUnsharableConnectionAcrossEJBGlobalTranAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testUnsharableConnectionAcrossEJBGlobalTran");
    }

    @ExpectedFFDC("javax.transaction.xa.XAException") // intentionally caused failure to make the transaction in-doubt
    @Test
    public void testXARecoveryAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testXARecovery");
    }

    @Test
    public void testXATerminatorAAS() throws Exception {
        runTest(server, DerbyRAAnnoServlet, "testXATerminator");
    }

    @ExpectedFFDC({ "javax.resource.ResourceException" }) //simulated exception in destroy
    @Test
    public void testConnPoolStatsExceptionInDestroyAAS() throws Exception {
        runTest(server, DerbyRAServlet, "testConnPoolStatsExceptionInDestroy");
    }

    @Test
    public void testErrorInFreeConnAAS() throws Exception {
        server.setTraceMarkToEndOfDefaultTrace();
        runTest(server, DerbyRAServlet, "testErrorInFreeConn");
        assertEquals("J2CA1004I should have been found in logs", 1, server.findStringsInLogsUsingMark("J2CA1004I", server.getDefaultTraceFile()).size());
    }

    @Test
    public void testErrorInUsedConnAAS() throws Exception {
        server.setTraceMarkToEndOfDefaultTrace();
        runTest(server, DerbyRAServlet, "testErrorInUsedConn");
        assertEquals("J2CA0056I should have been found in logs", 1, server.findStringsInLogsUsingMark("J2CA0056I", server.getDefaultTraceFile()).size());
    }
}
