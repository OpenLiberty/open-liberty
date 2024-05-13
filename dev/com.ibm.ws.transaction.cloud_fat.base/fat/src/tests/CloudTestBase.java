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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tests;

import org.junit.After;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public class CloudTestBase extends FATServletClient {

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

    protected LibertyServer[] serversToCleanup;
    protected static final String[] toleratedMsgs = new String[] { ".*" };

    @After
    public void cleanup() throws Exception {

        // If any servers have been added to the serversToCleanup array, we'll stop them now
        // test is long gone so we don't care about messages & warnings anymore
        if (serversToCleanup != null && serversToCleanup.length > 0) {
            String serverNames[] = new String[serversToCleanup.length];
            int i = 0;
            for (LibertyServer s : serversToCleanup) {
                serverNames[i++] = s.getServerName();
            }
            Log.info(CloudTestBase.class, "cleanup", "Cleaning " + String.join(", ", serverNames));
            FATUtils.stopServers(toleratedMsgs, serversToCleanup);
            serversToCleanup = null;
        } else {
            Log.info(CloudTestBase.class, "cleanup", "No servers to stop");
        }

        // Clean up XA resource files
        server1.deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

        // Remove tranlog DB
        server1.deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
    }
}
