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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class DupXidTestBase extends FATServletClient {

    public static final String APP_NAME = "transaction";
    protected static String SERVLET_NAME;

    public static LibertyServer server1;
    public static LibertyServer server2;

    protected static void setup(LibertyServer s1, LibertyServer s2, String servletName) throws Exception {

        server1 = s1;
        server2 = s2;

        ShrinkHelper.defaultApp(server1, APP_NAME, "web.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "web.*");

        SERVLET_NAME = servletName;

        server1.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server1, server2);
    }

    @Test
    public void testDupXid() throws Exception {

        FATUtils.startServers(server1);
        try {
            // We expect this to fail since it is gonna crash the server
            runTest(server1, SERVLET_NAME, "setupDupXid");
        } catch (IOException e) {
        }
        assertNotNull(server1.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        FATUtils.startServers(server2);
        try {
            // We expect this to fail since it is gonna crash the server
            runTest(server2, SERVLET_NAME, "setupDupXid");
        } catch (IOException e) {
        }
        assertNotNull(server2.waitForStringInLog(XAResourceImpl.DUMP_STATE));

        FATUtils.startServers(server1);

        // Check recover returns 2 xids
        assertNotNull(server1.waitForStringInTrace("Resource returned 2 Xids"));

        // Check we only recover 1
        assertNotNull(server1.waitForStringInTrace("After filter by cruuid and epoch, Xids to recover 1"));
    }
}