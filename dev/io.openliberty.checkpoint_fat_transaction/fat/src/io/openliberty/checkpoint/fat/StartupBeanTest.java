/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.deleteTranlogDir;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static io.openliberty.checkpoint.fat.FATSuite.stopServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Verify checkpoint fails when a startup bean (EJB Session bean), or its container,
 * begins a transaction during application startup.
 */
@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class StartupBeanTest extends FATServletClient {

    static final String SERVER_NAME = "checkpointTransactionStartupBean";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(SERVER_NAME).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(SERVER_NAME).fullFATOnly());

    static final String APP_NAME = "transactionstartupbean";

    TestMethod testMethod;
    JavaArchive TxStartupBeanJar;
    EnterpriseArchive TxStartupBeanEar;

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        switch (testMethod) {
            case testStartupBeanRequiresNewAtDeployment:
                TxStartupBeanJar = ShrinkHelper.buildJavaArchive(APP_NAME, "com.ibm.ws.transaction.ejb.first");
                TxStartupBeanEar = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
                TxStartupBeanEar.addAsModule(TxStartupBeanJar);
                ShrinkHelper.exportDropinAppToServer(server, TxStartupBeanEar);

                server.setCheckpoint(CheckpointPhase.DEPLOYMENT, false, null);
                break;
            case testStartupBeanRequiresNewAtApplications:
                TxStartupBeanJar = ShrinkHelper.buildJavaArchive(APP_NAME, "com.ibm.ws.transaction.ejb.first");
                TxStartupBeanEar = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
                TxStartupBeanEar.addAsModule(TxStartupBeanJar);
                ShrinkHelper.exportDropinAppToServer(server, TxStartupBeanEar);

                // Expect checkpoint and restore to fail
                server.setCheckpoint(new CheckpointInfo(CheckpointPhase.APPLICATIONS, false, true, true, null));
                break;
            case testStartupBeanUserTranAtApplications:
                TxStartupBeanJar = ShrinkHelper.buildJavaArchive(APP_NAME, "com.ibm.ws.transaction.ejb.second");
                TxStartupBeanEar = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
                TxStartupBeanEar.addAsModule(TxStartupBeanJar);
                ShrinkHelper.exportDropinAppToServer(server, TxStartupBeanEar);

                server.setCheckpoint(new CheckpointInfo(CheckpointPhase.APPLICATIONS, false, true, true, null));
                break;
            default:
                break;
        }
    }

    @After
    public void tearDown() throws Exception {
        stopServer(server);
        deleteTranlogDir(server);
        ShrinkHelper.cleanAllExportedArchives();
    }

    /**
     * Verify checkpoint at=deployment completes, ensuring the bean does not begin
     * a container-managed transaction during application deployment.
     */
    @Test
    public void testStartupBeanRequiresNewAtDeployment() throws Exception {
        // Request a server checkpoint
        server.setServerStartTimeout(300000);
        server.startServer(getTestMethod(TestMethod.class, testName) + ".log");

        assertNotNull("The container should bind startup bean InitNewTxBean1 during checkpoint at=deployment, but did not.",
                      server.waitForStringInLogUsingMark("CNTR0167I:.*InitNewTxBean1"));

        server.checkpointRestore();

        assertNotNull("The container should invoke @PostConstruct method InitNewTxBean1.initTx1() during restore, but did not.",
                      server.waitForStringInLogUsingMark("initTx1"));

        assertNotNull("Method InitNewTxBean1.initTx1() should complete a UOW in a CMT during restore, but did not.",
                      server.waitForStringInLogUsingMark("Leaving the UOWAction"));
    }

    /**
     * Verify checkpoint at=applications fails when the startup bean begins a container-managed
     * transaction during application startup.
     */
    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testStartupBeanRequiresNewAtApplications() throws Exception {
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int returnCode = output.getReturnCode();
        assertEquals("The server checkpoint request should have returned failure code 72, but did not.", 72, returnCode);

        assertNotNull("The transaction manager should report it is unable to begin a transaction during a checkpoint request, but did not.",
                      server.waitForStringInLogUsingMark("WTRN0154"));
    }

    /**
     * Verify checkpoint at=applications fails when a startup bean begins a user transaction
     * during application startup.
     */
    @Test
    @ExpectedFFDC("io.openliberty.checkpoint.internal.criu.CheckpointFailedException")
    public void testStartupBeanUserTranAtApplications() throws Exception {
        ProgramOutput output = server.startServer(getTestMethodNameOnly(testName) + ".log");
        int returnCode = output.getReturnCode();
        assertEquals("The server checkpoint request should have returned failure code 72, but did not.", 72, returnCode);

        assertNotNull("The transaction manager should report it is unable to begin a transaction during a checkpoint request, but did not.",
                      server.waitForStringInLogUsingMark("WTRN0154"));
    }

    static enum TestMethod {
        testStartupBeanRequiresNewAtApplications,
        testStartupBeanRequiresNewAtDeployment,
        testStartupBeanUserTranAtApplications,
        unknown;
    }
}
