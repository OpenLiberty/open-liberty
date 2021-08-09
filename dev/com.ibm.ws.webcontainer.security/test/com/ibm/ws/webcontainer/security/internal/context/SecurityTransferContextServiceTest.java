/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal.context;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.webcontainer.security.context.SecurityTransferContextService;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextService;

/**
 *
 */
public class SecurityTransferContextServiceTest {
    private final SubjectManager subjectManager = new SubjectManager();
    private final ITransferContextService context = new SecurityTransferContextService();

    @Before
    public void setUp() {
        subjectManager.clearSubjects();
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.context.SecurityTransferContextService#storeState(java.util.Map)}.
     */
    @Test
    public void storeState_nullSubjects() {
        Subject callerSubject = null;
        Subject invocationSubject = null;

        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);

        Map<String, Object> m = new HashMap<String, Object>();
        context.storeState(m);
        assertSame("Caller subject should be set in the map",
                   callerSubject, m.get(SecurityTransferContextService.CALLER_SUBJECT_KEY));
        assertSame("Invocation subject should be set in the map",
                   invocationSubject, m.get(SecurityTransferContextService.INVOCATION_SUBJECT_KEY));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.context.SecurityTransferContextService#storeState(java.util.Map)}.
     */
    @Test
    public void storeState_subjects() {
        Subject callerSubject = new Subject();
        Subject invocationSubject = new Subject();

        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);

        Map<String, Object> m = new HashMap<String, Object>();
        context.storeState(m);
        assertSame("Caller subject should be set in the map",
                   callerSubject, m.get(SecurityTransferContextService.CALLER_SUBJECT_KEY));
        assertSame("Invocation subject should be set in the map",
                   invocationSubject, m.get(SecurityTransferContextService.INVOCATION_SUBJECT_KEY));
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.context.SecurityTransferContextService#restoreState(java.util.Map)}.
     */
    @Test
    public void restoreState_nullSubjects() {
        Subject callerSubject = null;
        Subject invocationSubject = null;

        Map<String, Object> m = new HashMap<String, Object>();
        m.put(SecurityTransferContextService.CALLER_SUBJECT_KEY, callerSubject);
        m.put(SecurityTransferContextService.INVOCATION_SUBJECT_KEY, invocationSubject);

        context.restoreState(m);
        assertSame("Caller subject should be set in the map",
                   callerSubject, subjectManager.getCallerSubject());
        assertSame("Invocation subject should be set in the map",
                   invocationSubject, subjectManager.getInvocationSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.context.SecurityTransferContextService#restoreState(java.util.Map)}.
     */
    @Test
    public void restoreState_subjects() {
        Subject callerSubject = new Subject();
        Subject invocationSubject = new Subject();

        Map<String, Object> m = new HashMap<String, Object>();
        m.put(SecurityTransferContextService.CALLER_SUBJECT_KEY, callerSubject);
        m.put(SecurityTransferContextService.INVOCATION_SUBJECT_KEY, invocationSubject);

        context.restoreState(m);
        assertSame("Caller subject should be set in the map",
                   callerSubject, subjectManager.getCallerSubject());
        assertSame("Invocation subject should be set in the map",
                   invocationSubject, subjectManager.getInvocationSubject());
    }

    /**
     * Test method for {@link com.ibm.ws.webcontainer.security.context.SecurityTransferContextService#resetState()}.
     */
    @Test
    public void resetState() {
        Subject callerSubject = new Subject();
        Subject invocationSubject = new Subject();

        subjectManager.setCallerSubject(callerSubject);
        subjectManager.setInvocationSubject(invocationSubject);

        context.resetState();

        assertNull("Caller subject should be null",
                   subjectManager.getCallerSubject());
        assertNull("Invocation subject should be null",
                   subjectManager.getInvocationSubject());
    }

}
