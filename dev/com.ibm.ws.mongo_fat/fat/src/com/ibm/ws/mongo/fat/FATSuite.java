/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                MongoBasicTest.class,
                MongoSSLTest.class,
                MongoSSLInvalidTrustTest.class,
                MongoDefaultSSLTest.class,
                MongoConfigUpdateTest.class
})
public class FATSuite {
    public static final String APP_NAME = "mongo";
    public static String HOST_NAME;

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
    public static void waitForMongoSSL(LibertyServer server) throws Exception {
        String[] keystores = serverKeystores.get(server.getServerName());

        // Just because SSL feature is reported as starting doesn't mean it is really ready,
        // nor that Mongo is functional, so wait for SSL to report the keystore has been
        // added, then wait for MongoDBService to activate.
        server.resetLogMarks(); // look from start of logs
        for (String keystore : keystores) {
            assertNotNull("Did not find trace message indicating SSL trustStore had been added",
                          server.waitForStringInTraceUsingMark("Adding keystore: " + keystore));
        }
        // Note: may need to also convert this to wait for multiple messages; and also account for deactivate calls
        assertNotNull("Did not find message indicating MongoDBService had activated", server.waitForStringInTraceUsingMark("MongoDBService * < activate"));
    }
}