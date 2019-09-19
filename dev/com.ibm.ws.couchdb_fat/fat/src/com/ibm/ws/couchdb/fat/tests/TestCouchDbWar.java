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

package com.ibm.ws.couchdb.fat.tests;

import static com.ibm.ws.couchdb.fat.FATSuite.couchdb;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.CouchDBElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.couchdb.fat.web.InjectedCouchDbServlet;
import com.ibm.ws.couchdb.fat.web.InjectedURLCouchDbServlet;
import com.ibm.ws.couchdb.fat.web.JNDIDirectLookupServlet;
import com.ibm.ws.couchdb.fat.web.JNDIResourceEnvRefServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test use of the couchdb-1.0 feature from a WAR application.
 */
@RunWith(FATRunner.class)
public class TestCouchDbWar extends FATServletClient {
    private final static Logger logger = Logger.getLogger(TestCouchDbWar.class.getName());

    public static final String APP_NAME = "couchdb";
    static final String DATABASE = "my_database";

    @Server("com.ibm.ws.couchdb.fat.server")
    @TestServlets({ @TestServlet(servlet = InjectedCouchDbServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = InjectedURLCouchDbServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = JNDIDirectLookupServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = JNDIResourceEnvRefServlet.class, contextRoot = APP_NAME) })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.couchdb.fat.server"))
                    .andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.couchdb.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'couchdb.war' once it's written to a file
        // Include the 'com.ibm.ws.couchdb.fat.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "com.ibm.ws.couchdb.fat.web");

        // No need to create the couchdb database; will auto-create on first connection
        // couchdb.createDb(DATABASE);

        // Setup environment variables for server configuration, using values from test container
        server.addEnvVar("couchdb_host", couchdb.getContainerIpAddress());
        server.addEnvVar("couchdb_port", couchdb.getPort(false));
        server.addEnvVar("couchdb_url", couchdb.getURL(false));
        server.addEnvVar("couchdb_username", couchdb.getUser());
        server.addEnvVar("couchdb_password", couchdb.getPassword());
        logger.info("setup : couchdb_host=" + couchdb.getContainerIpAddress());
        logger.info("setup : couchdb_port=" + couchdb.getPort(false));
        logger.info("setup : couchdb_url=" + couchdb.getURL(false));
        logger.info("setup : couchdb_username=" + couchdb.getUser());
        logger.info("setup : couchdb_password=" + couchdb.getPassword());

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {

            // Drop the database (for test repeats) and stop the server
            try {
                FATServletClient.runTest(server, "couchdb/InjectedCouchDbServlet", "drop");
            } finally {
                server.stopServer();
            }
        }
    }

    private void updateServerConfiguration(ServerConfiguration config) throws Exception {
        logger.info("updating server configuraion");
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }

    private void restoreServerConfiguration() throws Exception {
        logger.info("restoring server configuraion");
        server.setMarkToEndOfLog();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }

    /**
     * Find configuration values from trace statement logged by couchdb service when creating an instance:
     * "creating a org.ektorp.impl.StdCouchDbInstance...."
     */
    private List<Map<String, String>> getConfigurationValuesFromTrace(RemoteFile trace) throws Exception {
        List<Map<String, String>> res = new ArrayList<Map<String, String>>();
        for (int z = 0; z < 2; z++) {
            String line = server.waitForStringInLogUsingMark("creating a org.ektorp.impl.StdCouchDbInstance", trace);
            if (line == null || line.isEmpty()) {
                fail("Didn't find 'creating a org.ektorp.impl.StdCouchDbInstance' in " + server.getMostRecentTraceFile().getAbsolutePath());
            }
            Map<String, String> map = new HashMap<String, String>();
            line = line.substring(line.indexOf("{"), line.length());
            String[] kvs = line.split(",");
            for (int i = 0; i < kvs.length; i++) {
                String[] kv = kvs[i].split("[=]");
                map.put(kv[0].trim(), kv[1].trim());
            }
            res.add(map);
        }
        return res;
    }

    /**
     * Test updating the CouchDB configuration properties in server.xml on
     * a running server. Verify the application is restarted and the changed
     * configuration is reflected.
     */
    @Test
    public void testSetAllPropertiesOnRunningServer() throws Exception {
        final String method = "testSetAllPropertiesOnRunningServer";
        logger.info("entering " + method);
        server.saveServerConfiguration();
        ServerConfiguration config = server.getServerConfiguration();
        try {
            // Update all elements with non default (including invalid configurations ie:enableSSL)
            for (CouchDBElement couch : config.getCouchDBs()) {
                // default="true"/>
                couch.setCaching(false);
                // default="true"/>
                couch.setCleanupIdleConnections(false);
                // default="1s"            ibm:type="duration"/>
                couch.setConnectionTimeout("17s");
                // default="false"/>
                couch.setEnableSSL(true);
                // <AD id="maxCacheEntries" default="1000"          min="0"/>
                couch.setMaxCacheEntries(798);
                // <AD id="maxConnections default="20"/>
                couch.setMaxConnections(37);
                // <AD id="socketTimeout" default="10s"           ibm:type="duration"/>
                couch.setSocketTimeout("389s");
                // <AD id="relaxedSSLSettings default="false"/>
                couch.setRelaxedSSLSettings(true);
                // <AD id="maxObjectSizeBytes default="8192"          min="0"/>
                couch.setMaxObjectSizeBytes(6591);
                // <AD id="useExpectContinue default="true"/>
                couch.setUseExpectContinue(false);
                // <AD id="port" default="5984"/>
                couch.setPort("777");

            }

            updateServerConfiguration(config);

            // Set marks to end of log and trace files
            RemoteFile trace = server.getMostRecentTraceFile();
            server.setMarkToEndOfLog();
            server.setMarkToEndOfLog(trace);

            // Invoke the servlets to force initialization and couchdb instance creation.
            // All servlets should hit DbAccessException due to invalid config, but instance creation will be traced
            FATServletClient.runTest(server, "couchdb/InjectedCouchDbServlet", "dumpConfig");
            FATServletClient.runTest(server, "couchdb/JNDIResourceEnvRefServlet", "dumpConfig");
            FATServletClient.runTest(server, "couchdb/JNDIDirectLookupServlet", "dumpConfig");

            // Obtain configurations used for instance creation from trace, and verify with above values
            // Should be one instance creation for each servlet invoked above.
            List<Map<String, String>> creates = getConfigurationValuesFromTrace(trace);
            logger.info(method + ": verifying config for " + creates.size() + " create couch trace entries");
            assertTrue(creates.size() > 0);
            for (Map<String, String> create : creates) {
                assertTrue("Found : " + create.get("caching"), create.get("caching").equals("false"));
                assertTrue(create.get("cleanupIdleConnections").equals("false"));
                assertTrue(create.get("connectionTimeout").equals("17000"));
                assertTrue(create.get("enableSSL").equals("true"));
                assertTrue(create.get("maxCacheEntries").equals("798"));
                assertTrue(create.get("maxConnections").equals("37"));
                assertTrue(create.get("socketTimeout").equals("389000"));
                assertTrue(create.get("relaxedSSLSettings").equals("true"));
                assertTrue(create.get("maxObjectSizeBytes").equals("6591"));
                assertTrue(create.get("useExpectContinue").equals("false"));
                assertTrue(create.get("port").equals("777"));
            }
        } finally {
            restoreServerConfiguration();
        }

        logger.info("exiting " + method);
    }
}
