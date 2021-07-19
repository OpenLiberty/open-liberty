/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.fat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.custom.junit.runner.FATRunner;

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class MultiServerAuthCacheTest extends BaseTestCase {
    private static BasicAuthClient client1;
    private static BasicAuthClient client2;

    @BeforeClass
    public static void beforeClass() throws Exception {
        UUID hazelcastGroupName = UUID.randomUUID();

        /*
         * Start the server that will server as a Hazelcast member first.
         */
        startServer1(hazelcastGroupName.toString());
        client1 = new BasicAuthClient(server1);

        /*
         * Start the server that will serve as a Hazelcast client.
         */
        startServer2(hazelcastGroupName.toString());
        client2 = new BasicAuthClient(server2);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        /*
         * Stop client first.
         */
        stopServer2();

        /*
         * Now stop the Hazelcast member.
         */
        stopServer1();
    }

    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void basicAuth_multipleServers() throws Exception {

        /*
         * Send initial request to the first server. This should result in a cache miss, but should
         * add the entry to the distributed cache.
         */
        String response = client1.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", client1.verifyResponse(response, USER1_NAME, false, false));
        assertFalse("Request should have resulted in a JCache miss.", server1.findStringsInTrace(JCACHE_MISS_USER1_BASICAUTH).isEmpty());

        /*
         * Send the second request to the second server. This should result in a cache hit since the
         * first request populated the distributed cache.
         */
        response = client2.accessProtectedServletWithAuthorizedCredentials(ServletClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", client2.verifyResponse(response, USER1_NAME, false, false));
        assertFalse("Request should have resulted in a JCache hit.", server2.findStringsInTrace(JCACHE_HIT_USER1_BASICAUTH).isEmpty());
    }
}
