/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.junit;

import java.util.Collections;
import java.util.concurrent.Callable;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jbatch.test.BatchAppUtils;
import com.ibm.ws.jbatch.test.FatUtils;

import batch.fat.util.BatchFATHelper;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 * Test dynamic updates to JDBC config using various schemas and tablePrefixes.
 *
 */
@RunWith(FATRunner.class)
@CheckpointTest
public class JdbcConfigTestCheckpoint extends BatchFATHelper { 

    /**
     * This ClassRule will run all the tests in this class multiple times, against
     * all the given server.xml configuration files.
     *
     * Use setInitialSetup/setFinalTearDown to run logic before/after ALL
     * tests (against all configurations) have run.
     */
    @ClassRule
    public static DynamicConfigRuleCheckpoint dynamicConfigRuleCheckpoint = new DynamicConfigRuleCheckpoint() //
                        .setInitialSetup(JdbcConfigTestCheckpoint::setup) //
                        .setBeforeEach(JdbcConfigTestCheckpoint::beforeEach) //
                        .setFinalTearDown(JdbcConfigTestCheckpoint::tearDown) //
                        .addServerXml("JDBCPersistenceCheckpoint/jdbc.config.myschema1.server.xml") //
                        .addServerXml("JDBCPersistenceCheckpoint/jdbc.config.myschema2.server.xml") //
                        .addServerXml("JDBCPersistenceCheckpoint/jdbc.config.myschema1.tp1.server.xml") //
                        .addServerXml("JDBCPersistenceCheckpoint/jdbc.config.myschema1.tp2.server.xml");
    /**
     * Start the server and setup the DB.
     */
    public static LibertyServer setup() throws Exception {

        log("setup", "start server and execute DDLs");

        server = LibertyServerFactory.getLibertyServer("batchFAT");
        BatchAppUtils.addDropinsBatchFATWar(server);
        BatchAppUtils.addDropinsBonusPayoutWar(server);
        BatchAppUtils.addDropinsDbServletAppWar(server);
        
        // Disabling security test for checkpoint
        FATSuite.configureBootStrapProperties(server, Collections.singletonMap("websphere.java.security.exempt","true"));
    
        // Start server
        server.setServerConfigurationFile("JDBCPersistenceCheckpoint/jdbc.config.myschema1.server.xml");
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer("JdbcConfigTestCheckpoint.log");
        return server;
    }

    /**
     * Stop the server.
     */
    public static void tearDown() throws Exception {
        log("tearDown", "stopping server");
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Do a Checkpoint restore and setup chunk test data.
     */
    public static void beforeEach() throws Exception {
        log("beforeEach", "");
       	// Apply config and restore
        server.waitForStringInLog("CWWKF0011I", 20000);
        FatUtils.waitForSmarterPlanet(server);

        // Setup chunk test data
        executeSql("jdbc/batch", getChunkInTableSql());
        executeSql("jdbc/batch", getChunkOutTableSql("APP.OUT4"));

        executeSql("jdbc/myds", getChunkInTableSql());
        executeSql("jdbc/myds", getChunkOutTableSql("APP.OUT1"));
        executeSql("jdbc/myds", getChunkOutTableSql("APP.OUT2"));

        executeSql("jdbc/mydsNonTran", getChunkInTableSql());
        executeSql("jdbc/mydsNonTran", getChunkOutTableSql("APP.OUT3"));
    }

    /**
     * Test a simple batchlet.
     */
    @Test
    public void testBasicJDBCPersistence() throws Exception {
        test("Basic", "jslName=BasicPersistence");
    }

    /**
     * Chunk test using the same datasource as the batch tables.
     */
    @Test
    public void testSharedResourceMultiChunk() throws Exception {
        test("Chunk", "jslName=ChunkTestMultipleCheckpoint&writeTable=APP.OUT4&sharedDB=true");
    }

    /**
     * Log helper.
     */
    public static void log(String method, String msg) {
        Log.info(JdbcConfigTestCheckpoint .class, method, msg);
    }

}
