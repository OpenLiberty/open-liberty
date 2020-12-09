/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.jbatch.test.dbservlet.DbServletClient;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 *
 */
public abstract class BatchFATHelper {

    protected final String DFLT_CTX_ROOT = "batchFAT";
    public final static String DFLT_PERSISTENCE_DDL = "common/batch-derby.ddl";
    public final static String DFLT_SERVER_XML = "common/server.xml";
    public final static String DFLT_PERSISTENCE_JNDI = "jdbc/batch";
    public final static String DFLT_PERSISTENCE_SCHEMA = "JBATCH";
    public final static String DFLT_TABLE_PREFIX = "";

    public static final String APP_OUT1 = "APP.OUT1";

    public static final String SUCCESS_MESSAGE = "TEST PASSED";
    public static final String FAILED_MESSAGE = "FAILED";
    protected static final int TIMEOUT = 10;

    private static String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");

    protected static final LibertyServer server = LibertyServerFactory.getLibertyServer("batchFAT");

    public String _testName = "";

    protected final static String ADMIN_NAME = "bob";
    protected final static String ADMIN_PASSWORD = "bobpwd";

    //Instance fields
    private final Map<String, String> adminHeaderMap = Collections.singletonMap("Authorization", "Basic " + Base64Coder.base64Encode(ADMIN_NAME + ":" + ADMIN_PASSWORD));

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setupForSyntheticJava6Test() {
    }

    @Before
    public void setTestName() throws Exception {
        _testName = name.getMethodName();
        Log.info(this.getClass(), _testName, "===== Starting test " + _testName + " =====");
    }

    protected String getTempFilePrefix(String filePrefix) {
        return tmpDir + java.io.File.separator + filePrefix;
    }

    protected String getContextRoot() {
        return DFLT_CTX_ROOT;
    }

    protected String test(String servlet) throws Exception {
        return test(servlet, null);
    }

    protected String test(String servlet, String urlParms) throws Exception {
        return test(server, "", _testName, servlet, urlParms, SUCCESS_MESSAGE);
    }

    protected String test(String servlet, String urlParms, String statusMessage) throws Exception {
        return test(server, "", _testName, servlet, urlParms, statusMessage);
    }

    protected String test(LibertyServer server, String appname, String testName, String servlet, String urlParms, String statusMessage) throws Exception {
        String urlAppend = (urlParms == null ? "" : "?" + urlParms);

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                          "/" + getContextRoot() + "/" + servlet + urlAppend);

        Log.info(this.getClass(), "test", "About to execute URL: " + url);

        String output = HttpUtils.getHttpResponseAsString(url);

        assertNotNull(output);
        assertNotNull(output.trim());
        assertTrue("' appname:'" + appname + "' output:'" + output + "' testName:'" + testName + "'", output.trim().contains(statusMessage));
        return output;
    }

    protected String testWithHttpAuthHeader(String servlet, String urlParms) throws Exception {
        return testWithHttpAuthHeader(server, "", _testName, servlet, urlParms);
    }

    protected String testWithHttpAuthHeader(LibertyServer server, String appname, String testName, String servlet, String urlParms) throws Exception {
        String urlAppend = (urlParms == null ? "" : "?" + urlParms);

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                          "/" + getContextRoot() + "/" + servlet + urlAppend);

        Log.info(this.getClass(), "test", "About to execute URL: " + url);

        HttpURLConnection con = getConnection("/" + getContextRoot() + "/" + servlet + urlAppend,
                                              HttpURLConnection.HTTP_OK,
                                              HTTPRequestMethod.GET,
                                              null,
                                              adminHeaderMap);

        BufferedReader br = HttpUtils.getConnectionStream(con);

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();

        String output = response.toString();

        assertNotNull(output);
        assertNotNull(output.trim());
        assertTrue("' appname:'" + appname + "' output:'" + output + "' testName:'" + testName + "'", output.trim().contains(SUCCESS_MESSAGE));
        return output;
    }

    private static long MYTIMEOUT = 20000;

    public static void setConfig(String config, Class testClass) throws Exception {
        Log.info(testClass, "setConfig", "Setting server.xml to: " + config);
        setConfig(server, config, testClass.getSimpleName());
    }

    /**
     * Apply the config file
     */
    protected static void setConfig(LibertyServer server, String config, String testName) throws Exception {
        server.setServerConfigurationFile(config);
    }

    /**
     * Start server and set log file to the test name
     */
    public static void startServer(LibertyServer server, Class testClass) throws Exception {
        server.startServer(testClass.getSimpleName() + ".log");
    }

    /**
     * Execute the given SQL against the given dataSource (identified by its jndi)
     *
     */
    public static void executeSql(String dataSourceJndi, String sql) throws IOException {
        new DbServletClient().setDataSourceJndi(dataSourceJndi).setDataSourceUser("user", "pass").setHostAndPort(server.getHostname(),
                                                                                                                 server.getHttpDefaultPort()).setSql(sql).executeUpdate();
    }

    /**
     * Execute the given SQL against the given dataSource (identified by its jndi)
     *
     */
    public static void loadAndExecuteSql(String dataSourceJndi,
                                         String fileName,
                                         String schema,
                                         String tablePrefix) throws IOException {
        new DbServletClient().setDataSourceJndi(dataSourceJndi).setDataSourceUser("user", "pass").setHostAndPort(server.getHostname(),
                                                                                                                 server.getHttpDefaultPort()).loadSql(server.pathToAutoFVTTestFiles
                                                                                                                                                      + fileName, schema,
                                                                                                                                                      tablePrefix).executeUpdate();
    }

    /**
     * @return SQL for CREATEing the input table and INSERTing some values.
     *         This table is used by the Chunk tests.
     */
    public static String getChunkInTableSql() {

        String[] inputVals = { "AAA", "BB", "C", "DDDD", "EEE", "FF", "G", "HHHHH", "IIII", "JJJ", "KK", "L" };

        StringBuilder retMe = new StringBuilder();
        retMe.append("DROP TABLE APP.INTABLE;");
        retMe.append("CREATE TABLE APP.INTABLE("
                     + "id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT APP.INTABLE_PK PRIMARY KEY,"
                     + "name VARCHAR(512));");

        for (String inputVal : inputVals) {
            retMe.append("INSERT INTO APP.INTABLE (name) VALUES('" + inputVal + "');");
        }

        return retMe.toString();
    }

    /**
     * @return SQL for CREATEing the output table. This table is used by Chunk tests.
     */
    public static String getChunkOutTableSql(String tableName) {
        return "DROP TABLE " + tableName + ";"
               + "CREATE TABLE " + tableName
               + "(name VARCHAR(512) CONSTRAINT " + tableName + "_PK PRIMARY KEY,"
               + "lettercount BIGINT);";
    }

    protected static void restartServerAndWaitForAppStart() throws Exception {
        //restart server
        if (server != null && server.isStarted()) {
            server.restartServer();
        }
        server.waitForStringInLog("CWWKF0011I", MYTIMEOUT);
    }

    protected static void createDefaultRuntimeTables() throws Exception {
        // No-op now with JPA auto-create
        //loadAndExecuteSql(DFLT_PERSISTENCE_JNDI,
        //                 DFLT_PERSISTENCE_DDL,
        //                DFLT_PERSISTENCE_SCHEMA,
        //               DFLT_TABLE_PREFIX);
    }

    private static String getPort() {
        return System.getProperty("HTTP_default.secure", "8020");
    }

    private static URL getURL(String path) throws MalformedURLException {
        URL myURL = new URL("https://localhost:" + getPort() + path);
        System.out.println("Built URL: " + myURL.toString());
        return myURL;
    }

    protected static HttpURLConnection getConnection(String path, int expectedResponseCode, HTTPRequestMethod method, InputStream streamToWrite,
                                                     Map<String, String> map) throws IOException {
        return HttpUtils.getHttpConnection(getURL(path), expectedResponseCode, new int[0], TIMEOUT, method, map, streamToWrite);
    }

    /**
     * Inner class that holds the output from a process.
     */
    private static class ProcessOutput {
        private final List<String> sysout;
        private final List<String> syserr;

        ProcessOutput(Process p) throws Exception {
            sysout = new ArrayList<String>();
            syserr = new ArrayList<String>();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while (br.ready()) {
                sysout.add(br.readLine());
            }
            br.close();

            br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while (br.ready()) {
                syserr.add(br.readLine());
            }
            br.close();
        }

        void printOutput() {
            System.out.println("SYSOUT:");
            for (String x : sysout) {
                System.out.println(" " + x);
            }

            System.out.println("SYSERR:");
            for (String x : syserr) {
                System.out.println(" " + x);
            }
        }

        private static String search(List<String> list, String z) {
            for (String x : list) {
                if (x.contains(z)) {
                    return x;
                }
            }

            return null;
        }

        String getLineInSysoutContaining(String s) {
            return search(sysout, s);
        }
    }

    private ProcessBuilder getProcessBuilder(LibertyServer server) throws Exception {
        String scriptName;
        String serverName = server.getServerName();
        String installRoot = server.getInstallRoot();
        Machine machine = server.getMachine();

        if (machine.getOperatingSystem() == OperatingSystem.WINDOWS) {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen.bat";
        } else {
            scriptName = installRoot + File.separator + "bin" + File.separator + "ddlGen";
        }

        return new ProcessBuilder(scriptName, "generate", serverName).directory(new File(installRoot));
    }

    public String getBatchDDL(LibertyServer server) throws Exception {
        ProcessBuilder processBuilder = getProcessBuilder(server);
        Process process = processBuilder.start();
        int returnCode = process.waitFor();

        if (returnCode != 0)
            throw new Exception("Expected return code 0, actual return code " + returnCode);

        ProcessOutput processOutput = new ProcessOutput(process);
        processOutput.printOutput();

        String successMessage = processOutput.getLineInSysoutContaining("CWWKD0107I");
        if (successMessage == null)
            throw new Exception("Output did not contain success message CWWKD0107I");

        File outputPath = new File(successMessage.substring(72));
        if (outputPath.exists() == false)
            throw new Exception("Output path did not exist: " + outputPath.toString());

        String[] ddlFiles = outputPath.list();
        if (ddlFiles.length == 0)
            throw new Exception("There was no output in the output directory: " + outputPath.toString());

        File ddlFile = null;

        for (String fileName : ddlFiles) {
            if ("databaseStore[BatchDatabaseStore]_batchPersistence.ddl".equals(fileName))
                ddlFile = new File(outputPath, fileName);
        }

        if (!ddlFile.equals(null))
            return ddlFile.getAbsolutePath();
        else
            return null;
    }
}
