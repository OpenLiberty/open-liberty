/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class SessionCacheOneServerTest extends FATServletClient {

    @Server("sessionCacheServer")
    public static LibertyServer server;

    public static SessionCacheApp app = null;

    public static ExecutorService executor;

    @BeforeClass
    public static void setUp() throws Exception {
        executor = Executors.newFixedThreadPool(12);
        app = new SessionCacheApp(server, true, "session.cache.web", "session.cache.web.listener1", "session.cache.web.listener2");

        //String hazelcastConfigFile = "hazelcast-localhost-only.xml";

//        if (FATSuite.isMulticastDisabled()) {
//            Log.info(SessionCacheOneServerTest.class, "setUp", "Disabling multicast in Hazelcast config.");
//            hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";
//        }

        String hazelcastConfigFile = "hazelcast-localhost-only-multicastDisabled.xml";

        String configLocation = new File(server.getUserDir() + "/shared/resources/hazelcast/" + hazelcastConfigFile).getAbsolutePath();
        server.setJvmOptions(Arrays.asList("-Dhazelcast.group.name=" + UUID.randomUUID(),
                                           "-Dhazelcast.config=" + configLocation));

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        executor.shutdownNow();
        server.stopServer();
    }

    /**
     * Submit concurrent requests to put new attributes into a single session.
     * Verify that all of the attributes (and no others) are added to the session, with their respective values.
     * After attributes have been added, submit concurrent requests to remove some of them.
     */
    @Test
    public void testConcurrentPutNewAttributesAndRemove() throws Exception {
        final int NUM_THREADS = 9;

        List<String> session = new ArrayList<>();

        StringBuilder attributeNames = new StringBuilder("testConcurrentPutNewAttributesAndRemove-key0");

        List<Callable<Void>> puts = new ArrayList<Callable<Void>>();
        List<Callable<Void>> gets = new ArrayList<Callable<Void>>();
        List<Callable<Void>> removes = new ArrayList<Callable<Void>>();
        for (int i = 1; i <= NUM_THREADS; i++) {
            final int offset = i;
            attributeNames.append(",testConcurrentPutNewAttributesAndRemove-key").append(i);
            puts.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    app.sessionPut("testConcurrentPutNewAttributesAndRemove-key" + offset + "&sync=true", (char) ('A' + offset), session, true);
                    return null;
                }
            });
            gets.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    app.sessionGet("testConcurrentPutNewAttributesAndRemove-key" + offset, (char) ('A' + offset), session);
                    return null;
                }
            });
            if (i < NUM_THREADS) // leave the last session attribute
                removes.add(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        app.invokeServlet("sessionRemoveAttribute&key=testConcurrentPutNewAttributesAndRemove-key" + offset + "&sync=true", session);
                        return null;
                    }
                });
        }

        String sessionId = app.sessionPut("testConcurrentPutNewAttributesAndRemove-key0&sync=true", 'A', session, true);
        try {
            List<Future<Void>> futures = executor.invokeAll(puts);
            for (Future<Void> future : futures)
                future.get(); // report any exceptions that might have occurred

            app.invokeServlet("testAttributeNames&allowOtherAttributes=false&sessionAttributes=" + attributeNames, session);

            futures = executor.invokeAll(gets);
            for (Future<Void> future : futures)
                future.get(); // report any exceptions that might have occurred

            // check exact values in cache
            app.invokeServlet("testSessionInfoCache&sessionId=" + sessionId + "&attributes=" + attributeNames, session);
            for (int i = 1; i <= NUM_THREADS; i++)
                app.invokeServlet("testSessionPropertyCache&sessionId=" + sessionId
                                  + "&type=java.lang.Character&key=testConcurrentPutNewAttributesAndRemove-key" + i
                                  + "&values=" + (char) ('A' + i),
                                  session);

            futures = executor.invokeAll(removes);
            for (Future<Void> future : futures)
                future.get(); // report any exceptions that might have occurred

            // first and last attribute must remain, others must be removed
            app.invokeServlet("testAttributeNames&allowOtherAttributes=false&sessionAttributes=" +
                              "testConcurrentPutNewAttributesAndRemove-key0,testConcurrentPutNewAttributesAndRemove-key" + NUM_THREADS, session);

            // check exact values in cache
            app.invokeServlet("testSessionInfoCache&sessionId=" + sessionId + "&attributes=testConcurrentPutNewAttributesAndRemove-key0,testConcurrentPutNewAttributesAndRemove-key"
                              + NUM_THREADS, session);
            app.invokeServlet("testSessionPropertyCache&sessionId=" + sessionId + "&type=java.lang.Character&key=testConcurrentPutNewAttributesAndRemove-key0&values=A", session);
            app.invokeServlet("testSessionPropertyCache&sessionId=" + sessionId + "&type=java.lang.Character&key=testConcurrentPutNewAttributesAndRemove-key" + NUM_THREADS
                              + "&values=" + (char) ('A' + NUM_THREADS), session);
        } finally {
            app.invalidateSession(session);
        }
    }

    /**
     * Submit concurrent requests to replace the value of the same attributes within a single session.
     */
    @Test
    public void testConcurrentReplaceAttributes() throws Exception {
        final int NUM_ATTRS = 2;
        final int NUM_THREADS = 8;

        List<String> session = new ArrayList<>();

        Map<String, String> expectedValues = new TreeMap<String, String>();
        List<Callable<Void>> puts = new ArrayList<Callable<Void>>();
        for (int i = 1; i <= NUM_ATTRS; i++) {
            final String key = "testConcurrentReplaceAttributes-key" + i;
            StringBuilder sb = new StringBuilder();

            for (int j = 1; j <= NUM_THREADS / NUM_ATTRS; j++) {
                final int value = i * 100 + j;
                if (j > 1)
                    sb.append(',');
                sb.append(value);

                puts.add(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        app.sessionPut(key + "&sync=true", value, session, false);
                        return null;
                    }
                });
            }

            expectedValues.put(key, sb.toString());
        }

        String sessionId = app.sessionPut("testConcurrentReplaceAttributes-key1&sync=true", 100, session, true);
        try {
            app.sessionPut("testConcurrentReplaceAttributes-key2&sync=true", 200, session, false);

            List<Future<Void>> futures = executor.invokeAll(puts);
            for (Future<Void> future : futures)
                future.get(); // report any exceptions that might have occurred

            app.invokeServlet("testAttributeNames&allowOtherAttributes=false&sessionAttributes=testConcurrentReplaceAttributes-key1,testConcurrentReplaceAttributes-key2", session);

            for (Map.Entry<String, String> expected : expectedValues.entrySet()) {
                String response = app.invokeServlet("testAttributeIsAnyOf&type=java.lang.Integer&key=" + expected.getKey() + "&values=" + expected.getValue(), session);

                int start = response.indexOf("session property value: [") + 25;
                String value = response.substring(start, response.indexOf("]", start));

                // check exact value in JCache
                app.invokeServlet("testSessionPropertyCache&sessionId=" + sessionId + "&type=java.lang.Integer&key=" + expected.getKey() + "&values=" + value, session);
            }

            app.invokeServlet("testSessionInfoCache&sessionId=" + sessionId + "&attributes=testConcurrentReplaceAttributes-key1,testConcurrentReplaceAttributes-key2", session);
        } finally {
            app.invalidateSession(session);
        }
    }

    /**
     * Submit concurrent requests to add/update/get/remove a session attribute.
     * There will be no guarantee whether or not the session attribute exists at the end of the test,
     * but if it does exist, it must have one of the values that was set during the test.
     */
    @Test
    public void testConcurrentSetGetAndRemove() throws Exception {
        final int NUM_THREADS = 12;

        List<String> session = new ArrayList<>();

        // The test case only adds even values, so expect results include null (attribute doesn't exist) or an even value
        final StringBuilder expectedValues = new StringBuilder("null");
        for (int i = 0; i < NUM_THREADS; i += 2) {
            expectedValues.append(',');
            expectedValues.append(1000 + i);
        }

        List<Callable<Void>> requests = new ArrayList<Callable<Void>>();
        for (int i = 0; i < NUM_THREADS; i++) {
            final int value = 1000 + i;
            switch (i % 4) {
                case 0:
                case 2:
                    requests.add(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            app.sessionPut("testConcurrentSetGetAndRemove-key&sync=true", value, session, false);
                            return null;
                        }
                    });
                    break;
                case 1:
                    requests.add(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            app.invokeServlet("testAttributeIsAnyOf&type=java.lang.Integer&key=testConcurrentSetGetAndRemove-key&values=" + expectedValues, session);
                            return null;
                        }
                    });
                    break;
                default: // 3
                    requests.add(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            app.invokeServlet("sessionRemoveAttribute&key=testConcurrentSetGetAndRemove-key&sync=true", session);
                            return null;
                        }
                    });
            }
        }

        String response1 = app.invokeServlet("getSessionId", session); // creates the session
        try {
            List<Future<Void>> futures = executor.invokeAll(requests);
            for (Future<Void> future : futures)
                future.get(); // report any exceptions that might have occurred

            String response2 = app.invokeServlet("testAttributeIsAnyOf&type=java.lang.Integer&key=testConcurrentSetGetAndRemove-key&values=" + expectedValues, session);

            int start = response1.indexOf("session id: [") + 13;
            String sessionId = response1.substring(start, response1.indexOf("]", start));

            start = response2.indexOf("session property value: [") + 25;
            String value = response2.substring(start, response2.indexOf("]", start));

            // verify the property value is present in the session properties cache
            app.invokeServlet("testSessionPropertyCache&sessionId=" + sessionId + "&type=java.lang.Integer&key=testConcurrentSetGetAndRemove-key&values=" + expectedValues,
                              session);

            // The following fails intermittently and is being investigated. // TODO re-enable
            // verify the property name is present in the session info cache
            //if (!"null".equals(value))
            //    app.invokeServlet("testSessionInfoCache&sessionId=" + sessionId + "&attributes=testConcurrentSetGetAndRemove-key", session);
        } finally {
            app.invalidateSession(session);
        }
    }

    /**
     * Verify that the time reported as the creation time of the session is reasonably close to when we created it.
     */
    @Test
    public void testCreationTime() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testCreationTime", session);
        app.invalidateSession(session);
    }

    /**
     * Verify that two HttpSessionListeners both receive events when sessions are created and destroyed.
     */
    @Test
    public void testHttpSessionListeners() throws Exception {
        List<String> session1 = new ArrayList<>();
        String sessionId1 = app.sessionPut("testHttpSessionListeners-key1", (byte) 10, session1, true);
        try {
            // Registered HttpSessionListeners listener1 and listener2 must be notified of
            // the creation of session1, and at this point must contain no record of it being destroyed,

            app.invokeServlet("testHttpSessionListener&listener=listener1" +
                              "&sessionCreated=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId1,
                              null);

            app.sessionGet("testHttpSessionListeners-key1", (byte) 10, session1);

            app.invokeServlet("testHttpSessionListener&listener=listener2" +
                              "&sessionCreated=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId1,
                              null);
        } finally {
            // Invalidating the session should cause sessionDestroyed to be sent to the listeners
            app.invalidateSession(session1);
        }

        List<String> session2 = new ArrayList<>();
        String sessionId2 = app.sessionPut("testHttpSessionListeners-key2", true, session2, true);
        try {
            // Registered HttpSessionListeners listener1 and listener2 must be notified of
            // the creation of session2, and at this point must contain no record of it being destroyed.
            // They should however, indicate that session1 was destroyed,

            app.invokeServlet("testHttpSessionListener&listener=listener1" +
                              "&sessionCreated=" + sessionId2 +
                              "&sessionDestroyed=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId2,
                              null);

            app.invokeServlet("testHttpSessionListener&listener=listener2" +
                              "&sessionCreated=" + sessionId2 +
                              "&sessionDestroyed=" + sessionId1 +
                              "&sessionNotDestroyed=" + sessionId2,
                              null);

            app.sessionGet("testHttpSessionListeners-key2", true, session2);
        } finally {
            // Invalidating the session should cause sessionDestroyed to be sent to the listeners
            app.invalidateSession(session2);
        }

        // Registered HttpSessionListeners listener1 and listener2 must be notified of
        // the destruction of session2,

        app.invokeServlet("testHttpSessionListener&listener=listener1" +
                          "&sessionDestroyed=" + sessionId2,
                          null);
        app.invokeServlet("testHttpSessionListener&listener=listener2" +
                          "&sessionDestroyed=" + sessionId2,
                          null);
    }

    /**
     * Test of IBMSessionExt.invalidateAll(true)
     */
    @Test
    public void testInvalidateAll() throws Exception {
        List<String> session = new ArrayList<>();
        String sessionId = app.sessionPut("str&sync=true", "value1", session, true);

        app.invokeServlet("testInvalidateAll", session);

        app.invokeServlet("testSessionEmpty", session);

        app.invokeServlet("testHttpSessionListener&listener=listener1" +
                          "&sessionDestroyed=" + sessionId,
                          null);
        app.invokeServlet("testHttpSessionListener&listener=listener2" +
                          "&sessionDestroyed=" + sessionId,
                          null);
    }

    /**
     * Test that the last accessed time changes when accessed at different times.
     */
    @Test
    public void testLastAccessedTime() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testLastAccessedTime", session);
        app.invalidateSession(session);
    }

    /**
     * Verify that CacheMXBean and CacheStatisticsMXBean provided for each of the caches created by the sessionCache feature
     * can be obtained and report statistics about the cache.
     */
    @Test
    public void testMXBeansEnabled() throws Exception {
        app.invokeServlet("testMXBeansEnabled", new ArrayList<>());
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testSerialization() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testSerialization", session);
        try {
            app.invokeServlet("evictSession", null);
            app.invokeServlet("testSerialization_complete", session);
        } finally {
            app.invalidateSession(session);
        }
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testSerializeDataSource() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testSerializeDataSource", session);
        try {
            app.invokeServlet("evictSession", null);
            app.invokeServlet("testSerializeDataSource_complete", session);
        } finally {
            app.invalidateSession(session);
        }
    }
}
