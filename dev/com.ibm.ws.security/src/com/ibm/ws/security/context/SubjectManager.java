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
package com.ibm.ws.security.context;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.context.internal.SubjectThreadContext;

/**
 * The SubjectManager sets and gets caller/invocation subjects off the thread
 * and provides the ability to clear the subjects off the thread.
 */
public class SubjectManager {

    private static ThreadLocal<SubjectThreadContext> threadLocal = new SecurityThreadLocal();

    /**
     * Sets the caller subject on the thread.
     */
    public void setCallerSubject(Subject callerSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        subjectThreadContext.setCallerSubject(callerSubject);
    }

    /**
     * Gets the caller subject from the thread.
     */
    public Subject getCallerSubject() {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        return subjectThreadContext.getCallerSubject();
    }

    /**
     * Sets the invocation subject on the thread.
     */
    public void setInvocationSubject(Subject invocationSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        subjectThreadContext.setInvocationSubject(invocationSubject);
    }

    /**
     * Gets the invocation subject from the thread.
     */
    public Subject getInvocationSubject() {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        return subjectThreadContext.getInvocationSubject();
    }

    /**
     * Replaces the caller subject on the thread and returns the replaced subject.
     */
    public Subject replaceCallerSubject(Subject callerSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        Subject replacedCallerSubject = subjectThreadContext.getCallerSubject();
        subjectThreadContext.setCallerSubject(callerSubject);
        return replacedCallerSubject;
    }

    /**
     * Replaces the invocation subject on the thread and returns the replaced subject.
     */
    public Subject replaceInvocationSubject(Subject invocationSubject) {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        Subject replacedInvocationSubject = subjectThreadContext.getInvocationSubject();
        subjectThreadContext.setInvocationSubject(invocationSubject);
        return replacedInvocationSubject;
    }

    /**
     * Clears the caller and invocation subjects by setting them to null.
     */
    public void clearSubjects() {
        SubjectThreadContext subjectThreadContext = getSubjectThreadContext();
        subjectThreadContext.clearSubjects();
    }

    /**
     * Gets the subject thread context that is unique per thread.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     * 
     * @return the subject thread context.
     */
    @Trivial
    protected SubjectThreadContext getSubjectThreadContext() {
        ThreadLocal<SubjectThreadContext> currentThreadLocal = getThreadLocal();
        SubjectThreadContext subjectThreadContext = currentThreadLocal.get();
        if (subjectThreadContext == null) {
            subjectThreadContext = new SubjectThreadContext();
            currentThreadLocal.set(subjectThreadContext);
        }
        return subjectThreadContext;
    }

    /**
     * Gets the thread local object.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     * 
     * @return the thread local object.
     */
    @Trivial
    private ThreadLocal<SubjectThreadContext> getThreadLocal() {
        return threadLocal;
    }

    private static final class SecurityThreadLocal extends ThreadLocal<SubjectThreadContext> {
        @Override
        protected SubjectThreadContext initialValue() {
            return new SubjectThreadContext();
        }
    }

}
