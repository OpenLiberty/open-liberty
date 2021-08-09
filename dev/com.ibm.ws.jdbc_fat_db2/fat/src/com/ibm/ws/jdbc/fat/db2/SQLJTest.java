/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.db2;

import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Db2Container;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SQLJTest extends FATServletClient {

    @Server("com.ibm.ws.sqlj.fat")
    public static LibertyServer server;

    public static Db2Container db2 = FATSuite.db2;

    public static final String JEE_APP = "sqljapp";
    public static final String SERVLET_NAME = "SQLJTestServlet";

    // Directory paths for shrinkwrap
    private static final String BASEDIR = System.getProperty("user.dir");
    private static final String CLASS_DIR = BASEDIR + "/build/classes/";

    // Event types for eventLogging
    private static final String EVENT_PS_EXEC_UPDATE = "websphere.datasource.psExecuteUpdate";
    private static final String EVENT_PS_EXEC_QUERY = "websphere.datasource.psExecuteQuery";
    private static final String EVENT_EXEC = "websphere.datasource.execute";
    private static final String EVENT_PS_EXEC = "websphere.datasource.psExecute";

    private static boolean threadLocalContextEnabled = false;

    @BeforeClass
    public static void setUp() throws Exception {
        applyDB2Env();
        setUpSQLJ();
        // Create a normal Java EE application and export to server
        WebArchive app = ShrinkWrap.create(WebArchive.class, JEE_APP + ".war");
        app = addGeneratedPackage(app, "web");
        ShrinkHelper.exportAppToServer(server, app);
        server.addInstalledAppForValidation(JEE_APP);
        server.startServer(SQLJTest.class.getSimpleName() + "_before_warmstart.log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private static void applyDB2Env() {
        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
        server.addEnvVar("DB2_HOSTNAME", db2.getContainerIpAddress());
        server.addEnvVar("DB2_PORT", String.valueOf(db2.getMappedPort(50000)));
        server.addEnvVar("DB2_USER", db2.getUsername());
        server.addEnvVar("DB2_PASS", db2.getPassword());
    }

    @Before
    public void beforeEach() throws Exception {
        server.setMarkToEndOfLog();
    }

    @Test
    public void testDocExample() throws Exception {
        runTest(JEE_APP);
    }

    @Test
    public void testSQLJSimpleSelect() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateRead1() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateReadWithConnectionContext() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateReadWithConnectionContextWrapper() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testResetExecutionContextWhenCachingSQLJContext() throws Exception {
        runTest(JEE_APP);
    }

    @Test
    public void testBasicCreateReadWithConnectionContextWrapperAndCachedSQLJContext() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateReadWithDataSource() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateReadWithSQLJContext() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateRollback1() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateUpdateDelete1() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicCreateUpdateRollback1() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBasicDeleteRollback1() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testBatching() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_QUERY);
    }

    @Test
    @AllowedFFDC("com.ibm.db2.jcc.am.BatchUpdateException")
    public void testBatchUpdateException() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testCacheCallableStatement() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testCacheCallableStatementAndSQLJContext() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testCacheCallableStatementAndSQLJContext2() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testCachePreparedStatement() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testCachePreparedStatementAndSQLJContext() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testCachePreparedStatementAndSQLJContextWithIsolationLevels() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testConnectionCasting() throws Exception {
        runTest(JEE_APP);
    }

    @Test
    public void testSQLJJDBCCombo1() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC_UPDATE, EVENT_PS_EXEC_QUERY, EVENT_EXEC);
    }

    @Test
    public void testBasicCallableStatement() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testIsolation_RU() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testIsolation_RC() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testIsolation_SER() throws Exception {
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testDefaultContext() throws Exception {
        if (threadLocalContextEnabled) {
            // Because SQLJ does not allow disabling thread local context once enabled,
            // if the _threadlocal tests run before this one, we must restart the server
            // in order to clear out the thread local context
            server.stopServer();
            applyDB2Env();
            server.startServer();
            threadLocalContextEnabled = false;
        }
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testDefaultContext_caching() throws Exception {
        if (threadLocalContextEnabled) {
            // Because SQLJ does not allow disabling thread local context once enabled,
            // if the _threadlocal tests run before this one, we must restart the server
            // in order to clear out the thread local context
            server.stopServer();
            applyDB2Env();
            server.startServer();
            threadLocalContextEnabled = false;
        }
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testDefaultContext_threadlocal() throws Exception {
        threadLocalContextEnabled = true;
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testDefaultContext_caching_threadlocal() throws Exception {
        threadLocalContextEnabled = true;
        runTest(JEE_APP);
        checkForPMI(EVENT_PS_EXEC, EVENT_PS_EXEC_QUERY);
    }

    @Test
    public void testWarmStart() throws Exception {
        runTest(JEE_APP);
        server.stopServer();
        applyDB2Env();
        server.startServer(SQLJTest.class.getSimpleName() + "_after_warmstart.log", false);
        runTest(JEE_APP);
    }

    private void runTest(String appName) throws Exception {
        runTest(server, appName + '/' + SERVLET_NAME, testName);
    }

    /**
     * Classes generated at runtime (i.e. the SQLJ .class files) are not picked up by the
     * classloader immediately so we need to force-feed the classes to ShrinkWrap.
     */
    private static WebArchive addGeneratedPackage(WebArchive war, String packageName) {
        String packagePath = packageName.replace('.', '/');
        File packageDir = new File(CLASS_DIR + packagePath);

        for (File f : packageDir.listFiles())
            if (f.isFile())
                war = war.addAsResource(f, packagePath + '/' + f.getName());

        return war;
    }

    /**
     * Perform the following steps in sequence:<br>
     * 1 - Run sqlj.tools.Sqlj on any .sqlj files. Normally this is done at build time,
     * but we've just checked in the output of this step as the Sqlj transformer is not publicly available<br>
     * 2 - Create tables on the DB using JSE JDBC<br>
     * 3 - Run com.ibm.db2.jcc.sqlj.Customizer to<br>
     * enhance the .ser files generated in step 1.
     */
    private static void setUpSQLJ() throws Exception {
        String javaClassPath = System.getProperty("java.class.path");

        // 2 - Setup tables and stored procedures needed for the DB2 SQLJ Customizer
        Connection conn = db2.createConnection("");
        try {
            Statement st = conn.createStatement();
            try {
                st.execute("drop table sqljtest");
            } catch (SQLException x) {
            }
            st.executeUpdate("create table sqljtest (id int not null primary key, name varchar(20))");

            String regProc = "CREATE OR REPLACE PROCEDURE PUTNAME" +
                             "(IN id_num INTEGER, IN name_str VARCHAR(20)) " +
                             "BEGIN INSERT INTO sqljtest VALUES (id_num, name_str); END";
            st.execute(regProc);
        } finally {
            conn.close();
        }

        // 3 - Run the DB2 SQLJ Customizer to "enhance" the previously generated .ser files
        List<String> args = new ArrayList<String>();
        args.add("java");
        if (JavaInfo.JAVA_VERSION >= 9) {
            args.add("--add-opens=java.base/java.util=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.lang.module=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.util.concurrent=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.io=ALL-UNNAMED");
            args.add("--add-opens=java.base/jdk.internal.reflect=ALL-UNNAMED");
            args.add("--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.lang.ref=ALL-UNNAMED");
            args.add("--add-opens=java.base/jdk.internal.module=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.net=ALL-UNNAMED");
            args.add("--add-opens=java.base/jdk.internal.perf=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.nio=ALL-UNNAMED");
            args.add("--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.util.jar=ALL-UNNAMED");
            args.add("--add-opens=java.base/jdk.internal.util.jar=ALL-UNNAMED");
            args.add("--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED");
            args.add("--add-opens=java.base/java.security=ALL-UNNAMED");
        }
        args.add("com.ibm.db2.jcc.sqlj.Customizer");
        args.add("-url");
        args.add(db2.getJdbcUrl());
        args.add("-user");
        args.add(db2.getUsername());
        args.add("-password");
        args.add(db2.getPassword());
        args.add("-rootPkgName");
        args.add("web");
        args.add("SQLJProcedure_SJProfile0.ser");
        args.add("SQLJProcedure_SJProfile1.ser");
        args.add("SQLJProcedure_SJProfile2.ser");
        args.add("SQLJProcedure_SJProfile3.ser");
        ProcessBuilder customize = new ProcessBuilder(args);
        char colon = File.pathSeparatorChar;
        String cp = javaClassPath + colon + CLASS_DIR + "web";
        customize.environment().put("CLASSPATH", cp);

        Log.info(SQLJTest.class, "setUpSQLJ", "Running SQLJ customizer with command: " + customize.command());
        Process customizeProc = customize.redirectErrorStream(true).start();

        int rc = customizeProc.waitFor();
        String cmdOutput = readInputStream(customizeProc.getInputStream());
        Log.info(SQLJTest.class, "setUpSQLJ", cmdOutput);
        Log.info(SQLJTest.class, "setUpSQLJ", "Got return code " + rc + " from the DB2 Customizer.");
        if (rc != 0) {
            throw new Exception(cmdOutput);
        }
    }

    private static String readInputStream(InputStream is) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void checkForPMI(String... eventTypes) throws Exception {
        for (String eventType : eventTypes) {
            // Each event should have a BEGIN and END since logMode="entryExit" in the server.xml
            assertNotNull("Unable to locate BEGIN event for eventType: " + eventType + " in messages.log",
                          server.waitForStringInLogUsingMark("BEGIN.*eventType=" + eventType, 100));
            assertNotNull("Unable to locate END event for eventType: " + eventType + " in messages.log",
                          server.waitForStringInLogUsingMark("END.*eventType=" + eventType, 100));
        }
    }
}
