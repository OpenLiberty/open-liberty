/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package io.openliberty.jpa.emlocking;

import static com.ibm.websphere.simplicity.config.DataSourceProperties.DB2_JCC;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.ORACLE_JDBC;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.dsprops.testrules.SkipIfDataSourceProperties;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EMLockingTest extends FATServletClient {

    @Server("com.ibm.ws.jpa.fat.emlocking")
    public static LibertyServer server;

    static final String APP_NAME = "emlocking";
    static final String SERVLET_NAME = "EMLockingTestServlet";
    public static final String JPA_20 = "jpa-2.0";
    public static final String JPA_21 = "jpa-2.1";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")//
                        .addPackage("com.ibm.ws.jpa.fat.emlocking.entity")//
                        .addPackage("com.ibm.ws.jpa.fat.emlocking.web");//
//                        .addAsWebInfResource(new File("test-applications/" + JEE_APP + "/resources/META-INF/persistence.xml"));
        ShrinkHelper.addDirectory(app, "test-applications/" + APP_NAME + "/resources/");
        ShrinkHelper.exportToServer(server, "apps", app);

//        server.configureForAnyDatabase();
//        setJPALevel(server, JPA_21);
    }

    public void commonSetup() throws Exception {
        // This chunk is effectively the @BeforeClass bit because we can't
        // properly override static methods.  Leave the @BC for setting jpa level
        server.addInstalledAppForValidation(APP_NAME);
        server.startServer(getClass().getSimpleName() + ".log");
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("OptimisticLockException",
                          "org.eclipse.persistence.exceptions.DatabaseException",
                          "Failed to find MBean Server");
    }

    @Test
    // Skip for Oracle because they do not support TransactionIsolation=read-uncommitted
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario01_CMTS_Annotated_NoLock() throws Exception {
        runTest("testScenario01_CMTS_Annotated_NoLock");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario01_CMTS_Annotated_Optimistic() throws Exception {
        runTest("testScenario01_CMTS_Annotated_Optimistic");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario01_CMTS_Annotated_Optimistic_Force() throws Exception {
        runTest("testScenario01_CMTS_Annotated_Optimistic_Force");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario01_CMTS_Annotated_Pessimistic_Force() throws Exception {
        runTest("testScenario01_CMTS_Annotated_Pessimistic_Force");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario01_CMTS_Annotated_Pessimistic_Read() throws Exception {
        runTest("testScenario01_CMTS_Annotated_Pessimistic_Read");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario01_CMTS_Annotated_Pessimistic_Write() throws Exception {
        runTest("testScenario01_CMTS_Annotated_Pessimistic_Write");
    }

    @Test
    @SkipIfDataSourceProperties({ ORACLE_JDBC, DB2_JCC })
    public void testScenario02_CMTS_Annotated_NoLock() throws Exception {
        runTest("testScenario02_CMTS_Annotated_NoLock");
    }

    @Test
    @SkipIfDataSourceProperties({ ORACLE_JDBC, DB2_JCC })
    public void testScenario02_CMTS_Annotated_Optimistic() throws Exception {
        runTest("testScenario02_CMTS_Annotated_Optimistic");
    }

    @Test
    @SkipIfDataSourceProperties({ ORACLE_JDBC, DB2_JCC })
    public void testScenario02_CMTS_Annotated_Optimistic_Force() throws Exception {
        runTest("testScenario02_CMTS_Annotated_Optimistic_Force");
    }

    @Test
    @SkipIfDataSourceProperties({ ORACLE_JDBC, DB2_JCC })
    public void testScenario02_CMTS_Annotated_Pessimistic_Force() throws Exception {
        runTest("testScenario02_CMTS_Annotated_Pessimistic_Force");
    }

    @Test
    @SkipIfDataSourceProperties({ ORACLE_JDBC, DB2_JCC })
    public void testScenario02_CMTS_Annotated_Pessimistic_Read() throws Exception {
        runTest("testScenario02_CMTS_Annotated_Pessimistic_Read");
    }

    @Test
    @SkipIfDataSourceProperties({ ORACLE_JDBC, DB2_JCC })
    public void testScenario02_CMTS_Annotated_Pessimistic_Write() throws Exception {
        runTest("testScenario02_CMTS_Annotated_Pessimistic_Write");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario03_CMTS_Annotated_NoLock() throws Exception {
        runTest("testScenario03_CMTS_Annotated_NoLock");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario03_CMTS_Annotated_Optimistic() throws Exception {
        runTest("testScenario03_CMTS_Annotated_Optimistic");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario03_CMTS_Annotated_Optimistic_Force() throws Exception {
        runTest("testScenario03_CMTS_Annotated_Optimistic_Force");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario03_CMTS_Annotated_Pessimistic_Force() throws Exception {
        runTest("testScenario03_CMTS_Annotated_Pessimistic_Force");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario03_CMTS_Annotated_Pessimistic_Read() throws Exception {
        runTest("testScenario03_CMTS_Annotated_Pessimistic_Read");
    }

    @Test
    @SkipIfDataSourceProperties(ORACLE_JDBC)
    public void testScenario03_CMTS_Annotated_Pessimistic_Write() throws Exception {
        runTest("testScenario03_CMTS_Annotated_Pessimistic_Write");
    }

    /**
     * Start the server if it's not started yet (can't be done in @BeforeClass
     * due to static method inheritance mechanics).
     * Then run the test in the server associated with APP_NAME and SERVLET_NAME
     */
    public void runTest(String testMethodName) throws Exception {
        if (!server.isStarted())
            commonSetup();

        FATServletClient.runTest(server, APP_NAME + '/' + SERVLET_NAME, testMethodName);
    }

}
