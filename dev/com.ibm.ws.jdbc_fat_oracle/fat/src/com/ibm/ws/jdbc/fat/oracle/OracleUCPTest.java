/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.OracleContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ConnectionManager;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties_oracle_ucp;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import oracle.jdbc.pool.OracleDataSource;
import ucp.web.OracleUCPTestServlet;

@RunWith(FATRunner.class)
public class OracleUCPTest extends FATServletClient {

    public static final String JEE_APP = "oracleucpfat";
    public static final String SERVLET_NAME = "OracleUCPTestServlet";

    @Server("com.ibm.ws.jdbc.fat.oracle.ucp")
    @TestServlet(servlet = OracleUCPTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;
    
    //TODO replace this container with the official oracle-xe container if/when it is available without a license
    public static OracleContainer oracle = FATSuite.oracle;

    @BeforeClass
    public static void setUp() throws Exception {
    	// Set server environment variables
        server.addEnvVar("URL", oracle.getJdbcUrl());
        server.addEnvVar("USER", oracle.getUsername());
        server.addEnvVar("PASSWORD", oracle.getPassword());
        
    	// Create a normal Java EE application and export to server
    	ShrinkHelper.defaultApp(server, JEE_APP, "ucp.web");

    	// Start Server
        server.startServer();

        // Create database tables
        initDatabaseTables();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer("CWWKE0701E"); // CWWKE0701E expected in testOracleUCPConnectionPoolDS
    }
    
    private static void initDatabaseTables() throws SQLException {
		Properties connProps = new Properties();
		// This property prevents "ORA-01882: timezone region not found" errors due to the Oracle DB not understanding 
		// some time zones(specifically those used by our RHEL 6 test systems).
		connProps.put("oracle.jdbc.timezoneAsRegion", "false");
		
		OracleDataSource ds = new OracleDataSource();
		ds.setConnectionProperties(connProps);
		ds.setUser(oracle.getUsername());
		ds.setPassword(oracle.getPassword());
		ds.setURL(oracle.getJdbcUrl());
		
    	try (Connection conn = ds.getConnection()) {
            Statement stmt = conn.createStatement();
            
            //Create COLORTABLE for OracleUCPTest.class
            try {
                stmt.execute("DROP TABLE COLORTABLE");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE COLORTABLE (ID NUMBER NOT NULL PRIMARY KEY, COLOR NVARCHAR2(40))");
            PreparedStatement ps = conn.prepareStatement("INSERT INTO COLORTABLE VALUES(?,?)");
            ps.setInt(1, 1);
            ps.setString(2, "maroon");
            ps.executeUpdate();
            ps.close();

            //Close statement
            stmt.close();
    	}
    }

    /**
     * Config update test which tests that a datasource configured initially to use the Liberty connection pool
     * can be updated to use UCP and the Liberty connection pool is disabled. The test then switches the config back to
     * the Liberty conn pool and again checks that UCP is not being used.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUpdateLibertyConnPoolToUCP() throws Exception {
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testUsingLibertyConnPool");

        ServerConfiguration initialConfig = server.getServerConfiguration().clone();

        ServerConfiguration config = server.getServerConfiguration().clone();
        DataSource oracleDS = config.getDataSources().getBy("id", "oracleDS");
        oracleDS.clearDataSourceDBProperties();
        ConfigElementList<Properties_oracle_ucp> propsList = oracleDS.getProperties_oracle_ucp();
        Properties_oracle_ucp props = new Properties_oracle_ucp();
        propsList.add(props);
        try {
            //update to UCP
            props.setMaxPoolSize("2");
            props.setUser("${env.USER}");
            props.setPassword("${env.PASSWORD}");
            props.setURL("${env.URL}");
            props.setConnectionWaitTimeout("30");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));

            //Ensure we are now using UCP
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testUsingUCP");

        } finally {
            //Update config back to a non UCP
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(initialConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));

            //Ensure we have switched back
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testUsingLibertyConnPool");
        }
    }

    /**
     * Config update test which tests that after updating the properties of a datasouce using UCP the datasource is still
     * using UCP and Liberty connection pooling is still disabled.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testConnectionManagerUCPtoUCP() throws Exception {
        ServerConfiguration initialConfig = server.getServerConfiguration().clone();

        //use ucpDS to ensure it is initialized
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCP");

        ServerConfiguration config = server.getServerConfiguration().clone();
        DataSource ucpDS = config.getDataSources().getBy("id", "ucpDS");
        Properties_oracle_ucp props = ucpDS.getProperties_oracle_ucp().get(0);
        try {
            //update UCP Prop
            props.setMaxIdleTime("400");;

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));

            //Ensure we are still using UCP
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPMaxConnections");

        } finally {
            //Update config back
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(initialConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));
        }
    }

    /**
     * Config update test which tests that after updating the connection manager reference of a datasouce using UCP the datasource is still
     * using UCP and Liberty connection pooling is still disabled. We have a test for updating both the connection manager reference
     * and embedded connection manager as the code paths are slightly different.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUCPUpdateConnManager() throws Exception {
        ServerConfiguration initialConfig = server.getServerConfiguration().clone();

        //use ucpDS to ensure it is initialized
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCP");

        ServerConfiguration config = server.getServerConfiguration().clone();
        ConnectionManager conMgr = config.getConnectionManagers().getBy("id", "conMgr");
        try {
            //this update to the connection manager should not result in any functional updates
            //since the maxpoolsize is ignored
            conMgr.setMaxPoolSize("1");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));

            //Ensure we are still using UCP

            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPMaxConnections");

        } finally {
            //Update config back
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(initialConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));
        }
    }

    /**
     * Config update which tests that after updating the embedded connection manager of a datasouce using UCP the datasource is still
     * using UCP and Liberty connection pooling is still disabled. We have a test for updating both the connection manager reference
     * and embedded connection manager as the code paths are slightly different.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUCPUpdateConnManagerEmbedded() throws Exception {
        ServerConfiguration initialConfig = server.getServerConfiguration().clone();

        //use ucpDS to ensure it is initialized
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPEmbeddedConMgr");

        ServerConfiguration config = server.getServerConfiguration().clone();
        DataSource ucpDS = config.getDataSources().getBy("id", "ucpDSEmbeddedConMgr");
        ConnectionManager conMgr = ucpDS.getConnectionManagers().get(0);
        try {
            //this update to the connection manager should not result in any functional updates
            //since the maxpoolsize is ignored
            conMgr.setMaxPoolSize("1");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));

            //Ensure we are still using UCP
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPMaxConnectionsEmbedded");

        } finally {
            //Update config back
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(initialConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));
        }
    }

    /**
     * Config update test which switches a UCP datasource's connection manager from an embedded connection manager
     * to a reference and ensures that Liberty's connection pooling is still disabled. Switches back to the embedded
     * connection manager and again ensures that Liberty's connection pooling is still disabled.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUpdateEmbedConMgrtoRef() throws Exception {
        ServerConfiguration initialConfig = server.getServerConfiguration().clone();

        //use ucpDS to ensure it is initialized
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPEmbeddedConMgr");

        ServerConfiguration config = server.getServerConfiguration().clone();
        DataSource ucpDS = config.getDataSources().getBy("id", "ucpDSEmbeddedConMgr");
        try {
            //this update to the connection manager should not result in any functional updates
            //since the maxpoolsize is ignored
            ucpDS.getConnectionManagers().remove(0);
            ucpDS.setConnectionManagerRef("conMgrUpdate");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));

            //Ensure we are still using UCP
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPMaxConnectionsEmbedded");

        } finally {
            //Update config back
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(initialConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));

            //Ensure we are still using UCP
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPMaxConnectionsEmbedded");
        }
    }

    /**
     * Config update test which tests that when any overridden data source properties are updated
     * when using UCP that Liberty's connection pooling is still disabled.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUpdateOnlyIgnoredDSPropsWithUCP() throws Exception {
        ServerConfiguration initialConfig = server.getServerConfiguration().clone();
        //use ucpDS to ensure it is initialized
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCP");
        ServerConfiguration config = server.getServerConfiguration().clone();
        DataSource ucpDS = config.getDataSources().getBy("id", "ucpDS");
        try {
            //this update to the statementcachesize should not result in any functional updates
            //since the property is ignored
            ucpDS.setStatementCacheSize("20");
            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));
            //Ensure we are now using UCP
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPMaxConnections");
        } finally {
            //Update config back
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(initialConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));
        }
    }

    /**
     * Config update test which tests that when a datasource type is updated when using
     * UCP that the impl class is updated to the correct class.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUpdateDSType() throws Exception {
        ServerConfiguration initialConfig = server.getServerConfiguration().clone();
        //use ucpXADS to ensure it is initialized
        runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPXADS");
        ServerConfiguration config = server.getServerConfiguration().clone();
        DataSource ucpDS = config.getDataSources().getBy("id", "ucpXADS");
        try {
            //Update to javax.sql.DataSource
            ucpDS.setType("javax.sql.DataSource");
            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testUsingPoolDataSource");
        } finally {
            //Update config back
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(initialConfig);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton(JEE_APP));
            //Test we are again using XA
            runTest(server, JEE_APP + '/' + SERVLET_NAME, "testOracleUCPXADS");
        }

    }
}
