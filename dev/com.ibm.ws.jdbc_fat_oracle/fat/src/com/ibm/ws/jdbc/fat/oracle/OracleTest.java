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

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import oracle.jdbc.pool.OracleDataSource;
import web.OracleTestServlet;

@RunWith(FATRunner.class)
public class OracleTest extends FATServletClient {

    public static final String JEE_APP = "oraclejdbcfat";
    public static final String SERVLET_NAME = "OracleTestServlet";
    public static final String SSL_PASSWORD = "{xor}Oz0vKDtu";

    @Server("com.ibm.ws.jdbc.fat.oracle")
    @TestServlet(servlet = OracleTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;
    
    //TODO replace this container with the official oracle-xe container if/when it is available without a license
    @ClassRule
    public static OracleContainer oracle = new OracleContainer("oracleinanutshell/oracle-xe-11g").withLogConsumer(OracleTest::log);
    
    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(OracleTest.class, "oracle", msg);
    }

    @BeforeClass
    public static void setUp() throws Exception {
    	// Set server environment variables
        server.addEnvVar("URL", oracle.getJdbcUrl());
        server.addEnvVar("USER", oracle.getUsername());
        server.addEnvVar("PASSWORD", oracle.getPassword());
        server.addEnvVar("DBNAME", "XE");
        server.addEnvVar("PORT", Integer.toString(oracle.getFirstMappedPort()));
        server.addEnvVar("HOST", oracle.getContainerIpAddress());
        server.addEnvVar("SSL_PASSWORD", SSL_PASSWORD);

    	// Create a normal Java EE application and export to server
    	ShrinkHelper.defaultApp(server, JEE_APP, "web");

    	// Start Server
        server.startServer();

        // Create database tables
        initDatabaseTables();
    }

    @AfterClass
    public static void tearDown() throws Exception {
    	testSSLPasswordIsNotLogged();
    	testURLPasswordIsNotLogged();
    	
        if (server.isStarted()) {
            server.stopServer();
        }
    }
    
    //FIXME com.ibm.ws.jdbc.* does not log this password, but it is still being logged 
    //by OSGi Declaritive Service.  Enable this test once that is no longer the case. 
    private static void testSSLPasswordIsNotLogged() throws Exception {
    	if(server.isStarted()) {
    		//Allow in output.txt log as we use env variables to set SSL_PASSWORD and URL
    		int count = server.findStringsInTrace(Pattern.quote(SSL_PASSWORD)).size();
    		
    		if(count != 0) {
    			fail("Found SSL password being traced in log or trace.  Occurrences: " + count);
    		}
    	}
    }
    
    //FIXME com.ibm.ws.jdbc.* does not log the URL with password, but it is still being logged 
    //by OSGi Declaritive Service.  Enable this test once that is no longer the case. 
    private static void testURLPasswordIsNotLogged() throws Exception {
    	if(server.isStarted()) {
    		//Allow in output.txt log as we use env variables to set SSL_PASSWORD and URL
    		int count = server.findStringsInTrace(Pattern.quote(oracle.getJdbcUrl())).size();
    		
    		if(count != 0) {
    			fail("Found JDBC URL being traced with password in log or trace.  Occurrences: " + count);
    		}
    	}
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
		
    	try (Connection conn = ds.getConnection()){
            Statement stmt = conn.createStatement();
            
            //Create MYTABLE for OracleTest.class
            try {
                stmt.execute("DROP TABLE MYTABLE");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE MYTABLE (ID NUMBER NOT NULL PRIMARY KEY, STRVAL NVARCHAR2(40))");

            //Create CONCOUNT for OracleTest.class
            try {
                stmt.execute("DROP TABLE CONCOUNT");
            } catch (SQLException x) {
                // probably didn't exist
            }
            stmt.execute("CREATE TABLE CONCOUNT (NUMCONNECTIONS NUMBER NOT NULL)");
            stmt.execute("INSERT INTO CONCOUNT VALUES(0)");

            //Close statement
            stmt.close();
    	}
    }
}
