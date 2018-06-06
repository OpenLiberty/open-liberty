/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.derby;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.derby.web.JDBCDerbyServlet;

@RunWith(FATRunner.class)
public class JDBCDerbyTest extends FATServletClient {

    private static final Class<?> c = JDBCDerbyTest.class;

    private static final String jdbcapp = "jdbcapp";
    private static final String jdbcappfat = "jdbcapp/JDBCDerbyServlet";

    @Server("com.ibm.ws.jdbc.fat.derby")
    @TestServlet(servlet = JDBCDerbyServlet.class, contextRoot = jdbcapp)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, jdbcapp, "jdbc.fat.derby.web");

        JavaArchive tranNoneDriver = ShrinkWrap.create(JavaArchive.class, "trandriver.jar").addPackage("jdbc.tran.none.driver");
        ShrinkHelper.exportToServer(server, "../../shared/resources/derby", tranNoneDriver);

        server.configureForAnyDatabase();
        server.addInstalledAppForValidation("jdbcapp");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E", //expected by testTransactionalSetting
                          "DSRA4011E", //expected by testTNConfigIsoLvlReverse
                          "SRVE0319E", //expected by testTNConfigTnsl
                          "WTRN0017W"); //expected by testSuspendedUserTran
    }

    /**
     * Update the data source configuration while the server is running.
     * Ensure when using a data source configured with an isolation level of TRANSACTION_NONE
     * that when updating to a data source with Transactional = true the change is prevented.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testTNConfigTnsl() throws Throwable {
        String method = "testTNConfigTnsl";
        Log.info(c, method, "Executing " + method);

        //First check that we are not enlisting in transactions
        runTest(server, jdbcappfat, "testTNTransationEnlistment");

        ServerConfiguration config = server.getServerConfiguration();
        DataSource ds8 = config.getDataSources().getBy("id", "dsfat8");

        try {
            ds8.setTransactional("true");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(new TreeSet<String>(Arrays.asList(jdbcapp)));

            //Ensure that updating the config results in servlet class being found, but a resource injection failure to occur.
            runTest(server, jdbcappfat, "testTNTransationEnlistmentModified");
        } catch (Exception e) {
            fail("Exception should not have been thrown when switching transactional property.");
        } finally {
            //Attempt to switch back
            ds8.setTransactional("false");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(new TreeSet<String>(Arrays.asList(jdbcapp)));

            //Ensure that we were able to switch back
            runTest(server, jdbcappfat, "testTNTransationEnlistment");
        }
    }

    /**
     * Update the data source configuration while the server is running.
     * Ensure that updating from a data source configured with an isolation level of TRANSACTION_NONE
     * to a data source configured with a different isolation level does not fail.
     * And, ensure that switching the other way is prevented.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testTNConfigIsoLvl() throws Throwable {
        String method = "testTNConfigIsoLvl";
        Log.info(c, method, "Executing " + method);

        //First ensure we are using a data source with an isolation level of TRANSACTION_NONE
        runTest(server, jdbcappfat, "testTNOriginalIsoLvl");

        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsX = config.getDataSources().getBy("id", "dsfatX");

        try {
            dsX.setIsolationLevel("TRANSACTION_SERIALIZABLE");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(new TreeSet<String>(Arrays.asList(jdbcapp)));

            //Check update was successful and we are now using the updated transaction isolation level
            runTest(server, jdbcappfat, "testTNModifiedIsoLvl");
        } catch (Exception e) {
            fail("Exception should not have been thrown when switching isolation level from TRANSACTION_NONE.");
        }

        try {
            //Attempt to switch back to TRANSACTION_NONE
            dsX.setIsolationLevel("TRANSACTION_NONE");

            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(new TreeSet<String>(Arrays.asList(jdbcapp)));

            //Ensure that trying to get a connection fails since we cannot set isolation level to TRANSACTION_NONE
            runTest(server, jdbcappfat, "testTNRevertedIsoLvl");
        } catch (Exception e) {
            fail("Exception should not have been thrown when switching isolation level to TRANSACTION_NONE.");
        }
    }

    /**
     * Ensure that attempting to get more than one connection per data source
     * throws the J2CA0086 warning.
     */
    @Test
    public void testMultipleConnections() throws Throwable {
        List<String> beginResults = server.findStringsInTrace("J2CA0086");
        runTest(server, jdbcappfat, "testMultipleConnections");
        List<String> endResults = server.findStringsInTrace("J2CA0086");
        for (String result : beginResults) {
            endResults.remove(result);
        }
        assertTrue("J2CA0086 Warning should have sounded.", !endResults.isEmpty());
    }
}
