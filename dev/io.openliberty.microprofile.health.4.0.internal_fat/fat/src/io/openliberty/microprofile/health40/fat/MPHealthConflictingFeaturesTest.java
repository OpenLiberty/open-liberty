/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health40.fat;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class MPHealthConflictingFeaturesTest {

    final static String SERVER_NAME_MPHEALTH40_MPHEALTH31 = "MPHealth40And31";
    final static String SERVER_NAME_MPHEALTH40_MPHEALTH22 = "MPHealth40And22";
    final static String SERVER_NAME_MPHEALTH40_MPHEALTH10 = "MPHealth40And10";

    private static final String[] EXPECTED_FAILURES = { "CWWKF0033E", "CWWKF0044E", "CWWKF0046W" };

    @Server(SERVER_NAME_MPHEALTH40_MPHEALTH31)
    public static LibertyServer mpHealth40And31Server;

    @Server(SERVER_NAME_MPHEALTH40_MPHEALTH22)
    public static LibertyServer mpHealth40And22Server;

    @Server(SERVER_NAME_MPHEALTH40_MPHEALTH10)
    public static LibertyServer mpHealth40And10Server;

    LibertyServer server;

    @After
    public void cleanUp() throws Exception {
        if (server.isStarted()) {
            server.stopServer(EXPECTED_FAILURES);
        }
    }

    /*
     * Tests to see the expected CWWKF0033E
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testMPHealth40MPHealth31() throws Exception {
        final String METHOD = "testMPHealth40MPHealth31";
        server = mpHealth40And31Server;

        server.startServer(METHOD, true, false, false);

        Assert.assertTrue("The server has not started", server.isStarted());

        List<String> list = server.findStringsInLogs("CWWKF0033E.*(mpHealth-3.1|mpHealth-4.0) and (mpHealth-3.1|mpHealth-4.0)");

        Log.info(getClass(), METHOD, "CWWKF0033E matches are: " + list.toString());

        Assert.assertTrue("Expected matching count to be 1", list.size() == 1);

    }

    /*
     * Tests to see the exepcted CWWKF0033E
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testMPHealth40MPHealth22() throws Exception {
        final String METHOD = "testMPHealth40MPHealth22";
        server = mpHealth40And22Server;

        server.startServer(METHOD, true, false, false);

        Assert.assertTrue("The server has not started", server.isStarted());

        List<String> list = server.findStringsInLogs("CWWKF0033E.*(mpHealth-2.2|mpHealth-4.0) and (mpHealth-2.2|mpHealth-4.0)");

        Log.info(getClass(), METHOD, "CWWKF0033E matches are: " + list.toString());

        //Kernel issues 2 error messages with flipped sequences of mpHealth-2.2 and mpHealth-4.0 in the 2 messages
        Assert.assertTrue("Expected matching count to be 2", list.size() == 2);

    }

    /*
     * Tests to see the exepcted CWWKF0033E
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException" })
    public void testMPHealth40MPHealth10() throws Exception {
        final String METHOD = "testMPHealth40MPHealth10";
        server = mpHealth40And10Server;

        server.startServer(METHOD, true, false, false);

        Assert.assertTrue("The server has not started", server.isStarted());

        List<String> list = server.findStringsInLogs("CWWKF0033E.*(mpHealth-1.0|mpHealth-4.0) and (mpHealth-1.0|mpHealth-4.0)");

        Log.info(getClass(), METHOD, "CWWKF0033E matches are: " + list.toString());

        //Kernel issues 2 error messages with flipped sequences of mpHealth-2.2 and mpHealth-4.0 in the 2 messages
        Assert.assertTrue("Expected matching count to be 2", list.size() == 2);

    }

}
