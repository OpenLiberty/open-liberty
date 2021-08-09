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
package com.ibm.ws.security.context.internal;

import javax.security.auth.Subject;

/**
 * The thread context that holds the caller and invocation subjects.
 */
public class SubjectThreadContext {

    private Subject callerSubject;
    private Subject invocationSubject;

    /**
     * Sets the caller subject.
     */
    public void setCallerSubject(Subject callerSubject) {
        this.callerSubject = callerSubject;
    }

    /**
     * Gets the caller subject.
     */
    public Subject getCallerSubject() {
        return callerSubject;
    }

    /**
     * Sets the invocation subject.
     */
    public void setInvocationSubject(Subject invocationSubject) {
        this.invocationSubject = invocationSubject;
    }

    /**
     * Gets the invocation subject.
     */
    public Subject getInvocationSubject() {
        return invocationSubject;
    }

    /**
     * Clears the caller and invocation subjects by setting them to null.
     */
    public void clearSubjects() {
        callerSubject = null;
        invocationSubject = null;
    }

}
