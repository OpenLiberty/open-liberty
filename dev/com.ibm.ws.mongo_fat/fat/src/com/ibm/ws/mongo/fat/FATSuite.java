/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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
package com.ibm.ws.mongo.fat;

import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                MongoBasicTest.class,
                MongoSSLTest.class,
                MongoSSLInvalidTrustTest.class,
                MongoDefaultSSLTest.class,
                MongoConfigUpdateTest.class
})
public class FATSuite {
    public static final String APP_NAME = "mongo";
    public static String HOST_NAME;
    private static final Class<?> c = FATSuite.class;

    @SuppressWarnings("serial")
    public static Map<String, String[]> serverKeystores = new HashMap<String, String[]>() {
        {
            put("mongo.fat.server.ssl", new String[] { "differentTrustStore",
                                                       "invalidCertAuthTrustStore",
                                                       "myTrustStore",
                                                       "validCertAuthMultiKeyKeyStore",
                                                       "validCertAuthSingleKeyKeyStore",
                                                       "validCertAuthTrustStore" });
            put("mongo.fat.server.ssl.default.config", new String[] { "myDefaultKeyStore",
                                                                      "myDefaultTrustStore" });
        }
    };

    @SuppressWarnings("serial")
    public static Map<String, Integer> serverMongoDBServices = new HashMap<String, Integer>() {
        {
            put("mongo.fat.server.ssl", 20);
            put("mongo.fat.server.ssl.default.config", 1);
        }
    };

    static {
        try {
            HOST_NAME = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            HOST_NAME = "localhost-" + System.nanoTime();
        }
    }

    public static void createApp(LibertyServer server) throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "fat.mongo.web");
    }

    // Mongo uses min.cardinality with SSL, but SSL can still take a long time to start; wait
    public static boolean waitForMongoSSL(LibertyServer server) throws Exception {
        final String m = "waitForMongoSSL";
        String[] keystores = serverKeystores.get(server.getServerName());

        // Just because SSL feature is reported as starting doesn't mean it is really ready,
        // nor that Mongo is functional, so wait for SSL to report the keystore has been
        // added, then wait for MongoDBService to activate.
        Log.info(c, m, "Waiting for SSL trustStore added messages in trace...");
        server.resetLogMarks(); // look from start of logs
        for (String keystore : keystores) {
            assertNotNull("Did not find trace message indicating SSL trustStore had been added",
                          server.waitForStringInTraceUsingMark("Adding keystore: " + keystore));
        }

        // Wait for multiple MongoDBService activation messages; and also account for deactivate calls
        int mongoDBServices = serverMongoDBServices.get(server.getServerName());

        Log.info(c, m, "Looking for MongoDBService activate calls in trace...");
        List<String> activates = server.findStringsInLogsAndTraceUsingMark("MongoDBService * < activate");
        List<String> deactivates = server.findStringsInLogsAndTraceUsingMark("MongoDBService * > deactivate");

        if (mongoDBServices > (activates.size() - deactivates.size())) {
            Log.info(c, m, "Not all MongoDBServices are active: expected=" + mongoDBServices + ",  activates=" + activates.size() + ", deactivates=" + deactivates.size());
            Log.info(c, m, "Waiting for 30 seconds for remaining services to activate...");
            Thread.sleep(30 * 1000);
            activates = server.findStringsInLogsAndTraceUsingMark("MongoDBService * < activate");
            deactivates = server.findStringsInLogsAndTraceUsingMark("MongoDBService * > deactivate");
        }

        if (mongoDBServices != (activates.size() - deactivates.size())) {
            Log.info(c, m, "Not all MongoDBServices are active: expected=" + mongoDBServices + ",  activates=" + activates.size() + ", deactivates=" + deactivates.size());
            return false;
        }

        Log.info(c, m, "All MongoDBServices are active: expected=" + mongoDBServices + ",  activates=" + activates.size() + ", deactivates=" + deactivates.size());
        return true;
    }

    // skip tests when server has fips 140-3 enabled
    public static void skipTestOnFIPS140_3Enabled(LibertyServer server) throws Exception {
        final String m = "skipTestOnFIPS140_3Enabled";
        if (server.isFIPS140_3EnabledAndSupported()) {
            Log.info(c, m, "FIPS 140-3 is running with the supported IBM JDK 8 or JDK 17 on server: " + server.getServerName() + ". Test will not run.");
            Assume.assumeTrue(false); // This disables this test class. None of the tests in the class will be run.
        }
    }
}