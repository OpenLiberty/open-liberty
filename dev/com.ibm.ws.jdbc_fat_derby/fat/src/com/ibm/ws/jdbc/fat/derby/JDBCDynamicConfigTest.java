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

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JDBCDynamicConfigTest extends FATServletClient {

    private static final String jdbcappfat = FATSuite.jdbcapp + "/JDBCDerbyServlet";
    private static final Set<String> APP_SET = Collections.singleton(FATSuite.jdbcapp);

    @Server(FATSuite.SERVER)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E", //expected by testTransactionalSetting
                          "DSRA4011E", //expected by testTNConfigIsoLvlReverse
                          "SRVE0319E"); //expected by testTNConfigTnsl
    }

    /**
     * Update the data source configuration while the server is running.
     * Ensure when using a data source configured with an isolation level of TRANSACTION_NONE
     * that when updating to a data source with Transactional = true the change is prevented.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException" })
    public void testTNConfigTnsl() throws Throwable {
        //First check that we are not enlisting in transactions
        runTest(server, jdbcappfat, "testTNTransationEnlistment");

        ServerConfiguration config = server.getServerConfiguration();
        DataSource ds8 = config.getDataSources().getBy("id", "dsfat8");

        try {
            ds8.setTransactional("true");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_SET);

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
            server.waitForConfigUpdateInLogUsingMark(APP_SET);

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
        //First ensure we are using a data source with an isolation level of TRANSACTION_NONE
        runTest(server, jdbcappfat, "testTNOriginalIsoLvl");

        ServerConfiguration config = server.getServerConfiguration();
        DataSource dsX = config.getDataSources().getBy("id", "dsfatX");

        try {
            dsX.setIsolationLevel("TRANSACTION_SERIALIZABLE");

            //Update config
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(APP_SET);

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
            server.waitForConfigUpdateInLogUsingMark(APP_SET);

            //Ensure that trying to get a connection fails since we cannot set isolation level to TRANSACTION_NONE
            runTest(server, jdbcappfat, "testTNRevertedIsoLvl");
        } catch (Exception e) {
            fail("Exception should not have been thrown when switching isolation level to TRANSACTION_NONE.");
        }
    }
}
