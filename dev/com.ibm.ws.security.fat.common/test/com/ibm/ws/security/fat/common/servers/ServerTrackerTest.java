/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.servers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.MessageConstants;
import com.ibm.ws.security.test.common.CommonTestClass;

import componenttest.topology.impl.LibertyServer;
import test.common.SharedOutputManager;

public class ServerTrackerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private final LibertyServer server1 = mockery.mock(LibertyServer.class, "server1");
    private final LibertyServer server2 = mockery.mock(LibertyServer.class, "server2");
    private final LibertyServer server3 = mockery.mock(LibertyServer.class, "server3");

    ServerTracker tracker = new ServerTracker();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        tracker = new ServerTracker();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** addServer **************************************/

    /**
     * Tests:
     * - Add a null server instance to empty server tracker
     * Expects:
     * - Object should be successfully added
     */
    @Test
    public void test_addServer_nullServer() {
        try {
            LibertyServer server = null;

            tracker.addServer(server);

            Set<LibertyServer> servers = tracker.getServers();
            assertEquals("Server size did not match expected value.", 1, servers.size());
            LibertyServer addedServer = servers.iterator().next();
            assertNull("Single entry should have been null but was: " + addedServer, addedServer);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Add a server instance to empty server tracker
     * Expects:
     * - Object should be successfully added
     */
    @Test
    public void test_addServer_addOneServer() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                }
            });

            tracker.addServer(server1);

            Set<LibertyServer> servers = tracker.getServers();
            assertEquals("Server size did not match expected value.", 1, servers.size());
            assertTrue("Retrieved server set did not contain expected server " + server1 + ". Set was " + servers, servers.contains(server1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Add a server instance twice
     * Expects:
     * - Object should be successfully added, but only one instance should be stored
     */
    @Test
    public void test_addServer_addSameServerTwice() {
        try {
            mockery.checking(new Expectations() {
                {
                    exactly(2).of(server1).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    exactly(2).of(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    exactly(2).of(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    exactly(2).of(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    exactly(2).of(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    exactly(2).of(server1).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    exactly(2).of(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                }
            });

            tracker.addServer(server1);
            tracker.addServer(server1);

            Set<LibertyServer> servers = tracker.getServers();
            assertEquals("Server size did not match expected value.", 1, servers.size());
            assertTrue("Retrieved server set did not contain expected server " + server1 + ". Set was " + servers, servers.contains(server1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Add multiple different servers
     * Expects:
     * - Each object should be successfully added
     */
    @Test
    public void test_addServer_addMultipleServers() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                    one(server2).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                    one(server3).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                }
            });

            tracker.addServer(server1);
            tracker.addServer(server2);
            tracker.addServer(server3);

            Set<LibertyServer> servers = tracker.getServers();
            assertEquals("Server size did not match expected value.", 3, servers.size());
            assertTrue("Retrieved server set did not contain expected server " + server1 + ". Set was " + servers, servers.contains(server1));
            assertTrue("Retrieved server set did not contain expected server " + server2 + ". Set was " + servers, servers.contains(server2));
            assertTrue("Retrieved server set did not contain expected server " + server3 + ". Set was " + servers, servers.contains(server3));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** stopAllServers **************************************/

    /**
     * Tests:
     * - No servers currently tracked
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_stopAllServers_noServers() {
        try {
            tracker.stopAllServers();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One null server tracked
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_stopAllServers_oneNullServer() {
        try {
            tracker.addServer(null);

            tracker.stopAllServers();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One server tracked
     * Expects:
     * - Server should be stopped
     */
    @Test
    public void test_stopAllServers_oneServer() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                }
            });

            tracker.addServer(server1);

            mockery.checking(new Expectations() {
                {
                    one(server1).stopServer();
                }
            });

            tracker.stopAllServers();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One server tracked
     * - Exception thrown while stopping the server
     * Expects:
     * - Cumulative exception should be thrown that includes the underlying exception message
     */
    @Test
    public void test_stopAllServers_oneServerExceptionThrown() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                }
            });

            tracker.addServer(server1);

            mockery.checking(new Expectations() {
                {
                    one(server1).stopServer();
                    will(throwException(new Exception(defaultExceptionMsg)));
                }
            });

            try {
                tracker.stopAllServers();
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple servers tracked
     * Expects:
     * - All servers should be stopped successfully
     */
    @Test
    public void test_stopAllServers_multipleServers() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                    one(server2).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                    one(server3).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                }
            });

            tracker.addServer(server1);
            tracker.addServer(server2);
            tracker.addServer(server3);

            mockery.checking(new Expectations() {
                {
                    one(server1).stopServer();
                    one(server2).stopServer();
                    one(server3).stopServer();
                }
            });

            tracker.stopAllServers();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple servers tracked
     * - Exceptions thrown while stopping some servers
     * Expects:
     * - Cumulative exception should be thrown that includes the underlying exception message from each exception
     */
    @Test
    public void test_stopAllServers_multipleServersExceptionsThrown() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server1).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                    one(server2).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server2).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                    one(server3).addInstalledAppForValidation(Constants.APP_TESTMARKER);
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING, MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE, MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
                    one(server3).addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));
                }
            });

            tracker.addServer(server1);
            tracker.addServer(server2);
            tracker.addServer(server3);

            mockery.checking(new Expectations() {
                {
                    one(server1).stopServer();
                    will(throwException(new Exception("Server 1 exception")));
                    one(server2).stopServer();
                    one(server3).stopServer();
                    will(throwException(new Exception("Server 3 exception")));
                }
            });

            try {
                tracker.stopAllServers();
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, Pattern.quote("Server 1 exception"));
                verifyException(e, Pattern.quote("Server 3 exception"));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** stopServerOrThrowException **************************************/

    /**
     * Tests:
     * - Provided server is null
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_stopServerOrThrowException_nullServer() {
        try {
            LibertyServer server = null;
            tracker.stopServerOrThrowException(server);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Exception thrown while stopping the server
     * Expects:
     * - Exception should be re-thrown
     */
    @Test
    public void test_stopServerOrThrowException_stoppingThrowsException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).stopServer();
                    will(throwException(new Exception(defaultExceptionMsg)));
                }
            });
            try {
                tracker.stopServerOrThrowException(server1);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Successful server stop
     * Expects:
     * - Nothing should happen other than the server stop invocation
     */
    @Test
    public void test_stopServerOrThrowException_successfulStop() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(server1).stopServer();
                }
            });
            tracker.stopServerOrThrowException(server1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
