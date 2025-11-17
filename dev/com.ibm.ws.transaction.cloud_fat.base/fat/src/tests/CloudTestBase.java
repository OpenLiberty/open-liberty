/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package tests;

import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.TxFATServletClient;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.topology.impl.LibertyServer;

public class CloudTestBase extends TxFATServletClient {

    public static LibertyServer server1;
    public static LibertyServer server2;

    private static String[] testRecoveryTables = new String[] {
                                                                "WAS_PARTNER_LOGcloud0011",
                                                                "WAS_LEASES_LOG",
                                                                "WAS_TRAN_LOGcloud0011",
                                                                "WAS_PARTNER_LOGcloud0021",
                                                                "WAS_TRAN_LOGcloud0021"
    };

    protected static void dropTables() {
        Log.info(CloudTestBase.class, "dropTables", String.join(", ", testRecoveryTables));
        TxTestContainerSuite.dropTables(testRecoveryTables);
    }

    protected static HashSet<String> serversUsed = null;
    protected List<LibertyServer> serversToCleanup;
    protected String[] toleratedMsgs = new String[] { ".*" };

    @After
    public void cleanup() throws Exception {
        try {
            // If any servers have been added to the serversToCleanup array, we'll stop them now
            // test is long gone so we don't care about messages & warnings anymore
            if (serversToCleanup != null) {
                serversToCleanup.forEach(s -> {
                    serversUsed.add(s.getServerName());
                });
                FATUtils.stopServers(toleratedMsgs, serversToCleanup.stream().toArray(LibertyServer[]::new));
            } else {
                Log.info(CloudTestBase.class, "cleanup", "No servers to stop");
            }

            // Clean up XA resource files
            server1.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

            // Remove tranlog DB
            server1.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
        } finally {
            serversToCleanup = null;
            toleratedMsgs = new String[] { ".*" };
        }
    }

    @Before
    public void setup() throws Exception {
        TxTestContainerSuite.assertHealthy();
        serversToCleanup = null;
    }

    @BeforeClass
    public static void beforeCloudTestBase() throws Exception {
        serversUsed = new HashSet<String>();
    }

    @AfterClass
    public static void teardown() throws Exception {
        dropTables();

        serversUsed.forEach(s -> {
            Log.info(CloudTestBase.class, "teardown", "server used: " + s);
        });
    }
}