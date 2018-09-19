/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.expectations;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;

import componenttest.topology.impl.LibertyServer;
import test.common.SharedOutputManager;

public class ServerMessageExpectationTest extends CommonSpecificExpectationTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    protected final LibertyServer server = mockery.mock(LibertyServer.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
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

    /************************************** Constructors/getters **************************************/

    // TODO - tests for trace searching

    @Test
    public void test_constructor_nullArgs_noFailureMsg() {
        try {
            String testAction = null;
            String searchForVal = null;

            ServerMessageExpectation exp = new ServerMessageExpectation(testAction, server, searchForVal);

            String defaultExpectedMessage = String.format(ServerMessageExpectation.DEFAULT_FAILURE_MSG, searchForVal);
            verifyExpectationValues(exp, testAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, null, searchForVal, defaultExpectedMessage);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_nullArgs() {
        try {
            String testAction = null;
            String searchForVal = null;
            String failureMsg = null;

            ServerMessageExpectation exp = new ServerMessageExpectation(testAction, server, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor() {
        try {
            String testAction = "testAction";
            String searchForVal = "searchForVal";
            String failureMsg = "failureMsg";

            ServerMessageExpectation exp = new ServerMessageExpectation(testAction, server, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Override
    @Test
    public void test_validate_nullContentObject() {
        try {
            Expectation exp = createBasicExpectation();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(server).addIgnoredErrors(Arrays.asList(SEARCH_FOR_VAL));
                    one(server).waitForStringInLogUsingMark(SEARCH_FOR_VAL, 100);
                    will(returnValue(SEARCH_FOR_VAL));
                }
            });
            exp.validate(null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullAction() {
        try {
            Expectation exp = new ServerMessageExpectation(null, server, SEARCH_FOR_VAL);
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(server).addIgnoredErrors(Arrays.asList(SEARCH_FOR_VAL));
                    one(server).waitForStringInLogUsingMark(SEARCH_FOR_VAL, 100);
                    will(returnValue(SEARCH_FOR_VAL));
                }
            });
            exp.validate(htmlunitHtmlPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_messageNotLogged() {
        try {
            Expectation exp = createBasicExpectation();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(server).addIgnoredErrors(Arrays.asList(SEARCH_FOR_VAL));
                    one(server).waitForStringInLogUsingMark(SEARCH_FOR_VAL, 100);
                    will(returnValue(null));
                    one(server).getServerName();
                    will(returnValue("myServer"));
                }
            });
            try {
                exp.validate("some content");
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, Pattern.quote(String.format(ServerMessageExpectation.DEFAULT_FAILURE_MSG, SEARCH_FOR_VAL)));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_messageLogged() {
        try {
            Expectation exp = createBasicExpectation();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(server).addIgnoredErrors(Arrays.asList(SEARCH_FOR_VAL));
                    one(server).waitForStringInLogUsingMark(SEARCH_FOR_VAL, 100);
                    will(returnValue(SEARCH_FOR_VAL));
                }
            });
            exp.validate(0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new ServerMessageExpectation(TEST_ACTION, server, SEARCH_FOR_VAL);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new ServerMessageExpectation(null, server, SEARCH_FOR_VAL);
    }

}
