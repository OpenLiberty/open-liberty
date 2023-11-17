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
import static io.openliberty.checkpoint.fat.FATSuite.stopServer;
import static io.openliberty.checkpoint.fat.util.FATUtils.LOG_SEARCH_TIMEOUT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.fat.util.FATUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class Simple2PCCloudTest extends FATServletClient {

    public static final String APP_NAME = "transactioncloud";
    public static final String SERVLET_NAME = APP_NAME + "/Simple2PCCloudServlet";
    protected static final int cloud2ServerPort = 9992;

    @Server("checkpointTransactionCloud001")
    public static LibertyServer cloud1ServerInstantOn;

    @Server("checkpointTransactionCloud002")
    public static LibertyServer cloud2ServerInstantOn;

    @Server("checkpointTransactionCloud001")
    public static LibertyServer cloud1Server;

    @Server("checkpointTransactionLongLeaseServer")
    public static LibertyServer cloud1LongLeaseServer;

    TestMethod testMethod;

    // Potential datasource and transaction config overrides
    static final String HOSTNAME001 = "HOSTNAME001";
    static final String TX_RETRY_INT = "10";
    static final String TRANLOG_DS_JNDINAME = "jdbc/tranlogDataSource";
    static final int TRANLOG_DS_PORT = 1619;
    static final String APP_DS_JNDINAME = "jdbc/derby";

    @BeforeClass
    public static void setUpClass() throws Exception {
        cloud1ServerInstantOn.saveServerConfiguration();
        cloud2ServerInstantOn.saveServerConfiguration();
        cloud1Server.saveServerConfiguration();

        cloud1LongLeaseServer.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        cloud1LongLeaseServer.saveServerConfiguration();
        ShrinkHelper.defaultApp(cloud1LongLeaseServer, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.cloud.*");
    }

    @Before
    public void setupTest() throws Exception {
        cleanupSharedResources();
        ShrinkHelper.cleanAllExportedArchives();

        DerbyNetworkUtilities.startDerbyNetwork(TRANLOG_DS_PORT);

        testMethod = getTestMethod(TestMethod.class, testName);
        try {
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testLeaseTableAccess:
                case testDBBaseRecovery:
                    // Checkpoint server1 (cloud1)
                    cloud1ServerInstantOn.restoreServerConfiguration();
                    ShrinkHelper.defaultApp(cloud1ServerInstantOn, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.cloud.*");
                    Consumer<LibertyServer> preRestoreLogic1 = checkpointServer -> {
                        // Env vars that override the application datasource, tranlog datasource,
                        // and transaction configurations at restore
                        File serverEnvFile = new File(cloud1ServerInstantOn.getServerRoot() + "/server.env");
                        try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                            serverEnvWriter.println("HOSTNAME=" + HOSTNAME001);
                            serverEnvWriter.println("APP_DS_JNDINAME=" + APP_DS_JNDINAME);
                            serverEnvWriter.println("TRANLOG_DS_PORT=" + TRANLOG_DS_PORT);
                        } catch (FileNotFoundException e) {
                            throw new UncheckedIOException(e);
                        }
                    };
                    cloud1ServerInstantOn.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
                    cloud1ServerInstantOn.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic1);
                    cloud1ServerInstantOn.startServer();
                    break;
                case testDBRecoveryTakeover:
                case testDBRecoveryCompeteForLog:
                    // Normal server1 (cloud1)
                    cloud1Server.restoreServerConfiguration();
                    ShrinkHelper.defaultApp(cloud1Server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.cloud.*");
                    File serverEnvFile = new File(cloud1Server.getServerRoot() + "/server.env");
                    try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                        // Use the server's built-in HOSTNAME env var
                        //serverEnvWriter.println("HOSTNAME=" + HOSTNAME001);
                        serverEnvWriter.println("APP_DS_JNDINAME=" + APP_DS_JNDINAME);
                        serverEnvWriter.println("TRANLOG_DS_PORT=" + TRANLOG_DS_PORT);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                    cloud1Server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);

                    // Checkpoint server2 (cloud2)
                    cloud2ServerInstantOn.restoreServerConfiguration();
                    ShrinkHelper.defaultApp(cloud2ServerInstantOn, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, "servlets.cloud.*");
                    Consumer<LibertyServer> preRestoreLogic2 = checkpointServer -> {
                        // Env vars that override the application datasource and transaction
                        // configurations at restore
                        File serverEnvFile2 = new File(checkpointServer.getServerRoot() + "/server.env");
                        try (PrintWriter serverEnvWriter2 = new PrintWriter(new FileOutputStream(serverEnvFile2))) {
                            serverEnvWriter2.println("TRANLOG_DS_JNDINAME=" + TRANLOG_DS_JNDINAME);
                            serverEnvWriter2.println("TRANLOG_DS_PORT=" + TRANLOG_DS_PORT);
                            serverEnvWriter2.println("TX_RETRY_INT=" + TX_RETRY_INT);
                        } catch (FileNotFoundException e) {
                            throw new UncheckedIOException(e);
                        }
                    };
                    cloud2ServerInstantOn.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
                    cloud2ServerInstantOn.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic2);
                    cloud2ServerInstantOn.startServer();
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }
        } catch (Exception e) {
            throw new AssertionError("Unexpected error during setupTest.", e);
        }
    }

    /**
     * Cleanup the XA resources and the tranlog DB shared by test servers.
     *
     * @throws Exception whenever an exception occurs deleting a shared resource.
     */
    public void cleanupSharedResources() throws Exception {
        // XA resource files
        cloud1LongLeaseServer.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);
        // Tranlog DB
        cloud1LongLeaseServer.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }

    @After
    public void teardownTest() throws Exception {
        TestMethod testMethod = getTestMethod(TestMethod.class, testName);
        try {
            Log.info(getClass(), testName.getMethodName(), "Tearing down: " + testMethod);

            // Ensure no server is running in the event a test fails
            switch (testMethod) {
                case testLeaseTableAccess:
                case testDBBaseRecovery:
                    stopServer(cloud1ServerInstantOn);
                    DerbyNetworkUtilities.stopDerbyNetwork(TRANLOG_DS_PORT);
                    break;
                case testDBRecoveryTakeover:
                    stopServer(cloud1Server);
                    stopServer(cloud2ServerInstantOn);
                    DerbyNetworkUtilities.stopDerbyNetwork(TRANLOG_DS_PORT);
                    break;
                case testDBRecoveryCompeteForLog:
                    stopServer(cloud1Server);
                    stopServer(cloud1LongLeaseServer);
                    stopServer(cloud2ServerInstantOn);
                    DerbyNetworkUtilities.stopDerbyNetwork(TRANLOG_DS_PORT);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            throw new AssertionError("Unexpected error during teardownTest.", e);
        }
    }

    /**
     * Test access to the Lease table.
     *
     * This is a readiness check to verify that resources are available and accessible.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    public void testLeaseTableAccess() throws Exception {
        final String method = "testLeaseTableAccess";
        StringBuilder sb = null;
        String id = "001";

        cloud1ServerInstantOn.checkpointRestore();

        try {
            sb = runTestWithResponse(cloud1ServerInstantOn, SERVLET_NAME, "testLeaseTableAccess");
        } finally {
            Log.info(this.getClass(), method, "testLeaseTableAccess" + id + " returned: " + sb);

            // "CWWKE0701E" error message is allowed
            FATUtils.stopServers(new String[] { "CWWKE0701E" }, cloud1ServerInstantOn);
        }
    }

    /**
     * The purpose of this test is as a control to verify that single server recovery is working.
     *
     * The Cloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     * Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testDBBaseRecovery() throws Exception {
        final String method = "testDBBaseRecovery";
        StringBuilder sb = null;
        String id = "001";

        cloud1ServerInstantOn.checkpointRestore();

        try {
            // We expect this to fail since it is gonna crash the server
            sb = runTestWithResponse(cloud1ServerInstantOn, SERVLET_NAME, "setupRec" + id);
        } catch (Exception expected) {
        } finally {
            Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);
        }
        cloud1ServerInstantOn.waitForStringInLog(XAResourceImpl.DUMP_STATE);

        try {
            // Re-restore cloud1 and recover indoubt tx
            ProgramOutput po = cloud1ServerInstantOn.checkpointRestore(); //.startServerAndValidate(false, true, true);
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(this.getClass(), "recoveryTest", ex);
                throw ex;
            }

            // Server appears to have started ok. Check for key string to see whether recovery has succeeded
            cloud1ServerInstantOn.waitForStringInTrace("Performed recovery for cloud001");
        } finally {
            // "WTRN0075W", "WTRN0076W", "CWWKE0701E" error messages are expected/allowed
            FATUtils.stopServers(new String[] { "WTRN0075W", "WTRN0076W", "CWWKE0701E" }, cloud1ServerInstantOn);
        }
    }

    /**
     * The purpose of this test is to verify simple peer transaction recovery.
     *
     * The Cloud001 server is started and halted by a servlet that leaves an indoubt transaction.
     * Cloud002, a peer server as it belongs to the same recovery group is started and recovers the
     * transaction that belongs to Cloud001.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    @AllowedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException" })
    public void testDBRecoveryTakeover() throws Exception {
        final String method = "testDBRecoveryTakeover";
        StringBuilder sb = null;
        String id = "001";

        // Start cloud1, normal server startup
        cloud1Server.startServer();

        try {
            // We expect this to fail since it is gonna crash server3
            sb = runTestWithResponse(cloud1Server, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable expected) {
        } finally {
            Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);
        }
        cloud1Server.waitForStringInLog(XAResourceImpl.DUMP_STATE);

        try {
            // Restore cloud2 from checkpoint image
            cloud2ServerInstantOn.setHttpDefaultPort(cloud2ServerPort);
            ProgramOutput po = cloud2ServerInstantOn.checkpointRestore(); // .startServerAndValidate(false, true, true);
            final int status = po.getReturnCode();
            // This test has shown that 22 is success sometimes
            if (status != 0 && status != 22) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(this.getClass(), "recoveryTest", ex);
                throw ex;
            }

            // Server appears to have started ok. Check for key string to see whether peer recovery has succeeded
            cloud2ServerInstantOn.waitForStringInTrace("Performed recovery for cloud001");
        } finally {
            // "CWWKE0701E" error message is allowed
            FATUtils.stopServers(new String[] { "CWWKE0701E" }, cloud2ServerInstantOn);
        }
    }

    /**
     * The purpose of this test is to verify correct behaviour when peer servers compete for a log.
     *
     * The Cloud001 server is started and a servlet invoked. The servlet modifies the owner of the server's
     * lease recored in the lease table. This simulates the situation where a peer server has acquired the
     * ownership of the lease and is recovering Cloud001's logs. Finally the servlet halts the server leaving
     * an indoubt transaction.
     *
     * Cloud001 is restarted but should fail to acquire the lease to its recovery logs as it is no longer the owner.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC(value = { "com.ibm.ws.recoverylog.spi.RecoveryFailedException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException",
                            "javax.resource.spi.ResourceAllocationException" })
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "com.ibm.tx.jta.XAResourceNotAvailableException", "com.ibm.ws.recoverylog.spi.RecoveryFailedException",
                           "java.lang.IllegalStateException" })
    // If cloud002 starts slowly, then access to cloud001's indoubt tx XAResources
    // may need to be retried (tx recovery is, in such cases, working as designed.)
    public void testDBRecoveryCompeteForLog() throws Exception {
        final String method = "testDBRecoveryCompeteForLog";
        StringBuilder sb = null;
        String id = "001";

        // Normal start of cloud1 w/o the ${HOSTNAME} prefix in recovery ID.
        // The HOSTNAME prefix is intended for use with Kubernetes and is not necessary
        // to verify peer recovery behavior.  Leaving the HOSTNAME in the recovery ID
        // requires test servlet method "modifyLeaseOwnder" to also prepend the HOSTNAME
        // to the name of the tranlog DB's lease table owner.
        ServerConfiguration config = cloud1Server.getServerConfiguration();
        Transaction transaction = config.getTransaction();
        transaction.setRecoveryIdentity("cloud001-1"); // Remove ${HOSTNAME} prefix
        cloud1Server.updateServerConfiguration(config);
        cloud1Server.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        cloud1Server.startServer();

        try {
            sb = runTestWithResponse(cloud1Server, SERVLET_NAME, "modifyLeaseOwner");

            // We expect this to fail since it crashes the server
            sb = runTestWithResponse(cloud1Server, SERVLET_NAME, "setupRec" + id);
        } catch (Throwable expected) {
        } finally {
            Log.info(this.getClass(), method, "setupRec" + id + " returned: " + sb);
        }
        cloud1Server.waitForStringInLog(XAResourceImpl.DUMP_STATE);

        // Pull in a new server.xml file that ensures that we have a long (5 minute) timeout
        // for the lease, otherwise we may decide that we CAN delete and renew our own lease.

        // Now restart cloud1; we expect the server to fail startup
        try {
            cloud1LongLeaseServer.startServerExpectFailure("recovery-dblog-fail.log", false, true);
        } catch (Exception ex) {
            // Tolerate an exception here, as recovery is asynch and the "successful start" message
            // may have been produced by the main thread before the recovery thread had completed
            Log.info(this.getClass(), method, "startServerExpectFailure threw exc: " + ex);
        }

        // Server appears to have failed as expected. Check for log failure string
        if (cloud1LongLeaseServer.waitForStringInLog("RECOVERY_LOG_FAILED") == null) {
            Exception ex = new Exception("Recovery logs should have failed");
            Log.error(this.getClass(), "recoveryTestCompeteForLock", ex);
            throw ex;
        }

        try {
            // defect 210055: Now start cloud2 so that we can tidy up the environment, otherwise cloud1
            // is unstartable because its lease is owned by cloud2.
            cloud2ServerInstantOn.setHttpDefaultPort(cloud2ServerPort);
            ProgramOutput po = cloud2ServerInstantOn.checkpointRestore(); // .startServerAndValidate(false, true, true);
            if (po.getReturnCode() != 0) {
                Log.info(this.getClass(), method, po.getCommand() + " returned " + po.getReturnCode());
                Log.info(this.getClass(), method, "Stdout: " + po.getStdout());
                Log.info(this.getClass(), method, "Stderr: " + po.getStderr());
                Exception ex = new Exception("Could not restart the server");
                Log.error(this.getClass(), "recoveryTest", ex);
                throw ex;
            }

            // Server appears to have started ok. Check for 2 key strings to see whether peer recovery has succeeded
            cloud2ServerInstantOn.waitForStringInTrace("Performed recovery for cloud001");
        } finally {
            // "CWWKE0701E" error message is allowed
            FATUtils.stopServers(new String[] { "CWWKE0701E" }, cloud2ServerInstantOn);
        }
    }

    static enum TestMethod {
        testLeaseTableAccess,
        testDBBaseRecovery,
        testDBRecoveryTakeover,
        testDBRecoveryCompeteForLog,
        unknown
    }
}
