/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.test.VmmServiceServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/**
 * Confirm correct defaults are used for the Custom Context pool. PI81923
 * Confirm correct/expected defaults are used for Search and Attribute cache. PI81954
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LDAPRegistryNoCustomContext {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("vmm.apis.tds.ldap");
    private static final Class<?> c = LDAPRegistryNoCustomContext.class;
    private static VmmServiceServletConnection servlet;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for vmmapi servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/vmmapi-1.0.mf");
        server.addInstalledAppForValidation("vmmService");
        server.startServer(c.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application vmmService does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*vmmService"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");

        servlet = new VmmServiceServletConnection(server.getHostname(), server.getHttpDefaultPort());

        Thread.sleep(5000);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer();
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/vmmapi-1.0.mf");
        }
    }

    @Test
    public void testNoCustomContext() throws Exception { // for issue
        Log.info(c, "testNoCustomContext", "Entering test testNoCustomContext");

        // Checking that these are correctly logged in the trace
        // We were setting the wrong defaults -- bad maxpool and preferred pool size
        String tr = "InitPoolSize: 1";
        List<String> errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "MaxPoolSize: 0";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PrefPoolSize: 3";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PoolTimeOut: 0";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "PoolWaitTime: 3000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        Log.info(c, "testCustomContextNoConfig", "Check cache config timeouts");

        tr = "CacheTimeOut: 1200000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());
        assertEquals("Should have found 2 entries -- attributes and search cache, " + tr, 2, errMsgs.size());

        tr = "CacheSize: 4000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheSizeLimit: 2000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheSize: 2000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());

        tr = "CacheResultSizeLimit: 2000";
        errMsgs = server.findStringsInLogsAndTrace(tr);
        assertFalse("Should have found, " + tr, errMsgs.isEmpty());
    }
}