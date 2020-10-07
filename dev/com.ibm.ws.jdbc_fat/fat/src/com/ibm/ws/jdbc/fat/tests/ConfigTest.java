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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ConnectionManager;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.File;
import com.ibm.websphere.simplicity.config.Fileset;
import com.ibm.websphere.simplicity.config.JdbcDriver;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.config.dsprops.Properties_db2_jcc;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_client;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
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
public class ConfigTest extends FATServletClient {
    private static final Class<?> c = ConfigTest.class;

    //App names
    private static final String setupfat = "setupfat";
    private static final String basicfat = "basicfat";
    private static final String jdbcapp = "jdbcapp";
    private static final String dsdfat = "dsdfat";
    private static final String dsdfat_global_lib = "dsdfat_global_lib";

    //Server used for ConfigTest.java and DataSourceTest.java
    @Server("com.ibm.ws.jdbc.fat")
    public static LibertyServer server;

    //Test container
    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    //List of apps tested by this test suite
    private static final Set<String> appNames = new HashSet<String>(Arrays.asList(dsdfat, jdbcapp));

    //Lists of allowable exceptions
    private static final String[] EMPTY_EXPR_LIST = new String[0];
    private static final String[] JDBCAPP_RECYCLE_EXPR_LIST = new String[] {
                                                                             "CWWKZ0009I.*" + jdbcapp,
                                                                             "CWWKZ0003I.*" + jdbcapp
    };
    private static final String[] JDBCAPP_AND_DSDFAT_RECYCLE_EXPR_LIST = new String[] {
                                                                                        "CWWKZ0009I.*" + jdbcapp,
                                                                                        "CWWKZ0003I.*" + jdbcapp,
                                                                                        "CWWKZ0009I.*" + dsdfat,
                                                                                        "CWWKZ0003I.*" + dsdfat
    };
    private static final String[] ALLOWED_MESSAGES = { "J2CA0045E",
                                                       "CWWKE0701E", // expected by testOnError
                                                       "DSRA8100E.*XJ004", // expected because we dropped the Derby database
                                                       "DSRA8020E.*badVendorProperty",
                                                       "J2CA0021E.*dsValTderby",
                                                       "CWWKG0033W.*(conMgr1|Derby)",
                                                       "J2CA8040E.*conMgr5" };
    private static String[] cleanUpExprs = EMPTY_EXPR_LIST;

    //Server configurations that will be changed during test suite, but that we will go back to later.
    private static ServerConfiguration originalServerConfig;
    private static ServerConfiguration originalServerConfigUpdatedForJDBC;

    @BeforeClass
    public static void setUp() throws Exception {
        // Delete the Derby database that might be left over from last run
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/jdbcfat");
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/data/derbyfat");

        // Get original server config
        originalServerConfig = server.getServerConfiguration().clone();

        //Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        server.addEnvVar("DB_DRIVER", type.getDriverName());
        server.addEnvVar("ANON_DRIVER", type.getAnonymousDriverName());
        server.addEnvVar("DB_USER", testContainer.getUsername());
        server.addEnvVar("DB_PASSWORD", testContainer.getPassword());

        //Setup server DataSource properties (use database specific properties in order to run testTrace() )
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, testContainer);

        // Get JDBC server config
        originalServerConfigUpdatedForJDBC = server.getServerConfiguration().clone();

        // Start the server with JDBC-4.1 but without any connection managers, data sources, or JDBC drivers
        ServerConfiguration config = server.getServerConfiguration();
        config.getConnectionManagers().clear();
        config.getDataSources().clear();
        config.getJdbcDrivers().clear();
        server.updateServerConfiguration(config);

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

        //Start server
        server.startServer();

        //Assure features and server are started.
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));

        //Update server config
        updateServerConfig(originalServerConfigUpdatedForJDBC, cleanUpExprs);
    }

    @After
    public void cleanUpPerTest() throws Exception {
        server.setLogOnUpdate(false); //Reduce output that is not specific to the actual tests being run
        updateServerConfig(originalServerConfigUpdatedForJDBC, cleanUpExprs);
        server.setLogOnUpdate(true);
        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(ALLOWED_MESSAGES);

        // Restore the original configuration
        server.updateServerConfiguration(originalServerConfig);
    }

    private static void updateServerConfig(ServerConfiguration config, String[] cleanup) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(appNames, cleanup);
    }

    private void runTest(String app, String test) throws Throwable {
        runTest(server, app, test);
    }

    /**
     * Remove the connectionManager that a datasource is using. Save the config.
     * Then add it back and save the config again while the server is running.
     */
    @Test
    public void testConfigChangeAddConnectionManager() throws Throwable {
        String method = "testConfigChangeAddConnectionManager";
        Log.info(c, method, "Executing " + method);

        // Remove a connectionManager
        ServerConfiguration config = server.getServerConfiguration();
        ConnectionManager conMgr1 = config.getConnectionManagers().getBy("id", "conMgr1");
        config.removeConnectionManagerById("conMgr1");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Add the connectionManager back
        config.getConnectionManagers().add(conMgr1);

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Data source with this connectionManager should be usable again.
            runTest(basicfat + '/', "testBasicQuery");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Update an authData element while the server is running.
     * Verify that we are using the updated value.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeAuthData() throws Throwable {
        String method = "testConfigChangeAuthData";
        Log.info(c, method, "Executing " + method);

        // First use the authData with its current value
        runTest(basicfat, "testConfigChangeAuthDataOriginalValue");

        // Find the derbyAuth1 element
        ServerConfiguration config = server.getServerConfiguration();
        AuthData derbyAuth1 = null;
        for (AuthData authData : config.getAuthDataElements())
            if ("derbyAuth1".equals(authData.getId()))
                derbyAuth1 = authData;
        // Fail if we cannot find it
        if (derbyAuth1 == null) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            fail("Did not find authData with id=derbyAuth1");
        }
        // Save the original username
        String originalUserName = derbyAuth1.getUser();

        // Update to a new username
        derbyAuth1.setUser("updatedUserName");

        try {
            // Update and use the authData with its new value
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeAuthData");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Restore the old value
        derbyAuth1.setUser(originalUserName);

        try {
            // Update and use the authData with its restored value
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeAuthDataOriginalValue");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Update the data source configuration
     * from commitOrRollbackOnCleanup unspecified,
     * to commitOrRollbackOnCleanup=commit
     * to commitOrRollbackOnCleanup=rollback
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeCommitOrRollbackOnCleanup() throws Throwable {
        String method = "testConfigCommitOrRollbackOnCleanup";
        Log.info(c, method, "Executing " + method);

        // Use the data source with its default behavior, which for transactional=false,
        // should be rollback on cleanup.
        runTest(basicfat, "testConfigChangeRollbackOnCleanup");

        // Change commitOrRollbackOnCleanup to commit
        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsfat3 = config.getDataSources().getBy("id", "dsfat3");
        dsfat3.setCommitOrRollbackOnCleanup("commit");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Behavior should now reflect the new setting of commit
            runTest(basicfat, "testConfigChangeCommitOnCleanup");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Change commitOrRollbackOnCleanup to rollback
        dsfat3.setCommitOrRollbackOnCleanup("rollback");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Behavior should now reflect the new setting of commit
            runTest(basicfat, "testConfigChangeRollbackOnCleanup");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Update the connectionManager configuration from maxPoolSize=2 to maxPoolSize=1
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException" })
    public void testConfigChangeConnectionManager() throws Throwable {
        String method = "testConfigChangeConnectionManager";
        Log.info(c, method, "Executing " + method);

        // Use the data source before making the dynamic update
        runTest(basicfat, "testMaxPoolSize2");

        // Change maxPoolSize to 1
        ServerConfiguration config = server.getServerConfiguration();
        ConnectionManager conMgr2 = config.getConnectionManagers().getBy("id", "conMgr2");
        conMgr2.setMaxPoolSize("1");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Behavior should now reflect the new setting of 1 for maxPoolSize
            runTest(basicfat, "testMaxPoolSize1");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        runTest(basicfat, "setServletInstanceStillActive");

        // Increase maxPoolSize to 2
        conMgr2.setMaxPoolSize("2");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Behavior should now reflect the new setting of 2 for maxPoolSize
            runTest(basicfat, "testMaxPoolSize2");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        runTest(basicfat, "requireServletInstanceStillActive");

        runTest(basicfat, "resetState");

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Update the data source configuration while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeDataSource() throws Throwable {
        String method = "testConfigChangeDataSource";
        Log.info(c, method, "Executing " + method);

        // use it first
        runTest(basicfat, "testConfigChangeDataSourceOriginalConfig");

        // change various attributes data source
        ServerConfiguration config = server.getServerConfiguration();
        ServerConfiguration originalConfig = config.clone();
        DataSource dsfat5 = config.getDataSources().getBy("id", "dsfat5derby");
        dsfat5.setBeginTranForResultSetScrollingAPIs("false");
        dsfat5.setBeginTranForVendorAPIs("false");
        dsfat5.setConnectionSharing("MatchCurrentState");
        dsfat5.setIsolationLevel("TRANSACTION_READ_UNCOMMITTED");
        dsfat5.setQueryTimeout("10");
        dsfat5.setStatementCacheSize("2");
        dsfat5.setSyncQueryTimeoutWithTransactionTimeout("false");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeDataSourceModified");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // restore the original settings
        dsfat5.setBeginTranForResultSetScrollingAPIs("true");
        dsfat5.setBeginTranForVendorAPIs("true");
        dsfat5.setConnectionSharing("MatchOriginalRequest");
        dsfat5.setIsolationLevel(null);
        dsfat5.setQueryTimeout("30");
        dsfat5.setSyncQueryTimeoutWithTransactionTimeout("true");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeDataSourceOriginalConfig");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(originalConfig);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    @Test
    public void testConfigChangeEnableConnectionCasting() throws Throwable {
        String method = "testConfigChangeEnableConnectionCasting";
        Log.info(c, method, "Executing " + method);

        // Use the data source with its default behavior, which is that connection casting is disabled
        runTest(basicfat, "testConfigChangeConnectionCastingDisabled");

        // Change enableConnectionCasting to true
        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsfat10derby = config.getDataSources().getBy("id", "dsfat10derby");
        dsfat10derby.setEnableConnectionCasting("true");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Behavior should now reflect the new setting of true
            runTest(basicfat, "testConfigChangeConnectionCastingEnabled");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Change enableConnectionCasting to false
        dsfat10derby.setEnableConnectionCasting("false");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Behavior should now reflect the new setting of false
            runTest(basicfat, "testConfigChangeConnectionCastingDisabled");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Update the fileset configuration to add an extra JAR
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeFileset() throws Throwable {
        String method = "testConfigChangeFileset";
        Log.info(c, method, "Executing " + method);

        // Use the data source before making the dynamic update
        runTest(basicfat, "testBasicQuery");

        // add a duplicate jar to the fileset
        ServerConfiguration config = server.getServerConfiguration();
        JdbcDriver FATJDBCDriver = config.getJdbcDrivers().getBy("id", "FATJDBCDriver");
        Library FATJDBCDriver_library = FATJDBCDriver.getNestedLibrary();
        Fileset FATJDBCDriver_library_fileset = FATJDBCDriver_library.getNestedFileset();
        String includes = FATJDBCDriver_library_fileset.getIncludes();
        FATJDBCDriver_library_fileset.setIncludes(includes + ' ' + includes);

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testBasicQuery");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Update the fileset configuration to switch between bad and good values for the dir attribute
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "java.lang.ClassNotFoundException", "java.sql.SQLNonTransientException" })
    public void testConfigChangeFilesetDir() throws Throwable {
        String method = "testConfigChangeFilesetDir";
        Log.info(c, method, "Executing " + method);

        // create a library with a fileset with a bad dir attribute
        ServerConfiguration config = server.getServerConfiguration();
        Fileset DerbFileset = new Fileset();
        DerbFileset.setDir("${shared.resource.dir}/derb");
        DerbFileset.setIncludes("derby.jar");
        Library DerbLib = new Library();
        DerbLib.setId("DerbLib");
        DerbLib.setNestedFileset(DerbFileset);
        config.getLibraries().add(DerbLib);

        // create a jdbcDriver to use this bad library/fileset
        JdbcDriver Derb = new JdbcDriver();
        Derb.setId("Derb");
        Derb.setLibraryRef("DerbLib");
        config.getJdbcDrivers().add(Derb);

        // create a new data source to use this jdbcDriver
        DataSource dsfat15 = new DataSource();
        dsfat15.setJndiName("jdbc/dsfat15");
        dsfat15.setJdbcDriverRef("Derb");
        // need to use a different database because Derby Embedded cannot access the same database from different class loaders
        Properties_derby_embedded propertiesDerbyEmbedded = new Properties_derby_embedded();
        propertiesDerbyEmbedded.setCreateDatabase("create");
        propertiesDerbyEmbedded.setDatabaseName("memory:derb");
        dsfat15.getProperties_derby_embedded().add(propertiesDerbyEmbedded);
        config.getDataSources().add(dsfat15);

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeFilesetBad");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Fix the fileset dir to point to a valid location
        DerbFileset.setDir("${shared.resource.dir}/derby");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeFilesetGood");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Make it bad again
        DerbFileset.setDir("${shared.resource.dir}/derbyNotFound");

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testConfigChangeFilesetBad");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Add a scan interval
        DerbFileset.setScanInterval("1s");
        DerbFileset.setDir("${shared.resource.dir}/derbII");
        DerbLib.setId("DerbIILib");
        Derb.setLibraryRef("DerbIILib");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeFilesetBad");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Correct the configuration and verify it works
        DerbFileset.setDir("${shared.resource.dir}/derby");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Delay for over a second (with some buffer) to allow the scanInterval to make the update
            Thread.sleep(3000);
            runTest(basicfat, "testConfigChangeFilesetGood");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Make it bad one last time
        DerbFileset.setDir("${shared.resource.dir}/derbyNotFound");

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testConfigChangeFilesetBad");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Update the jdbcDriver configuration to change the XADataSource impl name
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeJDBCDriver() throws Throwable {
        String method = "testConfigChangeJDBCDriver";
        Log.info(c, method, "Executing " + method);

        // Use the data source before making the dynamic update
        runTest(basicfat, "testDerbyJDBCDriver");

        // change the XA data source implementation class name
        ServerConfiguration config = server.getServerConfiguration();
        JdbcDriver driver = config.getJdbcDrivers().getBy("id", "Derby");
        driver.setJavaxSqlXADataSource("org.apache.derby.jdbc.EmbeddedXADataSource");

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testDerbyJDBCDriver");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Add loginTimeout=320 to the nested vendor properties configuration (jdbc/dsfat2)
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeLoginTimeout320() throws Throwable {
        String method = "testConfigChangeLoginTimeout320";
        Log.info(c, method, "Executing " + method);

        // Use the data source before making the dynamic update
        runTest(basicfat, "testBasicQuery");

        // set loginTimeout to 320 for dsfat2
        ServerConfiguration config = server.getServerConfiguration();
        DataSourceProperties dsfat2Props = config.getDataSources().getBy("id", "dsfat2").getDataSourceProperties().iterator().next();
        dsfat2Props.setLoginTimeout("320");

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testConfigChangeLoginTimeout320");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Add loginTimeout=550 to the top level vendor properties configuration
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeLoginTimeout550() throws Throwable {
        String method = "testConfigChangeLoginTimeout550";
        Log.info(c, method, "Executing " + method);

        // Use the data source before making the dynamic update
        runTest(basicfat, "testOnePhaseOptimization");

        // set loginTimeout to 550
        // TODO: this is supposed to be testing a config change to top level properties, but we had to remove that for the initial release.
        // Hopefully we can add it back in the future, and then we should switch this test case back to what it did before
        ServerConfiguration config = server.getServerConfiguration();
        DataSourceProperties dsfat2Props = config.getDataSources().getBy("id", "dsfat2").getDataSourceProperties().iterator().next();
        dsfat2Props.setLoginTimeout("550");

        DataSourceProperties dsfat3Props = config.getDataSources().getBy("id", "dsfat3").getDataSourceProperties().iterator().next();
        dsfat3Props.setLoginTimeout("550");

        DataSourceProperties dsfat4Props = config.getDataSources().getBy("jndiName", "jdbc/dsfat4").getDataSourceProperties().iterator().next();
        dsfat4Props.setLoginTimeout("550");

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testConfigChangeLoginTimeout550");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Move a connectionManager from top level config to nested config.
     * Modify it before and after using it.
     * Move it back to top level config, and use it again.
     *
     * @throws Throwable if it fails.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.ce.j2c.ConnectionWaitTimeoutException" })
    public void testConfigChangeNestedConnectionManager() throws Throwable {
        String method = "testConfigChangeNestedConnectionManager";
        Log.info(c, method, "Executing " + method);

        // Move a connectionManager from top level config to nested config
        ServerConfiguration config = server.getServerConfiguration();
        ConnectionManager conMgr2 = config.getConnectionManagers().getBy("id", "conMgr2");
        config.removeConnectionManagerById("conMgr2");
        conMgr2.setId(null);
        DataSource dsfat2 = config.getDataSources().getBy("id", "dsfat2");
        dsfat2.setConnectionManagerRef(null);
        dsfat2.getConnectionManagers().add(conMgr2);

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Modify the nested connectionManager before it gets used.
        conMgr2.setMaxPoolSize("1");
        conMgr2.setPurgePolicy("FailingConnectionOnly"); // just to change the file size

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Verify that dataSource is usable and its connectionManager has maxPoolSize=1
            runTest(basicfat, "testMaxPoolSize1");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        runTest(basicfat, "setServletInstanceStillActive");

        // Modify the nested config after it has been used
        conMgr2.setMaxPoolSize("2");
        conMgr2.setPurgePolicy("ValidateAllConnections"); // just to change the file size

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Verify that dataSource is usable and its connectionManager has maxPoolSize=2
            runTest(basicfat, "testMaxPoolSize2");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        runTest(basicfat, "requireServletInstanceStillActive");
        runTest(basicfat, "resetState");

        // Move the connectionManager back to top level config.
        dsfat2.setConnectionManagerRef("conMgr2");
        dsfat2.getConnectionManagers().clear();
        conMgr2.setId("conMgr2");
        config.getConnectionManagers().add(conMgr2);

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Verify that dataSource is usable and its connectionManager has maxPoolSize=2
            runTest(basicfat, "testMaxPoolSize2");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Move a jdbcDriver from top level config to nested config.
     * Modify it before and after using it.
     * Move it back to top level config, and use it again.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testConfigChangeNestedJDBCDriver() throws Throwable {
        String method = "testConfigChangeNestedJDBCDriver";
        Log.info(c, method, "Executing " + method);

        // Use the data source before making the dynamic update
        runTest(basicfat, "testBasicQuery");

        // Move a jdbcDriver from top level config to nested config
        ServerConfiguration config = server.getServerConfiguration();
        JdbcDriver Derby = config.getJdbcDrivers().getBy("id", "Derby");
        config.removeJdbcDriverById("Derby");
        Derby.setId(null);
        DataSource dsfat5 = config.getDataSources().getBy("id", "dsfat5derby");
        dsfat5.setJdbcDriverRef(null);
        dsfat5.getJdbcDrivers().add(Derby);
        DataSource dsfat10 = config.getDataSources().getBy("id", "dsfat10derby");
        dsfat10.setJdbcDriverRef(null);
        dsfat10.getJdbcDrivers().add(Derby);

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Modify the nested jdbcDriver before it gets used.
        Derby.setJavaxSqlConnectionPoolDataSource("org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource");
        Derby.setJavaxSqlXADataSource("org.apache.derby.jdbc.EmbeddedXADataSource");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Verify that dataSource is usable
            runTest(basicfat, "testIsolatedSharedLibraries");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Modify the nested config after it has been used
        Derby.setJavaxSqlConnectionPoolDataSource(null);

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            // Verify that dataSource is usable and its connectionManager has maxPoolSize=2
            runTest(basicfat, "testIsolatedSharedLibraries");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Move the jdbcDriver back to top level config.
        dsfat5.setJdbcDriverRef("Derby");
        dsfat5.getJdbcDrivers().clear();
        dsfat10.setJdbcDriverRef("Derby");
        dsfat10.getJdbcDrivers().clear();
        Derby.setId("Derby");
        config.getJdbcDrivers().add(Derby);

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            // Verify that dataSource is usable and its connectionManager has maxPoolSize=2
            runTest(basicfat, "testIsolatedSharedLibraries");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Update data source configuration to add, modify and remove onConnect while the server is running.
     */
    @Test
    public void testConfigChangeOnConnect() throws Throwable {
        String method = "testConfigChangeOnConnect";
        Log.info(c, method, "Executing " + method);

        // add onConnect to data source
        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsfat5derby = config.getDataSources().getBy("id", "dsfat5derby");
        Set<String> dsfat5derby_onConnects = dsfat5derby.getOnConnects();
        dsfat5derby_onConnects.add("DECLARE GLOBAL TEMPORARY TABLE TEMP1 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED");

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testOnConnectTable1");

        // modify the onConnect that we just added
        dsfat5derby_onConnects.clear();
        dsfat5derby_onConnects.add("DECLARE GLOBAL TEMPORARY TABLE TEMP2 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED");

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testOnConnectTable1NotFound");
        runTest(basicfat, "testOnConnectTable2");

        // add the first onConnect again, so that both apply
        dsfat5derby_onConnects.add("DECLARE GLOBAL TEMPORARY TABLE TEMP1 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED");

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testOnConnectTable1");
        runTest(basicfat, "testOnConnectTable2");

        // remove both
        dsfat5derby_onConnects.clear();

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testOnConnectTable1NotFound");
        runTest(basicfat, "testOnConnectTable2NotFound");

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Add a data source with purgePolicy=FailingConnectionOnly.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException",
                    "java.sql.SQLException",
                    "java.sql.SQLNonTransientConnectionException",
                    "javax.resource.spi.ResourceAllocationException" })
    public void testConfigChangePurgePolicy() throws Throwable {
        String method = "testConfigChangePurgePolicy";
        Log.info(c, method, "Executing " + method);

        ServerConfiguration config = server.getServerConfiguration();
        DataSource ds = new DataSource();
        config.getDataSources().add(ds);
        ds.setId("ds-tct");
        ds.setJndiName("jdbc/ds-tct");
        ds.setJdbcDriverRef("Derby");
        ds.setValidationTimeout("20s");
        Properties_derby_embedded properties_derby_embedded = new Properties_derby_embedded();
        ds.getProperties_derby_embedded().add(properties_derby_embedded);
        properties_derby_embedded.setDatabaseName("memory:dstct");
        ConnectionManager cm = new ConnectionManager();
        ds.getConnectionManagers().add(cm);
        cm.setMaxPoolSize("3");
        cm.setPurgePolicy("FailingConnectionOnly");

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testTestConnectionTimerNotRunning");
        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Update data source configuration to add, modify and remove validationTimeout while the server is running.
     */
    @Test
    @ExpectedFFDC({ "javax.resource.ResourceException" })
    public void testConfigChangeForValidationTimeout() throws Throwable {
        String method = "testConfigChangeForValidationTimeout";
        Log.info(c, method, "Executing " + method);

        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsValTderby = new DataSource();
        config.getDataSources().add(dsValTderby);
        dsValTderby.setId("dsValTderby");
        dsValTderby.setJndiName("jdbc/dsValTderby");
        dsValTderby.setJdbcDriverRef("Derby");
        Properties_derby_embedded pdeProps = new Properties_derby_embedded();
        pdeProps.setCreateDatabase("create");
        pdeProps.setDatabaseName("memory:dsValTderby");
        dsValTderby.getProperties_derby_embedded().add(pdeProps);
        dsValTderby.setValidationTimeout("10s");

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testValTimeoutTable1");

        dsValTderby.setValidationTimeout(null);

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testValNoTimeoutTable1");

        dsValTderby.setValidationTimeout("0");

        updateServerConfig(config, EMPTY_EXPR_LIST);
        runTest(basicfat, "testValTimeoutTable1");

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Update the shared library configuration to switch the fileset from nested to top level config
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeSharedLibrary() throws Throwable {
        String method = "testConfigChangeSharedLibrary";
        Log.info(c, method, "Executing " + method);

        // Use the data source before making the dynamic update
        runTest(basicfat, "testBasicQuery");

        // Move the shared library's fileset out of nested config and into a top level config element.
        ServerConfiguration config = server.getServerConfiguration();
        JdbcDriver FATJDBCDriver = config.getJdbcDrivers().getBy("id", "FATJDBCDriver");
        Library FATJDBCDriver_library = FATJDBCDriver.getNestedLibrary();
        FATJDBCDriver_library.setFilesetRef("FATJDBCFileset");
        Fileset FATJDBCDriver_library_fileset = FATJDBCDriver_library.getNestedFileset();
        FATJDBCDriver_library.setNestedFileset(null);
        FATJDBCDriver_library_fileset.setId("FATJDBCFileset");
        config.getFilesets().add(FATJDBCDriver_library_fileset);

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testBasicQuery");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Update the datasource configuration to add transactional=false
     * while the server is running.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeTransactional() throws Throwable {
        String method = "testConfigChangeTransactional";
        Log.info(c, method, "Executing " + method);

        // set transactional to false for data source
        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsfat1 = config.getDataSources().getBy("id", "dsfat1");
        dsfat1.setTransactional("false");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testConfigChangeTransactionalFalse");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // remove transactional (unspecified should default to true)
        dsfat1.setTransactional(null);

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testConfigChangeTransactionalTrue");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Update the data source configuration while connections are active.
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeWithActiveConnections() throws Throwable {
        String method = "testConfigChangeWithActiveConnections";
        Log.info(c, method, "Executing " + method);

        // On a separate thread, run a servlet that keeps a connection open for a few seconds
        // and checks the default queryTimeout value every 100 milliseconds.
        // this will fail if the timeout does not increase or goes above a certain hardcoded value.
        final BlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        new Thread() {
            @Override
            public void run() {
                try {
                    runTest(basicfat, "testConfigChangeWithActiveConnections");
                    results.add("successful");
                } catch (Throwable x) {
                    results.add(x);
                }
            }
        }.start();

        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsfat5 = config.getDataSources().getBy("id", "dsfat5derby");
        try {
            // Increase the queryTimeout several times
            for (int qt = 31; qt <= 34; qt++) {
                dsfat5.setQueryTimeout("" + qt);
                /*
                 * each time we change the file we toggle beginTranForResultSetScrollingAPIs from true to false to change the file size
                 * otherwise defect 58455 may occur as when the file is changed but remains the same size and has the same timestamp the
                 * change is not picked up
                 */
                if (qt % 2 == 1) {
                    dsfat5.setBeginTranForResultSetScrollingAPIs("true");
                } else {
                    dsfat5.setBeginTranForResultSetScrollingAPIs("false");
                }
                updateServerConfig(config, EMPTY_EXPR_LIST);
                Thread.sleep(100);
            }
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        } finally {
            Object result = results.poll(10, TimeUnit.SECONDS);
            if (result == null)
                throw new Exception("Test did not complete within allotted time");
            else if (result instanceof Throwable)
                throw (Throwable) result;
        }

        cleanUpExprs = EMPTY_EXPR_LIST;
    }

    /**
     * Use a data source with some configuration errors and verify that the
     * ignore/warn/fail setting (which defaults to warn) outputs the correct warnings.
     *
     * @param out PrintWriter for servlet response
     * @throws Throwable if it fails.
     */
    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC({ "java.lang.UnsupportedOperationException", "java.sql.SQLException" })
    public void testOnError() throws Throwable {
        String method = "testOnError";
        Log.info(c, method, "Executing " + method);

        runTest(basicfat, "testOnErrorFAIL");

        ServerConfiguration config = server.getServerConfiguration();
        Variable onError = null;
        for (Variable variable : config.getVariables())
            if (variable.getName().equals("onError"))
                onError = variable;

        onError.setValue("WARN");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            runTest(basicfat, "testOnErrorWARN");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        List<String> messages = server.findStringsInLogs("J2CA8040E");
        if (messages.isEmpty())
            throw new Exception("Did not find J2CA8040E for multiple dataSources (dsfat5derby and dsfat11derby) using same connectionManager.");
        else
            Log.info(c, method, "Found expected: " + messages);

        messages = server.findStringsInLogs("DSRA8020E");

        // TODO: enable this check if we add a stricter variant of onError
//        boolean foundBadProperty = false;
        boolean foundBadVendorProperty = false;
        for (String m : messages)
            if (m.indexOf("badProperty") > 0) {
//                foundBadProperty = true;
                Log.info(c, method, "Found expected warning: " + m);
            } else if (m.indexOf("badVendorProperty") > 0) {
                foundBadVendorProperty = true;
                Log.info(c, method, "Found expected warning: " + m);
            }

//        if (!foundBadProperty)
//            throw new Exception("Did not find DSRA8020E warning for invalid WAS data source property.");
        if (!foundBadVendorProperty)
            throw new Exception("Did not find DSRA8020E warning for invalid vendor data source property.");

        // Use the other data source (dsfat5derby), to ensure it hasn't been corrupted by dsfat11derby
        runTest(basicfat, "testConfigChangeDataSourceOriginalConfig");

        // Switch to onError=FAIL
        onError.setValue("FAIL");

        // Cause another configuration error (minPoolSize > maxPoolSize)
        ConnectionManager conMgr11 = new ConnectionManager();
        conMgr11.setId("conMgr11");
        conMgr11.setMaxPoolSize("5");
        conMgr11.setMinPoolSize("10");
        config.addConnectionManager(conMgr11);
        DataSource dsfat11 = config.getDataSources().getBy("id", "dsfat11derby");
        dsfat11.setConnectionManagerRef("conMgr11");

        try {
            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            // Verify that it fails when used
            runTest(basicfat, "testOnErrorFAIL");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        // Switch to onError=IGNORE
        onError.setValue("IGNORE");

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            // Verify that it works now
            runTest(basicfat, "testOnErrorIGNORE");
        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Update the data source configuration while the server is running.
     * Uses <file> element
     *
     * @throws Throwable if it fails.
     */
    @Test
    public void testConfigChangeFileElement() throws Throwable {
        String method = "testConfigChangeDataSourceFileAndFolder";
        Log.info(c, method, "Executing " + method);
        ServerConfiguration config = server.getServerConfiguration();

        try {
            // Switch the Datasource to uses a shared library with a <file> element

            // Create the <File> element
            File file = new File();
            file.setName("${shared.resource.dir}/derby/derby.jar");
            file.setId("FileElementTest");

            // Create new Library
            Library library = new Library();
            library.setId("LibraryFileTest");
            library.setFileRef("FileElementTest");
            library.setNestedFile(file);
            config.getLibraries().add(library);
            JdbcDriver jdbcDriver = config.getJdbcDrivers().getBy("id", "Derby");
            jdbcDriver.setLibraryRef("LibraryFileTest");
            DataSource dsfat5 = config.getDataSources().getBy("id", "dsfat5derby");
            dsfat5.getProperties_derby_embedded().get(0).setDatabaseName("memory:derbFileTest");

            // server.waitForConfigUpdateInLogUsingMark() might timeout without the following call to testBasicQuery.
            // If the app hasn't been used yet(this test runs first), then the config doesn't need an update.  This
            // call to testBasicQuery forces an app update to always occur.
            runTest(basicfat, "testBasicQuery");

            updateServerConfig(config, JDBCAPP_RECYCLE_EXPR_LIST);
            runTest(basicfat, "testConfigChangeDataSourceOriginalConfig");
            runTest(basicfat, "testDataSourceDefinitions");

        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
    }

    /**
     * Update the JDBC driver configuration while the server is running.
     * Uses Automatic Library which is not defined in server.xml.
     *
     * @throws Throwable if it fails.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testConfigChangeAutomaticLibrary() throws Throwable {
        String method = "testConfigChangeAutomaticLibrary";
        Log.info(c, method, "Executing " + method);
        ServerConfiguration config = server.getServerConfiguration();

        try {

            // Library already exists but is not defined in server.xml
            JdbcDriver jdbcDriver = config.getJdbcDrivers().getBy("id", "Derby");
            jdbcDriver.setLibraryRef("AutomaticDerbyLibrary");

            ConfigElementList<Application> apps = config.getApplications();

            Application app1 = apps.getById(jdbcapp);
            ClassloaderElement cl1 = app1.getClassloaders().get(0);
            Set<String> commonLibs = cl1.getCommonLibraryRefs();
            commonLibs.remove("DerbyLib");
            commonLibs.add("AutomaticDerbyLibrary");

            Application app2 = apps.getById(dsdfat);
            ClassloaderElement cl2 = app2.getClassloaders().get(0);
            Set<String> privateLib = cl2.getPrivateLibraryRefs();
            privateLib.remove("DerbyLib");
            privateLib.add("AutomaticDerbyLibrary");

            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(appNames, EMPTY_EXPR_LIST);
            try {
                server.stopServer(ALLOWED_MESSAGES);

                //Get driver type
                server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
                server.addEnvVar("ANON_DRIVER", "driver" + DatabaseContainerType.valueOf(testContainer).ordinal() + ".jar");
                server.addEnvVar("DB_USER", testContainer.getUsername());
                server.addEnvVar("DB_PASSWORD", testContainer.getPassword());
            } finally {
                server.startServer();
            }

            runTest(basicfat, "testConfigChangeDataSourceOriginalConfig");
            runTest(basicfat, "testDataSourceDefinitions");
            runTest(basicfat, "testIsolatedSharedLibraries");

        } catch (Throwable x) {
            System.out.println("Failure during " + method + " with the following config:");
            System.out.println(config);
            throw x;
        }

        cleanUpExprs = JDBCAPP_AND_DSDFAT_RECYCLE_EXPR_LIST;
    }

    /**
     * Test the tracing for whatever current driver is used. Steps:
     *
     * <ol>
     * <li>Disable all tracing and restart the server to ensure that we are
     * at a clean slate.</li>
     * <li>Execute a basic query test and verify that NO traces are present.</li>
     * <li>Enable trace dynamically (without re-starting the server) and run the
     * test again to determine if tracing is now working.</li>
     * <li>If the trace strings were found, exit test case - we passed, else:
     * <ol>
     * <li>Restart the server (with the updated configuration).</li>
     * <li>Run the basic test again, if trace now exists we know that trace can't
     * be updated dynamically but does work. If the trace strings still aren't found
     * then we know something is wrong - either some additional parameters are not
     * met or trace is broke.</li>
     * </ol>
     * </li>
     * </ol>
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTrace() throws Throwable {
        String method = "testTrace";
        Log.info(c, method, "Executing " + method);
        ServerConfiguration config = server.getServerConfiguration();
        List<String> matches = new ArrayList<String>();

        DataSource dsfat1 = config.getDataSources().getBy("id", "dsfat1");
        JdbcDriver jdbcDriver = config.getJdbcDrivers().getBy("id", "FATJDBCDriver");
        Library driverLibrary = jdbcDriver.getNestedLibrary();
        Fileset libraryFileset = driverLibrary.getNestedFileset();
        String includes = libraryFileset.getIncludes();
        includes = includes == null ? null : includes.toLowerCase();
        String dsPropsAlias = dsfat1.getDataSourcePropertiesUsedAlias();
        String traceString = null, traceSpec = null, platform = null;

        if (dsPropsAlias.equals(DataSourceProperties.GENERIC)) {
            //When using generic properties and likely a generic jdbc driver we will enable com.ibm.ws.database.logwriter
            //However, we cannot guarantee that this will enable tracing for every jdbc driver  (ex Oracle)
            //in general this should work, but since it is not guaranteed we will skip testing this case

            return;
        }

        // 1) Disable all tracing
        switch (dsPropsAlias) {
            case DataSourceProperties.DB2_JCC:
                platform = "DB2 (JCC)";
                traceSpec = "com.ibm.ws.db2.logwriter=all=enabled";
                traceString = "\\[jcc\\]\\[";

                ConfigElementList<Properties_db2_jcc> db2JccProps = dsfat1.getProperties_db2_jcc();
                if (!db2JccProps.isEmpty())
                    db2JccProps.get(0).setTraceLevel(null);
                break;
            case DataSourceProperties.DERBY_EMBEDDED:
                platform = "Derby Embedded";
                traceSpec = "com.ibm.ws.derby.logwriter=all=enabled";
                traceString = "new org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource40()";

                dsfat1.setSupplementalJDBCTrace(null);
                break;
            case DataSourceProperties.DERBY_CLIENT:
                platform = "Derby Network Client";
                traceSpec = "com.ibm.ws.derby.logwriter=all=enabled";
                traceString = "Driver: Apache Derby Network Client JDBC Driver";

                ConfigElementList<Properties_derby_client> derbyProps = dsfat1.getProperties_derby_client();
                if (!derbyProps.isEmpty())
                    derbyProps.get(0).setTraceLevel(null);
                break;
            case DataSourceProperties.ORACLE_JDBC:
                // Oracle tracing will only work if we are using *_g.jar
                // Make a best effort to check for it
                if (includes != null) {
                    platform = "Oracle";
                    traceSpec = "oracle.*=all";
                    traceString = "oracle.jdbc.driver.OracleDriver";
                    if (!includes.contains("_g.jar")) {
                        // make an effort to use the correct jars
                        StringBuilder sb = new StringBuilder();
                        String[] jars = includes.split(" ");
                        for (int i = 0; i < jars.length; ++i) {
                            if (jars[i].startsWith("ojdbc")) {
                                int index = jars[i].indexOf('.');
                                sb.append(jars[i].substring(0, index) + "_g.jar");
                                if (i + 1 != jars.length)
                                    sb.append(' ');
                            }
                        }

                        libraryFileset.setIncludes(sb.toString());
                    }
                } else {
                    Log.info(c, method, "Did not find *_g.jar required for Oracle tracing - aborting test");
                    return;
                }
                break;
            case DataSourceProperties.DATADIRECT_SQLSERVER:
                platform = "SQL Server (DataDirect)";
                traceSpec = "com.ibm.ws.sqlserver.logwriter=all=enabled";
                traceString = "jdbc:datadirect:sqlserver:";

                dsfat1.setSupplementalJDBCTrace(null);
                break;
            case DataSourceProperties.MICROSOFT_SQLSERVER:
                platform = "SQL Server (Microsoft)";
                traceSpec = "com.ibm.ws.sqlserver.logwriter=all=enabled";
                traceString = "setURL\\(\"jdbc:sqlserver://\"\\)|setApplicationName\\(\"Microsoft JDBC Driver for SQL Server\"\\)";

                dsfat1.setSupplementalJDBCTrace(null);
                break;
            case DataSourceProperties.SYBASE:
                platform = "Sybase";
                traceString = "new com.sybase.jdbc4.jdbc.SybConnectionPoolDataSource()|new com.sybase.jdbc3.jdbc.SybConnectionPoolDataSource()";
                traceSpec = "com.ibm.ws.sybase.logwriter=all=enabled";

                dsfat1.setSupplementalJDBCTrace(null);
                break;
            default:
                // skip the test since we don't know what we are running with
                platform = "Unknown";
                break;
        }

        if (traceSpec == null) {
            Log.info(c, method, "Trace spec not found for " + platform + " therefore skipping test");
            return;
        }
        if (traceString == null) {
            Log.info(c, method, "Couldn't find a trace string for " + platform + " therefore skipping test");
            return;
        }

        Log.info(c, method, "Trace spec found for " + platform + " and result is: " + traceSpec);

        try {
            updateServerConfig(config, EMPTY_EXPR_LIST);
            try {
                server.stopServer(ALLOWED_MESSAGES);

                //Get driver type
                server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
                server.addEnvVar("ANON_DRIVER", "driver" + DatabaseContainerType.valueOf(testContainer).ordinal() + ".jar");
                server.addEnvVar("DB_USER", testContainer.getUsername());
                server.addEnvVar("DB_PASSWORD", testContainer.getPassword());
            } finally {
                server.startServer();
            }
        } catch (Throwable t) {
            System.out.println("Failure during " + method + " with the following config: ");
            System.out.println(config);
            throw t;
        }

        // 2) execute testBasicQuery and ensure that NO trace is found
        runTest(basicfat, "testBasicQuery");
        matches = server.findStringsInLogsAndTrace(traceString);

        if (!matches.isEmpty())
            throw new Exception("Trace should be disabled, but found \"" + traceString + "\" for " + platform);

        // 3) attempt to enable trace dynamically and re-run test
        switch (dsPropsAlias) {
            case DataSourceProperties.DB2_JCC:
                ConfigElementList<Properties_db2_jcc> db2JccProps = dsfat1.getProperties_db2_jcc();
                if (!db2JccProps.isEmpty())
                    db2JccProps.get(0).setTraceLevel("-1");
                break;
            case DataSourceProperties.DERBY_EMBEDDED:
                dsfat1.setSupplementalJDBCTrace("true");
                break;
            case DataSourceProperties.DERBY_CLIENT:
                ConfigElementList<Properties_derby_client> derbyProps = dsfat1.getProperties_derby_client();
                if (!derbyProps.isEmpty())
                    derbyProps.get(0).setTraceLevel("-1");
                break;
            case DataSourceProperties.ORACLE_JDBC:
                break; // No additional setting needed to setup Oracle trace
            case DataSourceProperties.DATADIRECT_SQLSERVER:
                dsfat1.setSupplementalJDBCTrace("true");
                break;
            case DataSourceProperties.MICROSOFT_SQLSERVER:
                dsfat1.setSupplementalJDBCTrace("true");
                break;
            case DataSourceProperties.SYBASE:
                dsfat1.setSupplementalJDBCTrace("true");
                break;
        }

        String baseTraceSpec = "*=info=enabled"; //:com.ibm.ws.jdbc.*=all=enabled:com.ibm.ejs.j2c.*=all=enabled:com.ibm.ws.rsadapter.*=all=enabled";
        config.getLogging().setTraceSpecification(baseTraceSpec + ':' + traceSpec);

        try {
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            if (!platform.equalsIgnoreCase("Oracle"))
                server.waitForConfigUpdateInLogUsingMark(appNames, JDBCAPP_RECYCLE_EXPR_LIST);
            else
                server.waitForStringInLogUsingMark("TRAS0018I.*" + traceSpec);
            // trace should be enabled now
            runTest(basicfat, "testBasicQuery");
        } catch (Throwable t) {
            System.out.println("Failure during " + method + " with the following config: ");
            System.out.println(config);
            throw t;
        }
        matches = server.findStringsInLogsAndTrace(traceString);
        if (matches.isEmpty()) {
            // 4) if trace wasn't found, restart the server and test again
            try {
                server.stopServer(ALLOWED_MESSAGES);

                //Get driver type
                server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());
                server.addEnvVar("ANON_DRIVER", "driver" + DatabaseContainerType.valueOf(testContainer).ordinal() + ".jar");
                server.addEnvVar("DB_USER", testContainer.getUsername());
                server.addEnvVar("DB_PASSWORD", testContainer.getPassword());
            } finally {
                server.startServer();
            }
            runTest(basicfat, "testBasicQuery");
            matches = server.findStringsInLogsAndTrace(traceString);

            // 5) if trace(s) still not found, something is wrong: throw an error
            if (matches.isEmpty())
                throw new Exception("Trace string could not be found. Expected to find: " + traceString);
        }

        if (!platform.equalsIgnoreCase("Oracle"))
            cleanUpExprs = JDBCAPP_RECYCLE_EXPR_LIST;
        else
            cleanUpExprs = EMPTY_EXPR_LIST;
    }
}
