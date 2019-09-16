/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.security.auth.Subject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.context.internal.SubjectThreadContext;

import test.common.SharedOutputManager;

public class SubjectManagerTest {

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor:
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testCallerSubject() {
        final String methodName = "testCallerSubject";
        try {
            Subject callerSubject = new Subject();
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setCallerSubject(callerSubject);
            Subject actualSubject = subjectManager.getCallerSubject();

            assertEquals("The retrieved subject must be equals to the caller subject.", callerSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInvocationSubject() {
        final String methodName = "testInvocationSubject";
        try {
            Subject invocationSubject = new Subject();
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setInvocationSubject(invocationSubject);
            Subject actualSubject = subjectManager.getInvocationSubject();

            assertEquals("The retrieved subject must be equals to the invocation subject.", invocationSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testReplaceCallerSubject() {
        final String methodName = "testReplaceCallerSubject";
        try {
            Subject firstCallerSubject = createTestSubject("firstCallerSubject");
            Subject secondCallerSubject = createTestSubject("secondCallerSubject");
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setCallerSubject(firstCallerSubject);

            Subject replacedCallerSubject = subjectManager.replaceCallerSubject(secondCallerSubject);
            Subject actualSubject = subjectManager.getCallerSubject();

            assertEquals("The replaced caller subject must be equals to the first caller subject.", firstCallerSubject, replacedCallerSubject);
            assertEquals("The actual caller subject must be equals to the second caller subject.", secondCallerSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testReplaceInvocationSubject() {
        final String methodName = "testReplaceInvocationSubject";
        try {
            Subject firstInvocationSubject = createTestSubject("firstInvocationSubject");
            Subject secondInvocationSubject = createTestSubject("secondInvocationSubject");
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setInvocationSubject(firstInvocationSubject);

            Subject replacedInvocationSubject = subjectManager.replaceInvocationSubject(secondInvocationSubject);
            Subject actualSubject = subjectManager.getInvocationSubject();

            assertEquals("The replaced invocation subject must be equals to the first invocation subject.", firstInvocationSubject, replacedInvocationSubject);
            assertEquals("The actual invocation subject must be equals to the second invocation subject.", secondInvocationSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDifferentThreadsHaveDifferentThreadContexts() {
        final String methodName = "testDifferentThreadsHaveDifferentThreadContexts";
        try {
            SubjectManager subjectManager = new SubjectManager();
            FutureTask<SubjectThreadContext> futureTask = createStartedFutureTaskForContextAccessor(subjectManager);
            FutureTask<SubjectThreadContext> secondFutureTask = createStartedFutureTaskForContextAccessor(subjectManager);

            SubjectThreadContext firstThreadContext = futureTask.get();
            SubjectThreadContext secondThreadContext = secondFutureTask.get();

            assertFalse("The thread contexts must be different in each thread.", firstThreadContext.equals(secondThreadContext));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDifferentThreadsHaveDifferentSubjects() {
        final String methodName = "testDifferentThreadsHaveDifferentSubjects";
        try {
            SubjectManager subjectManager = new SubjectManager();
            FutureTask<Subject> futureTask = createStartedFutureTaskForSubjectAccessor(subjectManager);
            FutureTask<Subject> secondFutureTask = createStartedFutureTaskForSubjectAccessor(subjectManager);

            Subject firstThreadSubject = futureTask.get();
            Subject secondThreadSubject = secondFutureTask.get();
            Subject firstThreadSubjectAfter = futureTask.get();

            assertFalse("The thread subjects must be different in each thread.", firstThreadSubject.equals(secondThreadSubject));
            assertEquals("The first thread subject must NOT be replaced by the second thread.", firstThreadSubject, firstThreadSubjectAfter);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testClearSubjects() {
        final String methodName = "testClearSubjects";
        try {
            Subject invocationSubject = new Subject();
            Subject callerSubject = new Subject();
            SubjectManager subjectManager = new SubjectManager();
            subjectManager.setInvocationSubject(invocationSubject);
            subjectManager.setCallerSubject(callerSubject);
            subjectManager.clearSubjects();

            assertNull("The caller subject must be null.", subjectManager.getCallerSubject());
            assertNull("The invocation subject must be null.", subjectManager.getInvocationSubject());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    private Subject createTestSubject(String name) {
        Subject testSubject = new Subject();
        testSubject.getPublicCredentials().add(name);
        return testSubject;
    }

    private FutureTask<SubjectThreadContext> createStartedFutureTaskForContextAccessor(SubjectManager subjectManager) {
        Callable<SubjectThreadContext> threadContextAccessor = new ThreadContextAccessor(subjectManager);
        FutureTask<SubjectThreadContext> futureTask = new FutureTask<SubjectThreadContext>(threadContextAccessor);
        Thread thread = new Thread(futureTask);
        thread.start();
        return futureTask;
    }

    private FutureTask<Subject> createStartedFutureTaskForSubjectAccessor(SubjectManager subjectManager) {
        Callable<Subject> subjectAccessor = new SubjectAccessor(subjectManager);
        FutureTask<Subject> futureTask = new FutureTask<Subject>(subjectAccessor);
        Thread thread = new Thread(futureTask);
        thread.start();
        return futureTask;
    }

    private class ThreadContextAccessor implements Callable<SubjectThreadContext> {

        SubjectManager subjectManager;

        public ThreadContextAccessor(SubjectManager subjectManager) {
            this.subjectManager = subjectManager;
        }

        /** {@inheritDoc} */
        @Override
        public SubjectThreadContext call() throws Exception {
            subjectManager.setCallerSubject(createTestSubject(Thread.currentThread().getName()));
            return subjectManager.getSubjectThreadContext();
        }

    }

    private class SubjectAccessor implements Callable<Subject> {

        SubjectManager subjectManager;

        public SubjectAccessor(SubjectManager subjectManager) {
            this.subjectManager = subjectManager;
        }

        /** {@inheritDoc} */
        @Override
        public Subject call() throws Exception {
            Subject callerSubject = subjectManager.getCallerSubject();
            if (callerSubject == null) {
                callerSubject = createTestSubject(Thread.currentThread().getName());
                subjectManager.setCallerSubject(callerSubject);
            }
            return callerSubject;
        }
    }

}
