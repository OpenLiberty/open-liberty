/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                MongoBasicTest.class,
                MongoSSLTest.class,
                MongoDefaultSSLTest.class,
                MongoConfigUpdateTest.class
})
public class FATSuite {

    public static final String APP_NAME = "mongo";
    public static String HOST_NAME;

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

    // TODO: Should not need to scrape trace in order to know a feature is usable --> use min.cardinality
    public static void waitForMongoSSL(LibertyServer server) throws Exception {
        // Just because SSL feature is reported as starting doesn't mean it is really
        // ready,
        // nor that Mongo is functional, so wait for SSL to report the keystore has been
        // added, then wait for MongoDBService to activate.
        server.resetLogMarks(); // look from start of logs
        assertNotNull("Did not find trace message indicating SSL trustStore had been added",
                      server.waitForStringInTraceUsingMark("Adding keystore: (myDefaultTrustStore|differentTrustStore)"));
        assertNotNull("Did not find message indicating MongoDBService had activated", server.waitForStringInTraceUsingMark("MongoDBService * < activate"));
    }

}