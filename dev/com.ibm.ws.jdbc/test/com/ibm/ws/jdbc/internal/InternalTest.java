/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 * Unit test for com.ibm.ws.jdbc.internal package
 */
public class InternalTest {
    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * Test for JDBCDrivers.getDataSourceClassName/getConnectionPoolDataSourceClassName/getXADataSourceClassName(classpath)
     */
    @Test
    public void testInferClassNamesFromFileName() {
        final String m = "testInferClassNamesFromFileName";
        try {
            String name = JDBCDrivers.getDataSourceClassName(Collections.singleton(""));

            // DB2 JCC
            name = JDBCDrivers.getXADataSourceClassName(Arrays.asList("DB2JCC4.JAR", "DB2JCC_LICENSE_CU.JAR"));
            if (!"com.ibm.db2.jcc.DB2XADataSource".equals(name))
                fail("Incorrect XADataSource for db2jcc driver. Result = " + name);

            // Oracle
            name = JDBCDrivers.getConnectionPoolDataSourceClassName(Collections.singleton("OJDBC14_G.JAR"));
            if (!"oracle.jdbc.pool.OracleConnectionPoolDataSource".equals(name))
                fail("Incorrect ConnectionPoolDataSource for oracle driver. Result = " + name);

            // Microsoft SQL Server
            name = JDBCDrivers.getDataSourceClassName(Collections.singleton("SQLJDBC4.JAR"));
            if (!"com.microsoft.sqlserver.jdbc.SQLServerDataSource".equals(name))
                fail("Incorrect DataSource for microsoft driver. Result = " + name);

            // Derby Network Server
            name = JDBCDrivers.getXADataSourceClassName(Collections.singleton("DERBYCLIENT.JAR"));
            if (!name.startsWith("org.apache.derby.jdbc.Client"))
                fail("Incorrect XADataSource for derby network client driver. Result = " + name);

            // Derby Embedded
            name = JDBCDrivers.getDataSourceClassName(Collections.singleton("ORG.APACHE.DB.DERBY_10.5.3.2.1132854.JAR"));
            if (!name.startsWith("org.apache.derby.jdbc.Embedded"))
                fail("Incorrect DataSource for derby embedded driver. Result = " + name);

            // Oracle UCP
            name = JDBCDrivers.getXADataSourceClassName(Arrays.asList("OJDBC6.JAR", "UCP.JAR"));
            if (!"oracle.ucp.jdbc.PoolXADataSourceImpl".equals(name))
                fail("Incorrect XADataSource class for Oracle UCP. Result = " + name);

            // MySQL
            name = JDBCDrivers.getConnectionPoolDataSourceClassName(Collections.singleton("MYSQL-CONNECTOR-JAVA-3.1.14-BIN.JAR"));
            if (!"com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource".equals(name))
                fail("Incorrect ConnectionPoolDataSource class for MySQL. Result = " + name);

            // Unknown
            name = JDBCDrivers.getDataSourceClassName(Collections.singleton("SOMETHING.JAR"));
            if (name != null)
                fail("Should not return data source class names for unrecognized driver. Result = " + name);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test for JDBCDrivers.getDataSourceClassName/getConnectionPoolDataSourceClassName/getXADataSourceClassName(pid)
     */
    @Test
    public void testInferClassNamesFromPID() {
        final String m = "testInferClassNamesFromPID";
        try {
            String name;

            // DB2 JCC
            name = JDBCDrivers.getConnectionPoolDataSourceClassName("com.ibm.ws.jdbc.dataSource.properties.db2.jcc");
            if (!"com.ibm.db2.jcc.DB2ConnectionPoolDataSource".equals(name))
                fail("Incorrect ConnectionPoolDataSource for db2jcc driver. Result = " + name);

            // Oracle
            name = JDBCDrivers.getDataSourceClassName("com.ibm.ws.jdbc.dataSource.properties.oracle");
            if (!"oracle.jdbc.pool.OracleDataSource".equals(name))
                fail("Incorrect DataSource for oracle driver. Result = " + name);

            // Microsoft SQL Server
            name = JDBCDrivers.getXADataSourceClassName("com.ibm.ws.jdbc.dataSource.properties.microsoft.sqlserver");
            if (!"com.microsoft.sqlserver.jdbc.SQLServerXADataSource".equals(name))
                fail("Incorrect XADataSource for microsoft driver. Result = " + name);

            // Derby Network Server
            name = JDBCDrivers.getDataSourceClassName("com.ibm.ws.jdbc.dataSource.properties.derby.client");
            if (!name.startsWith("org.apache.derby.jdbc.Client"))
                fail("Incorrect DataSource for derby network client driver. Result = " + name);

            // Derby Embedded
            name = JDBCDrivers.getConnectionPoolDataSourceClassName("com.ibm.ws.jdbc.dataSource.properties.derby.embedded");
            if (!name.startsWith("org.apache.derby.jdbc.Embedded"))
                fail("Incorrect ConnectionPoolDataSourceDataSource for derby embedded driver. Result = " + name);

            // Oracle UCP
            name = JDBCDrivers.getDataSourceClassName("com.ibm.ws.jdbc.dataSource.properties.oracle.ucp");
            if (!"oracle.ucp.jdbc.PoolDataSourceImpl".equals(name))
                fail("Incorrect DataSource class for Oracle UCP. Result = " + name);

            // Unknown
            name = JDBCDrivers.getDataSourceClassName("com.ibm.ws.jdbc.dataSource.properties");
            if (name != null)
                fail("Should not return data source class names for unrecognized driver. Result = " + name);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
    
    @Test
    public void testURLFiltering() {
        //URL with descriptor beyond vendor
        assertEquals("Test1", "jdbc:oracle:thin:****", 
                     PropertyService.filterURL("jdbc:oracle:thin:username/pass123!@localhost:1521:oracle"));

        //URL with no descriptor beyond vendor, no properties
        assertEquals("Test2", "jdbc:vendor:****", 
                     PropertyService.filterURL("jdbc:vendor:username/pass123!@localhost:1521:oracle"));

        //Test with password in URL, with qualifier following, using semicolon
        assertEquals("Test4", "jdbc:sqlserver:****", 
                     PropertyService.filterURL("jdbc:sqlserver:!@pass://localhost;user=username;password=ab9&*&^*#(*;"));

        //Test with password at very end of URL, using semicolon
        assertEquals("Test5", "jdbc:sqlserver:****", 
                     PropertyService.filterURL("jdbc:sqlserver://localhost/;user=username;password=ab9&*&^*#(*"));
        
        //Driver using mutliple tokens
        assertEquals("Test15", "jdbc:driver:****", 
                     PropertyService.filterURL("jdbc:driver:database5,host=localhost:user=bob,foo=bar,test=2"));
        
        //Uncompliant driver without property
        assertEquals("Test16", "****", 
                     PropertyService.filterURL("this_is_a_very_non_complient_jdbc_url"));
        
        //Uncompliant driver with property
        assertEquals("Test17", "****", 
                     PropertyService.filterURL("this_is_a_very_non_complient_jdbc_url;password=12jsadf")); 
    }
    static String[] expectedConnProps = {
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=unencPassword1;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword =unencPassword2;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword= unencPassword3;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword = unencPassword4;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=password5;",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword =password6",
                                  "javax.net.ssl.keyStorePassword= password7",
                                  "javax.net.ssl.keyStorePassword =password8;",
                                  "javax.net.ssl.keyStorePassword = password9;;",
                                  "oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword = password10;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password5",
                                  "javax.net.ssl.keyStorePassword= password11",
                                  "javax.net.ssl.keyStorePassword =password12;;",
                                  "oracle.net.ssl_version=1.2;javax.net.ssl.keyStorePassword = password13;oracle.net.authentication_services=(TCPS);",
                                  "javax.net.ssl.keyStorePassword = password14  ;oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=password15;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password15",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=password16;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password16",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=password17;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password17",
                                  "oracle.net.ssl_version=1.2;"
    };
    static String[] expectedObfuscatedConnProps = {
                                 "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=*****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword =*****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword= *****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword = *****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=*****;",
                                 "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword =*****",
                                 "javax.net.ssl.keyStorePassword= *****",
                                 "javax.net.ssl.keyStorePassword =*****;",
                                 "javax.net.ssl.keyStorePassword = *****;;",
                                 "oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword = *****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "javax.net.ssl.keyStorePassword= *****",
                                 "javax.net.ssl.keyStorePassword =*****;;",
                                 "oracle.net.ssl_version=1.2;javax.net.ssl.keyStorePassword = *****;oracle.net.authentication_services=(TCPS);",
                                 "javax.net.ssl.keyStorePassword = *****  ;oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);",
                                 "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=*****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=*****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=*****;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=*****",
                                 "oracle.net.ssl_version=1.2;"
   };
    static String[] connProps = {
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=unencPassword1;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword ={xor}KjE6MTwPPiwsKDAtO20=;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword= {aes}AGSgzua84RvVKvqP/N1FhMMbhdoFoht03A+T02KrDZ4QAJngbba3jDbVUVnhGTZ0Ig==;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword = {aes}ANbbM1uZyVm8KVsTXKYzSk1FD/DBj6i3S8Rg3wzsK1hYQUzLebOQRq6rKGN4AuilTA==;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=password5;",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStorePassword =password6",
                                  "javax.net.ssl.keyStorePassword= {aes}ALodREgraDRBHdCzh0ED9GCVJlD5M7EXRHJ06f5dOSYs",
                                  "javax.net.ssl.keyStorePassword =password8;",
                                  "javax.net.ssl.keyStorePassword = {aes}ADrh5wfkAV1Wa4vomPOSApRA9muvAwiO0BQ0Sp3xVMOT;;",
                                  "oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword = {xor}Lz4sLCgwLTtubw==;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password5",
                                  "javax.net.ssl.keyStorePassword= {aes}AHpj0gTs8BZYdZgRMrIQe6hSBksUl6RY5qJBPYLEJFt7",
                                  "javax.net.ssl.keyStorePassword ={xor}Lz4sLCgwLTtubQ==;;",
                                  "oracle.net.ssl_version=1.2;javax.net.ssl.keyStorePassword = {aes}AI9ilTN0D64prTHWyTPVgF2MGmeLfX6gCvSKNphrZBvK;oracle.net.authentication_services=(TCPS);",
                                  "javax.net.ssl.keyStorePassword = {aes}AJsqh5c/+TrDO8Qw+9V12zBh8osrL3+01Nz//lctzD+u  ;oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword={xor}Lz4sLCgwLTtuag==;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword={xor}Lz4sLCgwLTtuag==",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword={xor}Lz4sLCgwLTtuaQ==;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword=password16",
                                  "oracle.net.ssl_version=1.2;oracle.net.authentication_services=(TCPS);javax.net.ssl.keyStore=path-to-keystore/keystore.p12;javax.net.ssl.keyStoreType=PKCS12;javax.net.ssl.keyStorePassword=password17;javax.net.ssl.trustStore= path-to-keystore/keystore.p12;javax.net.ssl.trustStoreType=PKCS12;javax.net.ssl.trustStorePassword={aes}AAu/gbr1pTHWM6fMJVxR79MgjNp4bs2ZKBw97EWi5M23",
                                  "oracle.net.ssl_version=1.2;"
    };
    
    @Test
    public void testOracleConnPropsPwdDecoding() {
        final String testName = "testOracleConnPropsPwdDecoding";
        // compare expected connProp after substitution
        try {
            for (int i = 0; i < connProps.length; i++ ) {
                String result = JDBCDriverService.decodeOracleConnectionPropertiesPwds(connProps[i]);
                assertEquals(testName, expectedConnProps[i], result );
            }            
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName, t);
        }
    }
    
    @Test
    public void testOracleConnPropsPwdObfuscating() {
        String message;
        // compare expected connProp after substitution
        try {
            for (int i = 0; i < connProps.length; i++ ) {
                message = System.lineSeparator() + "input: " + connProps[i] + System.lineSeparator() + "expected: " + expectedObfuscatedConnProps[i];
                String result = PropertyService.filterConnectionProperties(connProps[i]);
                message += System.lineSeparator() + "output: " + result;
                assertEquals(message, expectedObfuscatedConnProps[i], result );
            }            
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testOracleConnPropsPwdObfuscating", t);
        }
    }
}
