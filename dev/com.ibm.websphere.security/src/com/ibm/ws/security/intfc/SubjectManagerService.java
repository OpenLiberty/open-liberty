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
package com.ibm.ws.security.intfc;

import javax.security.auth.Subject;

/**
 * Please do not use this interface. Use WSSubject instead for all your thread
 * security context needs.
 * 
 * This interface is intended for internal only use by the security component to
 * work around visibility and circular build path issues.
 */
public interface SubjectManagerService {

    public final String KEY_SUBJECT_MANAGER_SERVICE = "subjectManagerService";

    /**
     * Sets the caller subject on the thread.
     */
    public void setCallerSubject(Subject callerSubject);

    /**
     * Gets the caller subject from the thread.
     */
    public Subject getCallerSubject();

    /**
     * Sets the invocation subject on the thread.
     */
    public void setInvocationSubject(Subject invocationSubject);

    /**
     * Gets the invocation subject from the thread.
     */
    public Subject getInvocationSubject();
}
