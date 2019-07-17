/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat;

import static componenttest.annotation.SkipIfSysProp.DB_Informix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URLEncoder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties_informix;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.DatabaseCluster;
import componenttest.topology.database.DerbyEmbeddedUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.SchedulerFATServlet;

@RunWith(FATRunner.class)
@SkipIfSysProp(DB_Informix) //persistent executor is not supported on Informix
public class SchedulerTest extends FATServletClient {

    private static final String APP_NAME = "schedtest";

    @Server("com.ibm.ws.concurrent.persistent.fat")
    @TestServlet(servlet = SchedulerFATServlet.class, path = APP_NAME)
    public static LibertyServer server;

    /**
     * Before running any tests, start the server
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
    	ShrinkHelper.defaultDropinApp(server, APP_NAME, "web");
        if ("Derby".equalsIgnoreCase(System.getProperty("fat.bucket.db.type", "Derby")))
            DerbyEmbeddedUtilities.createDB(server, "TaskStoreDS", "userx", "passx");

        // Update the server configuration artifacts if the bootstrapping.properties file was
        // populated with relevant database information.
        ServerConfiguration config = server.getServerConfiguration();
        config.updateDatabaseArtifacts();

        // Set the lock_mode_wait property if dealing with informix jdbc.
        ConfigElementList<DatabaseStore> dbStores = config.getDatabaseStores();
        DatabaseStore dbStore = dbStores.get(0);
        DataSource ds = dbStore.getDataSources().get(0);
        ConfigElementList<Properties_informix> informixProps = ds.getProperties_informix();
        if (informixProps != null && !informixProps.isEmpty()) {
            Properties_informix properties = informixProps.get(0);
            properties.setIfxIFX_LOCK_MODE_WAIT("30");
        }

        server.updateServerConfiguration(config);
        server.startServer();

        // Go generate the DDL required
        DDLGenScriptHelper helper = new DDLGenScriptHelper(server);
        helper.getDebugInfo();
        String ddlFile = helper.getPersistentExecutorDDL();

        // Go figure out whether we're going to run the DDL in Derby or somewhere else.
        // When this was written, Derby was not supported by the fat bucket tooling that
        // supports the other databases.  When Derby is supported (either embedded or
        // network), we should remove the special-case Derby code.
        Bootstrap bs = Bootstrap.getInstance();
        String dbVendor = bs.getValue(BootstrapProperty.DB_VENDORNAME.getPropertyName());

        if (dbVendor == null) {
            // There is currently only one databaseStore defined. DDL table creation
            // should only take place if the createTables attribute is set to false.
            String createTables = dbStores.get(0).getCreateTables();
            if (createTables != null && createTables.equalsIgnoreCase("false")) {
                runTest(server, APP_NAME, "createTablesDerby&fileName=" + URLEncoder.encode(ddlFile, "UTF-8"));
                server.stopServer("DSRA0174W");
            }
        } else {
            server.stopServer("DSRA0174W",
                              "DSRA1300E" /* Sybase does not implement Connection.getClientInfo */);

            String ddlDirectoryName = dbVendor.toLowerCase();
            String workingDirectory = System.getProperty("user.dir");
            File ddlDirectory = new File(workingDirectory + File.separator + "ddl" + File.separator + ddlDirectoryName);
            if (ddlDirectory.exists() == false) {
                if (ddlDirectory.mkdirs() == false) {
                    throw new Exception("DDL directory could not be created: " + ddlDirectory.getAbsolutePath());
                }
            }

            // Copy the DDL file to the proper place.
            BufferedReader br = new BufferedReader(new FileReader(ddlFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(ddlDirectory, "persistentExecutor.ddl")));
            while (br.ready()) {
                String line = br.readLine();
                System.out.println("Writing DDL command: " + line);
                bw.write(line);
                bw.newLine();
            }

            bw.newLine();
            bw.close();
            br.close();

            // Drive the database tool to go run the DDL against the database.
            // There is currently only one databaseStore defined. DDL table creation
            // should only take place if the createTables attribute is set to false.
            String createTables = dbStores.get(0).getCreateTables();
            if (createTables != null && createTables.equalsIgnoreCase("false")) {
                new DatabaseCluster().runDDL();
            }
        }

        // We should be good to go, so re-start the server.
        if (!server.isStarted()) {
            server.startServer();
        }
    }

    /**
     * After completing all tests, stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        try {
            runTest(server, APP_NAME, "verifyNoTasksRunning");
        } finally {
            if (server != null && server.isStarted())
                server.stopServer("CWWKC1500W",
                                  "CWWKC1510W",
                                  "DSRA0174W",
                                  "DSRA1300E" /* Sybase does not implement Connection.getClientInfo */);
        }
    }
}