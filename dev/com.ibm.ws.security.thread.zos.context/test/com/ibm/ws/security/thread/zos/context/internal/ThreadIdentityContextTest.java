/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.thread.zos.context.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.wsspi.kernel.security.thread.ThreadIdentityService;

/**
 * Unit test for ThreadIdentityContextImpl.
 */
@RunWith(JMock.class)
public class ThreadIdentityContextTest {

    /**
     * Mock environment for NativeMethodManager and native methods.
     */
    private static Mockery mockery = null;

    /**
     * The mocked ThreadIdentityService.
     */
    protected ThreadIdentityService mockTIS = null;

    /**
     * Create the Mockery environemnt and all the mock objects. Call this method at the
     * beginning of each test, to create a fresh isolated Mockery environment for the test.
     *
     * (This makes debugging easier when a test fails, because all the Expectations from
     * previous tests don't get dumped to the console).
     */
    @Before
    public void createMockEnv() {
        mockery = new JUnit4Mockery();
        mockTIS = mockery.mock(ThreadIdentityService.class);
        ThreadIdentityManager.addThreadIdentityService(mockTIS);
    }

    @After
    public void tearDown() {
        ThreadIdentityManager.removeAllThreadIdentityServices();
    }

    /**
     * Test basic operations.
     */
    @Test
    public void test() throws Exception {
        ThreadIdentityContextImpl threadIdentityContextImpl = new ThreadIdentityContextImpl();

        setExpectations(true);

        threadIdentityContextImpl.taskStarting();
        threadIdentityContextImpl.taskStopping();
    }

    /**
     * Test with current component disabled.
     */
    @Test
    public void testCurrentComnponentDisabled() throws Exception {
        ThreadIdentityContextImpl threadIdentityContextImpl = new ThreadIdentityContextImpl();

        setExpectations(true);

        threadIdentityContextImpl.taskStarting();
        threadIdentityContextImpl.taskStopping();
    }

    /**
     * Test with server-wide app disabled.
     */
    @Test
    public void testAppDisabled() throws Exception {
        ThreadIdentityContextImpl threadIdentityContextImpl = new ThreadIdentityContextImpl();

        setExpectations(false);

        threadIdentityContextImpl.taskStarting();
        threadIdentityContextImpl.taskStopping();
    }

    /**
     * Serialize and deserialize an instance of ThreadIdentityContextImpl
     */
    @Test
    public void testSerialization() throws Exception {
        ThreadIdentityContextImpl threadIdentityContext = new ThreadIdentityContextImpl();

        // Serialize to bytes
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
        objectOutput.writeObject(threadIdentityContext);
        byte[] bytes = byteOutput.toByteArray();
        objectOutput.close();

        // Deserialize from bytes
        ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes));
        threadIdentityContext = (ThreadIdentityContextImpl) objectInput.readObject();
        objectInput.close();

        setExpectations(true);

        threadIdentityContext.taskStarting();
        threadIdentityContext.taskStopping();
    }

    /**
     * Setup common mockery Expectations for ThreadIdentityManager.isApplicationSyncToThreadConfiguredForCurrentComponent().
     */
    protected Object setExpectations(final boolean appEnabledValue) {

        mockery.checking(new Expectations() {
            {
                oneOf(mockTIS).isAppThreadIdentityEnabled();
                will(returnValue(appEnabledValue));
            }
        });

        if (appEnabledValue) {
            // The check for current component was moved to ThreadIdentityServiceImpl.
            // The code will call ThreadIdentityManager.setAppThreadIdentity, which will call
            // ThreadIdentityService.isAppThreadIdentity and ThreadIdentityService.set.

            // Add a subject to the thread.  This subject will be passed to ThreadIdentityService.set().
            SubjectManager sm = new SubjectManager();
            final Subject invSubj = new Subject();
            sm.setInvocationSubject(invSubj);

            final Object token = new Object();
            mockery.checking(new Expectations() {
                {
                    oneOf(mockTIS).set(with(invSubj));
                    will(returnValue(token));
                }
            });

            // Since all is enabled and we're returning a non-null token,
            // setup ThreadIdentityManager.resetExpectations, which calls
            // ThreadIdentityService.reset().
            mockery.checking(new Expectations() {
                {
                    oneOf(mockTIS).reset(with(token));
                }
            });

            return token;
        }

        return null;
    }

    /**
     * Serialize and deserialize an instance of ThreadIdentityContextImpl
     */
    @Test
    public void testDefaultSerialization() throws Exception {
        ThreadIdentityContextImpl defaultThreadIdentityContext = new ThreadIdentityContextImpl().setIsDefaultContext(true);

        // Serialize to bytes
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
        objectOutput.writeObject(defaultThreadIdentityContext);
        byte[] bytes = byteOutput.toByteArray();
        objectOutput.close();

        // Deserialize from bytes
        ObjectInputStream objectInput = new ObjectInputStream(new ByteArrayInputStream(bytes));
        defaultThreadIdentityContext = (ThreadIdentityContextImpl) objectInput.readObject();
        objectInput.close();

        setDefaultExpectations(true);

        defaultThreadIdentityContext.taskStarting();
        defaultThreadIdentityContext.taskStopping();
    }

    /**
     * Setup common mockery Expectations for ThreadIdentityManager.isApplicationSyncToThreadConfiguredForCurrentComponent().
     */
    protected Object setDefaultExpectations(final boolean appEnabledValue) {

        mockery.checking(new Expectations() {
            {
                oneOf(mockTIS).isAppThreadIdentityEnabled();
                will(returnValue(appEnabledValue));
            }
        });

        if (appEnabledValue) {
            final Object token = new Object();
            mockery.checking(new Expectations() {
                {
                    oneOf(mockTIS).runAsServer();
                    will(returnValue(token));
                }
            });

            // Since all is enabled and we're returning a non-null token,
            // setup ThreadIdentityManager.resetExpectations, which calls
            // ThreadIdentityService.reset().
            mockery.checking(new Expectations() {
                {
                    oneOf(mockTIS).reset(with(token));
                }
            });

            return token;
        }

        return null;
    }
}
