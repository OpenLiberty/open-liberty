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
package com.ibm.ws.jdbc.fat.tests;

import static com.ibm.websphere.simplicity.config.DataSourceProperties.DERBY_EMBEDDED;
import static componenttest.annotation.SkipIfSysProp.DB_Oracle;
import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.dsprops.testrules.OnlyIfDataSourceProperties;
import com.ibm.websphere.simplicity.config.dsprops.testrules.SkipIfDataSourceProperties;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class DataSourceTest extends FATServletClient {

    //App names
    private static final String setupfat = "setupfat";
    private static final String basicfat = "basicfat";
    private static final String dsdfat = "dsdfat";
    private static final String dsdfat_global_lib = "dsdfat_global_lib";

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    //Server used for ConfigTest.java and DataSourceTest.java
    @Server("com.ibm.ws.jdbc.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        // Delete the Derby database that might be left over from last run
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/jdbcfat");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/derbyfat");

        //Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        server.addEnvVar("DB_DRIVER", type.getDriverName());
        server.addEnvVar("ANON_DRIVER", type.getAnonymousDriverName());
        server.addEnvVar("DB_USER", testContainer.getUsername());
        server.addEnvVar("DB_PASSWORD", testContainer.getPassword());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        //**** jdbcServer apps ****
        // Dropin app - setupfat.war
        ShrinkHelper.defaultDropinApp(server, setupfat, "setupfat");

        // Default app - dsdfat.war and dsdfat_global_lib.war
        ShrinkHelper.defaultApp(server, dsdfat, dsdfat);
        ShrinkHelper.defaultApp(server, dsdfat_global_lib, dsdfat_global_lib);

        // Default app - jdbcapp.ear [basicfat.war, application.xml]
        WebArchive basicfatWAR = ShrinkHelper.buildDefaultApp(basicfat, basicfat);
        EnterpriseArchive jdbcappEAR = ShrinkWrap.create(EnterpriseArchive.class, "jdbcapp.ear");
        jdbcappEAR.addAsModule(basicfatWAR);
        ShrinkHelper.addDirectory(jdbcappEAR, "test-applications/jdbcapp/resources");
        ShrinkHelper.exportAppToServer(server, jdbcappEAR);

        //Start Server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("J2CA0045E.*dsfat2", // expected by testMaxPoolSize1 and testMaxPoolSize2
                          "J2CA0021E.*dsfat5", // expected by testMinPoolSize
                          "J2CA0045E.*dsfat8", // expected by testDuplicateJNDINames
                          "DSRA030(2|4)E", // expected by testXARecovery
                          "WTRN0048W", // expected by testXARecovery
                          "WTRN0062E", //expected by testEnableSharingForDirectLookupsFalse
                          "J2CA0030E", //expected by testEnableSharingForDirectLookupsFalse
                          "CWWKE0701E"); //expected by testReapTimeUnsupportedValue
    }

    /**
     * Runs the test in the "basicfat" app
     */
    private void runTest() throws Exception {
        runTest(server, basicfat, testName);
    }

    @Test
    public void testServletWorking() throws Exception {
        runTest(server, setupfat, testName);
    }

    @Test
    @SkipIfDataSourceProperties(DERBY_EMBEDDED)
    public void testBootstrapDatabaseConnection() throws Throwable {
        runTest(server, setupfat, testName);
    }

    @Test
    public void testBasicQuery() throws Exception {
        runTest();
    }

    @Test
    public void testBatchUpdates() throws Exception {
        runTest();
    }

    @Test
    public void testConnectionManagerWithDefaultConfig() throws Exception {
        List<String> results = server.findStringsInLogs("CWWKG0033W");
        if (results.size() > 0)
            fail("Unexpected warnings in logs: " + results);
    }

    @Test
    public void testDataSourceDefinition() throws Exception {
        runTest();
    }

    @Test
    public void testDataSourceDefinitions() throws Exception {
        runTest();
    }

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException" })
    public void testDuplicateJNDINames() throws Exception {
        runTest(server, dsdfat, testName);
    }

    @Test
    @ExpectedFFDC({ "javax.resource.ResourceException", "java.lang.IllegalStateException", "java.sql.SQLException" })
    public void testEnableSharingForDirectLookupsFalse() throws Exception {
        runTest();
    }

    @Test
    public void testConnectionCleanup() throws Exception {
        runTest();
    }

    @Test
    public void testImplicitlyCloseChildren() throws Exception {
        runTest();
    }

    @Test
    public void testIsolatedSharedLibraries() throws Exception {
        runTest();
    }

    @Test
    @SkipIfSysProp({ DB_SQLServer }) // TODO
    // This test does not work for SQLServer yet.  It might be a problem with the Docker version of SQLServer
    // So we have opened up an issue with Microsoft to investigate further -> https://github.com/microsoft/mssql-docker/issues/554
    public void testLastParticipant() throws Exception {
        runTest();
    }

    @Test
    public void testMatchCurrentState() throws Exception {
        runTest();
    }

    @Test
    public void testMatchOriginalRequest() throws Exception {
        runTest();
    }

    @Test
    @ExpectedFFDC({ "com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException" })
    public void testMaxPoolSize2() throws Exception {
        runTest();
    }

    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC({ "javax.resource.ResourceException" })
    public void testMinPoolSize() throws Exception {
        runTest();
    }

    @Test
    public void testNonTransactional() throws Exception {
        runTest();
    }

    @Test
    public void testNonTransactionalCleanup() throws Exception {
        runTest();
    }

    @Test
    public void testNonTransactionalMultipleConnections() throws Exception {
        runTest();
    }

    @Test
    public void testOnePhaseOptimization() throws Exception {
        runTest();
    }

    @Test
    public void testQueryTimeout() throws Exception {
        runTest();
    }

    @Test
    public void testResultSetMetaData() throws Exception {
        runTest();
    }

    @Test
    public void testSerialization() throws Exception {
        runTest();
    }

    @Test
    public void testSerialReuseInGlobalTran() throws Exception {
        runTest();
    }

    @Test
    public void testSerialReuseInLTC() throws Exception {
        runTest();
    }

    @Test
    public void testSharableHandleReassociation() throws Exception {
        runTest();
    }

    @Test
    public void testSharingInGlobalTran() throws Exception {
        runTest();
    }

    @Test
    public void testStatementCleanup() throws Exception {
        runTest();
    }

    @Test
    public void testStatementsAcrossTranBoundaries() throws Exception {
        runTest();
    }

    @Test
    @SkipIfSysProp({ DB_SQLServer }) //TODO figure out why test stalls using SQLServer
    // This test does not work for SQLServer yet.  It might be a problem with the Docker version of SQLServer
    // So we have opened up an issue with Microsoft to investigate further -> https://github.com/microsoft/mssql-docker/issues/554
    public void testTwoPhaseCommit() throws Exception {
        runTest();
    }

    @Test
    public void testTwoTransactions() throws Exception {
        runTest();
    }

    @Test
    public void testUnsharable() throws Exception {
        runTest();
    }

    @Test
    public void testUpdatableResult() throws Exception {
        runTest();
    }

    /**
     * Test validationTimeout setting on an annotation-specified data source
     */
    @Test
    public void testValTimeoutAnnotation() throws Throwable {
        runTest();
    }

    @Test
    public void testWrapperPattern() throws Exception {
        runTest();
    }

    @Test
    @AllowedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.transaction.xa.XAException" })
    @SkipIfSysProp(DB_Oracle)
    public void testXARecovery() throws Exception {
        runTest();
    }

    @Test
    @ExpectedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.transaction.xa.XAException" })
    public void testXARecoveryContainerAuth() throws Exception {
        runTest();
    }

    @Test
    public void testXAWithMultipleDatabases() throws Exception {
        runTest();
    }

    @Test
    @OnlyIfDataSourceProperties(DERBY_EMBEDDED)
    public void testDataSourceDefGlobalLib() throws Exception {
        runTest(server, dsdfat_global_lib, testName);
    }

    /**
     * Test for PI23168 - two data sources with nested libraries with no IDs.
     */
    @Test
    public void testTwoNestedLibrariesWithNoIds() throws Exception {
        if (new File(server.getServerRoot() + "/db2/db2jcc4.jar").exists()) {
            runTest();
        } else {
            System.out.println("Skipping test because there is no db2 jar.");
        }
    }

    /**
     * Test if a reap time of 0 is still unsupported.
     */
    @Test
    @AllowedFFDC({ "java.security.PrivilegedActionException", "javax.resource.ResourceException" })
    public void testReapTimeUnsupportedValue() throws Exception {
        runTest();
    }

    /**
     * Test if a aged timeout value of 0 causes pooling to be disabled.
     */
    @Test
    public void testAgedTimeoutImmediate() throws Exception {
        runTest();
    }

    /**
     * Test if a maxIdleTime of 0 is still unsupported.
     */
    @Test
    @AllowedFFDC({ "java.security.PrivilegedActionException", "javax.resource.ResourceException" })
    public void testMaxIdleTimeUnsupportedValue() throws Exception {
        runTest();
    }

    /**
     * Test that when connectionTimeout is set to a value of -1 (infinite) the connection
     * request waits for longer than the default timeout (30 seconds)
     */
    @Test
    @Mode(TestMode.FULL)
    public void testConnectionTimeoutInfinite() throws Exception {
        runTest();
    }

    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC({ "javax.resource.spi.ResourceAllocationException" })
    public void testInterruptedWaiters() throws Exception {
        runTest();
    }
}