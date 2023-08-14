/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class DualServerPeerLockingTest1 extends DualServerPeerLockingTest {
    /**
     * This test verifies no regression in transaction recovery behaviour with a pair of servers that have HADB Locking enabled.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicCloudRecovery007() throws Exception {
        dynamicTest(firstServer, secondServer, 7, 2);
    }

    /**
     * This test verifies no regression in transaction recovery behaviour with a pair of servers that have HADB Locking enabled.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicCloudRecovery090() throws Exception {
        dynamicTest(firstServer, secondServer, 90, 3);
    }

    /**
     * This test repeats testDynamicCloudRecovery007 with HADB Locking enabled but allowing timeBetweenHeartbeats,
     * peerTimeBeforeStale and localTimeBeforeStale attributes to default.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. Cloud001 is restarted and transaction recovery verified.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultAttributesCloudRecovery007() throws Exception {
        dynamicTest(defaultAttributesServer1, defaultAttributesServer2, 7, 2);
    }

    /**
     * This test simulates the process by which a homeserver can aggressively reclaim its logs while a peer is in the process
     * of recovering those logs.
     *
     * The Cloud001 server is started and a servlet invoked to halt leaving an indoubt transaction. The Cloud002 server
     * peer recovers the indoubt. But ownership of the transaction logs reverts to Cloud001 even as the Cloud002 server
     * is recovering. Cloud002 should end its recovery processing quietly. Cloud001 is restarted and transaction recovery
     * verified.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicCloudRecoveryInterruptedPeerRecovery() throws Exception {
        dynamicTest(firstServer, secondServer, "InterruptedPeerRecovery", 2);
    }

    /**
     * This test is a repeat of testDynamicCloudRecovery007, except we ensure that both
     * servers are cold started by deleting the Derby file that contains the Transaction
     * Recovery log tables.
     *
     * @throws Exception
     */
    @Test
    public void testColdStartLocalAndPeerServer() throws Exception {

        // Delete existing DB files, so that the tables that support transaction recovery
        // are created from scratch.
        firstServer.deleteFileFromLibertyInstallRoot("/usr/shared/resources/data/tranlogdb");
        dynamicTest(firstServer, secondServer, 7, 2);
    }
}