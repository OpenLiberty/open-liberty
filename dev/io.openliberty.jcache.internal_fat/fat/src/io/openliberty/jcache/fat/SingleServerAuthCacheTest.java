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

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.custom.junit.runner.FATRunner;

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class SingleServerAuthCacheTest extends BaseTestCase {
    private static BasicAuthClient client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        UUID hazelcastGroupName = UUID.randomUUID();
        startServer1(hazelcastGroupName.toString());
        client = new BasicAuthClient(server1);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        stopServer1();
    }

    @Test
    @CheckForLeakedPasswords(USER1_PASSWORD)
    public void basicAuth_singleServer() throws Exception {
        /*
         * The initial request should result in a cache miss.
         */
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", client.verifyResponse(response, USER1_NAME, false, false));
        assertFalse("The HazelcastCachingProvider should have been used.", server1.findStringsInLogsAndTraceUsingMark("JCCHE0003I:.*jCache1.*HazelcastCachingProvider").isEmpty()); // Provider
        assertFalse("Cache should have been created.", server1.findStringsInLogsAndTraceUsingMark("JCCHE0001I:.*jCache1.*was created").isEmpty()); // Cache created
        assertFalse("Request should have resulted in a JCache miss.", server1.findStringsInLogsAndTraceUsingMark(JCACHE_MISS_USER1_BASICAUTH).isEmpty());

        /*
         * The second request should result in cache hit.
         */
        client.resetClientState(); // Clear tokens, etc
        resetMarksInLogs(server1);
        response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE, USER1_NAME, USER1_PASSWORD);
        assertTrue("Did not get the expected response", client.verifyResponse(response, USER1_NAME, false, false));
        assertTrue("The caching provider should only be loaded once.", server1.findStringsInLogsAndTraceUsingMark("JCCHE0003I:.*jCache1.*HazelcastCachingProvider").isEmpty()); // Provider
        assertTrue("Cache should not be created twice.", server1.findStringsInLogsAndTraceUsingMark("JCCHE0001I:.*jCache1.*was created").isEmpty()); // Cache created
        assertFalse("Request should have resulted in a JCache hit.", server1.findStringsInLogsAndTraceUsingMark(JCACHE_HIT_USER1_BASICAUTH).isEmpty());
    }
}
