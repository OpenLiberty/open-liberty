/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;

/**
 *
 */
public class WOLARequestDispatcherTest {

    /**
     * Mock environment.
     */
    private Mockery mockery = null;

    /**
     * Create the mockery environment for each test. Setting up a new mockery
     * environment for each test helps isolate Expectation sets, making it easier to
     * debug when some Expectation fails and all the Expectations are dumped
     * to the error log.
     */
    @Before
    public void before() {
        mockery = new JUnit4Mockery();
    }

    /**
     * There are alternative ways to do this.
     * 1) Use @RunWith(JMock.class) (deprecated)
     * 2) Declare the field: @Rule public final JUnitRuleMockery context = new JUnitRuleMockery();
     * (this version of Junit is not in our codebase).
     *
     * Doing it the manual way for now.
     */
    @After
    public void after() {
        mockery.assertIsSatisfied();
    }

    /**
     *
     */
    @Test
    public void testPreInvokeAndPostInvoke() {

        final WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(new byte[WolaMessage.HeaderSize]));

        // Setup mock WolaRequestInterceptors
        final WolaRequestInterceptor mockInterceptor1 = mockery.mock(WolaRequestInterceptor.class, "mockInterceptor1");
        final WolaRequestInterceptor mockInterceptor2 = mockery.mock(WolaRequestInterceptor.class, "mockInterceptor2");
        final WolaRequestInterceptor mockInterceptor3 = mockery.mock(WolaRequestInterceptor.class, "mockInterceptor3");

        // Tokens passed back by each mockInterceptor's preInvoke method.
        // Verify the same token is passed back on the postInvoke.
        final Object token1 = "token1";
        final Object token2 = null;
        final Object token3 = "token3";

        // Each interceptor is preInvoked and returns a token.
        // Verify the correct token is passed back on the postInvoke.
        mockery.checking(new Expectations() {
            {
                oneOf(mockInterceptor1).preInvoke(wolaMessage);
                will(returnValue(token1));

                oneOf(mockInterceptor2).preInvoke(wolaMessage);
                will(returnValue(token2));

                oneOf(mockInterceptor3).preInvoke(wolaMessage);
                will(returnValue(token3));

                oneOf(mockInterceptor1).postInvoke(token1, null);

                oneOf(mockInterceptor2).postInvoke(token2, null);

                oneOf(mockInterceptor3).postInvoke(token3, null);
            }
        });

        // Create a dispatcher and run the test.
        WOLARequestDispatcher dispatcher = new WOLARequestDispatcher(null, wolaMessage).setWolaRequestInterceptors(buildWolaRequestInterceptorSet(mockInterceptor1,
                                                                                                                                                  mockInterceptor2,
                                                                                                                                                  mockInterceptor3));
        dispatcher.postInvoke(dispatcher.preInvoke(wolaMessage));
    }

    /**
     * @return a Set with the given objects in it.
     */
    private Set<WolaRequestInterceptor> buildWolaRequestInterceptorSet(WolaRequestInterceptor... ts) {
        Set<WolaRequestInterceptor> retMe = new HashSet<WolaRequestInterceptor>();
        for (WolaRequestInterceptor t : ts) {
            retMe.add(t);
        }
        return retMe;
    }

    /**
     *
     */
    @Test
    public void testPreInvokeException() {

        final WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(new byte[WolaMessage.HeaderSize]));

        final RuntimeException e = new RuntimeException("blah");

        // Setup mock WolaRequestInterceptors
        final TestWolaRequestInterceptor testInterceptor1 = new TestWolaRequestInterceptor();
        final TestWolaRequestInterceptor testInterceptor2 = new TestWolaRequestInterceptor();
        final TestWolaRequestInterceptor testInterceptor3 = new TestWolaRequestInterceptor() {
            @Override
            public Object preInvoke(WolaMessage wolaMessage) {
                throw e;
            }
        };

        // Create a dispatcher and run the test.
        WOLARequestDispatcher dispatcher = new WOLARequestDispatcher(null, wolaMessage).setWolaRequestInterceptors(buildWolaRequestInterceptorSet(testInterceptor1,
                                                                                                                                                  testInterceptor2,
                                                                                                                                                  testInterceptor3));

        WOLARequestDispatcher.InvokeState invokeState = dispatcher.preInvoke(wolaMessage);

        assertEquals(e, invokeState.responseException);

        dispatcher.postInvoke(invokeState);

        assertEquals("testInterceptor1 pre/post invoke", testInterceptor1.wasPreInvokeCalled, testInterceptor1.wasPostInvokeCalled);
        assertEquals("testInterceptor2 pre/post invoke", testInterceptor2.wasPreInvokeCalled, testInterceptor2.wasPostInvokeCalled);
    }

    /**
     *
     */
    @Test
    public void testPostInvokeException() {

        final WolaMessage wolaMessage = new WolaMessage(new ByteBufferVector(new byte[WolaMessage.HeaderSize]));

        // Setup mock WolaRequestInterceptors
        final WolaRequestInterceptor mockInterceptor1 = mockery.mock(WolaRequestInterceptor.class, "mockInterceptor1");
        final WolaRequestInterceptor mockInterceptor2 = mockery.mock(WolaRequestInterceptor.class, "mockInterceptor2");
        final WolaRequestInterceptor mockInterceptor3 = mockery.mock(WolaRequestInterceptor.class, "mockInterceptor3");

        // Tokens passed back by each mockInterceptor's preInvoke method.
        // Verify the same token is passed back on the postInvoke.
        final Object token1 = "token1";
        final Object token2 = null;
        final Object token3 = "token3";

        // All postInvoke interceptors are invoked regardless of whether
        // any of them throw an exception.
        mockery.checking(new Expectations() {
            {
                oneOf(mockInterceptor1).preInvoke(wolaMessage);
                will(returnValue(token1));

                oneOf(mockInterceptor2).preInvoke(wolaMessage);
                will(returnValue(token2));

                oneOf(mockInterceptor3).preInvoke(wolaMessage);
                will(returnValue(token3));

                oneOf(mockInterceptor1).postInvoke(token1, null);

                oneOf(mockInterceptor2).postInvoke(token2, null);
                will(throwException(new RuntimeException("blah")));

                // #3 still gets called.
                oneOf(mockInterceptor3).postInvoke(token3, null);
            }
        });

        // Create a dispatcher and run the test.
        WOLARequestDispatcher dispatcher = new WOLARequestDispatcher(null, wolaMessage).setWolaRequestInterceptors(buildWolaRequestInterceptorSet(mockInterceptor1,
                                                                                                                                                  mockInterceptor2,
                                                                                                                                                  mockInterceptor3));

        dispatcher.postInvoke(dispatcher.preInvoke(wolaMessage));
    }

}

/**
 *
 */
class TestWolaRequestInterceptor implements WolaRequestInterceptor {

    public boolean wasPreInvokeCalled = false;
    public boolean wasPostInvokeCalled = false;

    private final Object token = new StringBuffer();

    @Override
    public Object preInvoke(WolaMessage wolaMessage) {
        wasPreInvokeCalled = true;
        return token;
    }

    @Override
    public void postInvoke(Object preInvokeToken, Exception responseException) {
        wasPostInvokeCalled = true;

        assertSame("preInvoke token (" + token + ") should be the same as postInvoke token (" + preInvokeToken + ")", token, preInvokeToken);
    }

}
