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

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
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

                server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
                break;
            case testStartupBeanUserTranAtApplications:
                TxStartupBeanJar = ShrinkHelper.buildJavaArchive(APP_NAME, "com.ibm.ws.transaction.ejb.second");
                TxStartupBeanEar = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
                TxStartupBeanEar.addAsModule(TxStartupBeanJar);
                ShrinkHelper.exportDropinAppToServer(server, TxStartupBeanEar);

                server.setCheckpoint(CheckpointPhase.APPLICATIONS, false, null);
                break;
            default:
                break;
        }
        server.setServerStartTimeout(300000);
        server.startServer(getTestMethod(TestMethod.class, testName) + ".log");
    }

    @After
    public void tearDown() throws Exception {
        stopServer();
        ShrinkHelper.cleanAllExportedArchives();
    }

    static void stopServer() {
        if (server.isStarted()) {
            try {
                server.stopServer();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Verify checkpoint at=deployment completes, ensuring the EJB container does not begin
     * a transaction for a startup bean during application deployment.
     */
    @Test
    public void testStartupBeanRequiresNewAtDeployment() throws Exception {
        assertNotNull("The container should bind startup bean InitNewTxBean1 during checkpoint at=deployment, but did not.",
                      server.waitForStringInLogUsingMark("CNTR0167I:.*InitNewTxBean1"));

        server.checkpointRestore();

        assertNotNull("The container should construct startup bean InitNewTxBean1 during restore, but did not.",
                      server.waitForStringInLogUsingMark("InitTx1"));
    }

    // TODO: FEATURE SUPPORT NOT YET IMPLEMENTED
    // Currently, the container binds the startup bean when the app is deployed and
    // then constructs the bean when the app starts. So, the container begins a tx during
    // checkpoint at=applications but not at=deployment.
    //
    // The TM should fail checkpoint whenever a transaction begins to ensure a tran log
    // does not initialize.
    //
    // MODIFY THIS TEST either to verify checkpoint fails when the container begins
    // the tx during @PostConstruct or verify @PostConstruct executes during restore.
    /**
     * Verify checkpoint at=applications fails when the EJB container begins a transaction
     * for a startup bean during application startup.
     */
    @Test
    public void testStartupBeanRequiresNewAtApplications() throws Exception {
        assertNotNull("The container should bind startup bean InitNewTxBean1 during checkpoint at=deployment, but did not.",
                      server.waitForStringInLogUsingMark("CNTR0167I:.*InitNewTxBean1"));

        server.checkpointRestore();
    }

    // TODO: FEATURE SUPPORT NOT YET IMPLEMENTED
    // Modify this test either to verify checkpoint fails when the container begins
    // the tx during @PostConstruct or verify @PostConstruct executes during restore.
    /**
     * Verify checkpoint at=applications fails when a startup beans begin a user transaction
     * during application startup.
     */
    @Test
    public void testStartupBeanUserTranAtApplications() throws Exception {
        assertNotNull("The server should bind startup bean InitNewTxBean2 during checkpoint at=application, but did not.",
                      server.waitForStringInLogUsingMark("CNTR0167I:.*InitNewTxBean2"));

        server.checkpointRestore();
    }

    static enum TestMethod {
        testStartupBeanRequiresNewAtApplications,
        testStartupBeanRequiresNewAtDeployment,
        testStartupBeanUserTranAtApplications,
        unknown;
    }
}
