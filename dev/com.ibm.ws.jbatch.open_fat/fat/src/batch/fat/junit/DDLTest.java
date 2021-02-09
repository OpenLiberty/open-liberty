/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.dbservlet.DbServletClient;

import batch.fat.util.BatchFATHelper;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class DDLTest extends BatchFATHelper {

    public final static String DROP_DDL = "IdPersistence/batch-jpa-drop.ddl";

    protected static final LibertyServer testServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.jbatch.fat");

    @BeforeClass
    public static void setup() throws Exception {

        Log.info(DDLTest.class, "setup", "Start server.");

        BatchAppUtils.addDropinsDbServletAppWar(server);

        // Start server
        testServer.startServer("DDLTest.log");
        testServer.waitForStringInLog("CWWKF0011I", 20000);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(DDLTest.class, "tearDown", "Stopping server.");

        if (testServer != null && testServer.isStarted()) {
            testServer.stopServer();
        }
    }

    @Test
    public void testddlGenProducesOutput() {
        try {
            // Attempt to create the DDL and get the file path
            String ddlFilePath = getBatchDDL(testServer);

            Assert.assertNotNull("DDL Generation failed.", ddlFilePath);
            File ddlFile = new File(ddlFilePath);
            Assert.assertTrue("DDL Generation did not produce a file", ddlFile.exists() && ddlFile.isFile());
            Assert.assertTrue("DDL Generation produced an empty file", ddlFile.length() > 0);
        } catch (Exception exception) {
            Assert.fail("An exception occurred while generating DDL file.");
        }
    }

    @Test
    public void testddlGenContainsCorrectTables() {
        try {
            // Attempt to create the DDL and get the file path
            String ddlFilePath = getBatchDDL(testServer);

            FileReader fileReader = new FileReader(ddlFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String sqlFromDDL = "";

            while (bufferedReader.ready()) {
                sqlFromDDL += bufferedReader.readLine();
            }

            bufferedReader.close();

            Assert.assertTrue("DDL File is missing table STEPTHREADEXECUTION", sqlFromDDL.contains("CREATE TABLE JBATCH.STEPTHREADEXECUTION"));
            Assert.assertTrue("DDL File is missing table STEPTHREADINSTANCE", sqlFromDDL.contains("CREATE TABLE JBATCH.STEPTHREADINSTANCE"));
            Assert.assertTrue("DDL File is missing table JOBEXECUTION", sqlFromDDL.contains("CREATE TABLE JBATCH.JOBEXECUTION"));
            Assert.assertTrue("DDL File is missing table JOBINSTANCE", sqlFromDDL.contains("CREATE TABLE JBATCH.JOBINSTANCE"));

        } catch (Exception exception) {
            Assert.fail("Test failed due to an unexpected exception.");
        }
    }

    //@Test
    public void testddlGenIsValidSQL() throws Exception {
        // Attempt to create the DDL and get the file path
        String ddlFilePath = getBatchDDL(testServer);

        File ddlFile = new File(ddlFilePath);
        File updatedddlFile = new File(ddlFilePath + ".tmp");
        BufferedReader reader = new BufferedReader(new FileReader(ddlFilePath));
        PrintWriter writer = new PrintWriter(new FileWriter(updatedddlFile));
        while (reader.ready()) {
            String line = reader.readLine();
            if (line.endsWith(";"))
                writer.println(line);
            else
                writer.println(line + ";");
        }
        writer.close();
        reader.close();
        ddlFile.delete();
        updatedddlFile.renameTo(ddlFile);

        // Determine authentication info for dataSource
        String userName = "user";
        String password = "pass";

        ServerConfiguration configuration = testServer.getServerConfiguration();
        ConfigElementList<DataSource> dataSourcesList = configuration.getDataSources();
        Iterator<DataSource> dataSourcesListIterator = dataSourcesList.iterator();

        while (dataSourcesListIterator.hasNext()) {
            DataSource dataSource = dataSourcesListIterator.next();

            if (dataSource.getJndiName().equals("jdbc/batch")) {
                Set<DataSourceProperties> dataSourcePropertiesList = dataSource.getDataSourceProperties();
                Iterator<DataSourceProperties> dataSourcePropertiesListIterator = dataSourcePropertiesList.iterator();

                while (dataSourcePropertiesListIterator.hasNext()) {
                    DataSourceProperties dataSourceProperties = dataSourcePropertiesListIterator.next();
                    userName = dataSourceProperties.getUser();
                    password = dataSourceProperties.getPassword();
                    break;
                }
            }

            if (!userName.equals("user"))
                break;
        }

        // Drop any existing tables
        loadAndExecuteSql(DFLT_PERSISTENCE_JNDI, DROP_DDL, DFLT_PERSISTENCE_SCHEMA, DFLT_TABLE_PREFIX);

        // Attempt to execute the generated SQL
        DbServletClient dbserv = new DbServletClient();
        HttpURLConnection conn = dbserv.setDataSourceJndi(DFLT_PERSISTENCE_JNDI).setDataSourceUser(userName,
                                                                                                   password).setHostAndPort(testServer.getHostname(),
                                                                                                                            testServer.getHttpDefaultPort()).loadSql(ddlFilePath,
                                                                                                                                                                     "",
                                                                                                                                                                     "").executeUpdate();

        Assert.assertTrue("DbServlet returned " + conn.getResponseCode() + " " + conn.getResponseMessage() + " " + conn.getHeaderFields(),
                          conn.getResponseCode() == 200);

    }

    @Test
    public void testBadDDL() {
        String ddlFilePath = null;
        try {
            // Attempt to create the DDL and get the file path
            ddlFilePath = getBatchDDL(testServer);

            // Determine authentication info for dataSource
            String userName = "user";
            String password = "pass";

            ServerConfiguration configuration = testServer.getServerConfiguration();
            ConfigElementList<DataSource> dataSourcesList = configuration.getDataSources();
            Iterator<DataSource> dataSourcesListIterator = dataSourcesList.iterator();

            while (dataSourcesListIterator.hasNext()) {
                DataSource dataSource = dataSourcesListIterator.next();

                if (dataSource.getJndiName().equals("jdbc/batch")) {
                    Set<DataSourceProperties> dataSourcePropertiesList = dataSource.getDataSourceProperties();
                    Iterator<DataSourceProperties> dataSourcePropertiesListIterator = dataSourcePropertiesList.iterator();

                    while (dataSourcePropertiesListIterator.hasNext()) {
                        DataSourceProperties dataSourceProperties = dataSourcePropertiesListIterator.next();
                        userName = dataSourceProperties.getUser();
                        password = dataSourceProperties.getPassword();
                        break;
                    }
                }

                if (!userName.equals("user"))
                    break;
            }

            // Attempt to execute the generated SQL.
            // The SQL has no semicolons and should therefore fail to execute.
            HttpURLConnection con = new DbServletClient().setDataSourceJndi("jdbc/batch").setDataSourceUser(userName,
                                                                                                            password).setHostAndPort(testServer.getHostname(),
                                                                                                                                     testServer.getHttpDefaultPort()).loadSql(ddlFilePath,
                                                                                                                                                                              "",
                                                                                                                                                                              "").executeUpdate();

            int rc = con.getResponseCode();

            Assert.assertTrue("Got a good return code when a bad return code was expected.", 200 != rc);
        } catch (Exception exception) {
            Assert.fail("Encountered an unexpected exception.");
        }
    }
}
