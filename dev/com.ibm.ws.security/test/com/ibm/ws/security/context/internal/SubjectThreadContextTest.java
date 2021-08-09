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
package com.ibm.ws.security.context.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.security.auth.Subject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

public class SubjectThreadContextTest {

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
            SubjectThreadContext subjectThreadContext = new SubjectThreadContext();
            subjectThreadContext.setCallerSubject(callerSubject);
            Subject actualSubject = subjectThreadContext.getCallerSubject();
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
            SubjectThreadContext subjectThreadContext = new SubjectThreadContext();
            subjectThreadContext.setInvocationSubject(invocationSubject);
            Subject actualSubject = subjectThreadContext.getInvocationSubject();
            assertEquals("The retrieved subject must be equals to the invocation subject.", invocationSubject, actualSubject);
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
            SubjectThreadContext subjectThreadContext = new SubjectThreadContext();
            subjectThreadContext.setInvocationSubject(invocationSubject);
            subjectThreadContext.setCallerSubject(callerSubject);
            subjectThreadContext.clearSubjects();

            assertNull("The caller subject must be null.", subjectThreadContext.getCallerSubject());
            assertNull("The invocation subject must be null.", subjectThreadContext.getInvocationSubject());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
